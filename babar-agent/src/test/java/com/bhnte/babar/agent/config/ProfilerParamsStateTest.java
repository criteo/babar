package com.bhnte.babar.agent.config;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProfilerParamsStateTest {

    @Test
    public void parseReturnOriginalStringAfterKVParsed() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, String> mainConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerParamsState(states, mainConfig, "orig");
        states.push(state);
        String s = state.parse(null);
        assertEquals("orig", s);
        assertEquals(0, states.size());
    }

    @Test
    public void parseCommaText() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, String> mainConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerParamsState(states, mainConfig, "orig");
        states.push(state);
        String s = state.parse(",a=b");
        assertEquals("a=b", s);
        assertEquals(2, states.size());
    }
}