package com.ilimi.taxonomycontroller.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@RunWith(value=SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:servlet-context.xml" })
public class GetTaxonomyTest {
	@Autowired 
    private WebApplicationContext context;
    
    private MockMvc mockMvc;
    
    @Before
    public void setup() throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    
   @org.junit.Test
    public void getDefination() throws Exception {
        ResultActions actions = mockMvc.perform(get("/taxonomy/NUMERACY").param("subgraph", "true").param("cfields", "name").param("tfields", "name").header("Content-Type", "application/json").header("user-id", "jeetu"));
        actions.andDo(MockMvcResultHandlers.print());
        actions.andExpect(status().isOk());
   }
   
   @org.junit.Test
   public void falseNumeracy() throws Exception {
       ResultActions actions = mockMvc.perform(get("/taxonomy/NUMERACY").param("subgraph", "false").param("cfields", "name").param("tfields", "name").header("Content-Type", "application/json").header("user-id", "jeetu"));
       actions.andDo(MockMvcResultHandlers.print());
       actions.andExpect(status().isOk());
  }

}
