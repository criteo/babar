package com.criteo.babar.agent.config;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class KVStateTest {

    @Test
    public void parseKeyValue() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, String> mainConfig = new HashMap<>();
        AgentConfig.ConfigParser.KVState state = new AgentConfig.ConfigParser.KVState(states, mainConfig);
        states.push(state);
        String s = state.parse("a=b");
        assertEquals("", s);
        assertEquals("b", mainConfig.get("a"));
        assertEquals(0, states.size());
    }

    @Test
    public void parseKeyValueOtherText() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, String> mainConfig = new HashMap<>();
        AgentConfig.ConfigParser.KVState state = new AgentConfig.ConfigParser.KVState(states, mainConfig);
        states.push(state);
        String s = state.parse("abc=b123,c=d");
        assertEquals(",c=d", s);
        assertEquals("b123", mainConfig.get("abc"));
        assertEquals(0, states.size());
    }
}