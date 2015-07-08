package org.ekstep.ilimi.analytics.streaming

import scala.collection.mutable.Buffer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.spark.broadcast.Broadcast
import org.ekstep.ilimi.analytics.model.Event
import org.ekstep.ilimi.analytics.util.CommonUtil
import java.io.File

case class Pdata(id: String, mod: String, ver: String)
case class Gdata(id: String, ver: String)
case class EventOutput(eid: String, ts: Long, ver: String, uid: Option[String], cid: Option[String], gdata: Option[Gdata], pdata: Option[Pdata], edata: Map[String, AnyRef])
case class LevelAgg(code: String, min: Int, max: Int, level: String);

object LitScreenerLevelComputation extends Serializable {

    def main(cmdline: Array[String]) {
        val qid = "EK.L.KAN.LT1.Q1";
        val qids = qid.split('.');
        Console.println("qids.length - " + qids.length + " | qids - " + qids.mkString("|"));
    }

    private val litScreenerId = "org.ekstep.lit.scrnr.kan.basic";
    private val screenerVersion = "1.30";

    def compute(events: Buffer[Event], loltMapping: Broadcast[Map[String, Array[(String, String)]]], ldloMapping: Broadcast[Map[String, Array[(String, String)]]], compldMapping: Broadcast[Map[String, Array[(String, String)]]], litLevelsMap: Broadcast[Map[String, Array[LevelAgg]]], output: String, outputDir: Option[String], brokerList: String) {

        val loltMap = loltMapping.value;
        val ldloMap = ldloMapping.value;
        val compldMap = compldMapping.value;
        val levelMap = litLevelsMap.value;
        val oeAssesEvents = events.filter { x => (x.eid.equals("OE_ASSESS") && x.gdata.id.equals(litScreenerId)) }.map { x =>
            {
                val qids = x.edata.eks.qid.get.split('.');
                (x.edata.eks.qid.get, x.uid.get, if ("Yes".equalsIgnoreCase(x.edata.eks.pass.get)) 1 else 0, qids(3));
            }
        };
        val uid = oeAssesEvents.last._2;
        val ltScores = oeAssesEvents.groupBy(f => f._4).mapValues(f => f.map(f => f._3)).mapValues { x => x.reduce(_ + _) }.toMap;
        val loScores = getCodeMap(loltMap, ltScores);
        val ldScores = getCodeMap(ldloMap, loScores);
        val compositeScores = getCodeMap(compldMap, ldScores);
        val ltLevels = ltScores.map(f => { (uid, f._1, f._2, getLevel(f._1, f._2, levelMap)) });
        val loLevels = loScores.map(f => { (uid, f._1, f._2, getLevel(f._1, f._2, levelMap)) });
        val ldLevels = ldScores.map(f => { (uid, f._1, f._2, getLevel(f._1, f._2, levelMap)) });
        val compositeLevels = compositeScores.map(f => { (uid, f._1, f._2, getLevel(f._1, f._2, levelMap)) });
        
        var result = Buffer[(String, String, Int, String)]();
        ltLevels.foreach(f => result += f);
        loLevels.foreach(f => result += f);
        ldLevels.foreach(f => result += f);
        compositeLevels.foreach(f => result += f);

        output match {
            case "console" =>
                Console.println("## Printing output to console ##");
                var resultEvents = Buffer[EventOutput]();
                result.foreach(f => resultEvents += getEventOutput(f));
                //resultEvents.foreach { x => Console.println(CommonUtil.jsonToString(x)) };
                result.foreach { x => Console.println(x) };
            case "csv" =>
                Console.println("## Printing output to csv ##");
                CommonUtil.printToFile(new File(outputDir.getOrElse("user-aggregates") + "/lit_scr_levels_" + uid + ".csv")) { p =>
                    result.foreach(f => {
                        p.println(f._1 + "," + f._2 + "," + f._3 + "," + f._4);
                    })
                }
            case "kafka" =>
                Console.println("## Printing output to kafka topic ##");
                var resultEvents = Buffer[EventOutput]();
                result.foreach(f => resultEvents += getEventOutput(f));
                KafkaEventProducer.sendEvents(resultEvents, outputDir.getOrElse("user_aggregates"), brokerList);
        }
    }

    def getEventOutput(event: (String, String, Int, String)): EventOutput = {
        val edata = Map[String, AnyRef]("score" -> event._3.asInstanceOf[AnyRef], "current_level" -> event._4);
        EventOutput("ME_USER_GAME_LEVEL", System.currentTimeMillis(), "1.0", Some(event._1), Some(event._2), Some(Gdata(litScreenerId, screenerVersion)),
            Some(Pdata("AssessmentPipeline", "LitScreenerLevelComputation", "1.0")), edata);
    }
    
    def getCodeMap(codeMap: Map[String, Array[(String, String)]], valueMap: Map[String, Int]) : Map[String, Int] = {
        codeMap.mapValues(f => {f.map(f => valueMap.getOrElse(f._2, 0))}).mapValues { x => x.reduce(_ + _) }.toMap;
    }

    def getLevel(code: String, score: Int, levelMap: Map[String, Array[LevelAgg]]): String = {

        var level = "NL";
        val arr = levelMap.get(code).getOrElse(null);
        if (arr != null) {
            arr.foreach { levelAgg =>
                {
                    if (score >= levelAgg.min && score <= levelAgg.max) {
                        level = levelAgg.level;
                    }
                }
            }
        }
        level;
    }

}