package com.ilimi.taxonomy.conceptcontroller.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
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

import com.ilimi.graph.common.Response;

@WebAppConfiguration
@RunWith(value=SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:servlet-context.xml" })
public class GetConceptsTest {
	@Autowired 
    private WebApplicationContext context;
    
    private MockMvc mockMvc;
    
    @Before
    public void setup() throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    
    @org.junit.Test
    public void getConcepts() throws Exception {
        ResultActions actions = mockMvc.perform(get("/concept/Num:C1/isParentOf").param("taxonomyId", "NUMERACY").param("depth", "0").header("Content-Type", "application/json").header("user-id", "jeetu"));
        actions.andDo(MockMvcResultHandlers.print());
        actions.andExpect(status().isOk());
        
    }
    
    @org.junit.Test
    public void withoutTaxonomyId() throws Exception {
        ResultActions actions = mockMvc.perform(get("/concept/Num:C1/isParentOf").param("depth", "0").header("Content-Type", "application/json").header("user-id", "jeetu"));
        actions.andDo(MockMvcResultHandlers.print());
        actions.andExpect(status().is(400));
        Assert.assertEquals("Required String parameter 'taxonomyId' is not present", actions.andReturn().getResponse().getErrorMessage());
        
    }
    
    @org.junit.Test
    public void emptyTaxonomyId() throws Exception {
        ResultActions actions = mockMvc.perform(get("/concept/Num:C1/isParentOf").param("taxonomyId", "").param("depth", "0").header("Content-Type", "application/json").header("user-id", "jeetu"));
        actions.andDo(MockMvcResultHandlers.print());
        actions.andExpect(status().is(400));        
        String content = (String) actions.andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Response resp = objectMapper.readValue(content, Response.class);
        Assert.assertEquals("Taxonomy Id is blank", resp.getParams().get("errmsg"));
        
    }
    
    
}
