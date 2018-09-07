package com.criteo.babar.agent.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigParserTest {

    @Test
    public void parseEmpty() throws Exception {
        AgentConfig config = AgentConfig.parse("");
        assertEquals(0, config.mainConfig.size());
        assertEquals(0, config.profilersConfig.size());
    }

    @Test
    public void parseKeyValues() throws Exception {
        AgentConfig config = AgentConfig.parse("a=b,c=d/e");
        assertEquals(2, config.mainConfig.size());
        assertEquals("b", config.mainConfig.get("a"));
        assertEquals("d/e", config.mainConfig.get("c"));
        assertEquals(0, config.profilersConfig.size());
    }

    @Test
    public void parseKeyValuesAndProfiler() throws Exception {
        AgentConfig config = AgentConfig.parse("a=b,myProfiler[a=b],c=d");
        assertEquals(2, config.mainConfig.size());
        assertEquals("b", config.mainConfig.get("a"));
        assertEquals("d", config.mainConfig.get("c"));
        assertEquals(1, config.profilersConfig.size());
        assertEquals("b", config.profilersConfig.get("myProfiler").get("a"));
    }

    @Test
    public void parseKeyValuesAndProfilers() throws Exception {
        AgentConfig config = AgentConfig.parse("a=b,myProfiler[a=b],c=d,otherProfiler[a=b]");
        assertEquals(2, config.mainConfig.size());
        assertEquals("b", config.mainConfig.get("a"));
        assertEquals("d", config.mainConfig.get("c"));
        assertEquals(2, config.profilersConfig.size());
        assertEquals("b", config.profilersConfig.get("myProfiler").get("a"));
        assertEquals("b", config.profilersConfig.get("otherProfiler").get("a"));
    }

}