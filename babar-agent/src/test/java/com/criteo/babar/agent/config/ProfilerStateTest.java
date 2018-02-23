package com.criteo.babar.agent.config;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProfilerStateTest {

    @Test
    public void parseNameOnly() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, Map<String, String>> profilersConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerState(states, profilersConfig);
        states.push(state);
        String s = state.parse("myProfiler");
        assertEquals("", s);
        assertEquals(0, profilersConfig.get("myProfiler").size());
        assertEquals(0, states.size());
    }

    @Test
    public void parseNameOtherText() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, Map<String, String>> profilersConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerState(states, profilersConfig);
        states.push(state);
        String s = state.parse("myProfiler,something");
        assertEquals(",something", s);
        assertEquals(0, profilersConfig.get("myProfiler").size());
        assertEquals(0, states.size());
    }

    @Test
    public void parseNameEmptyParams() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, Map<String, String>> profilersConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerState(states, profilersConfig);
        states.push(state);
        String s = state.parse("myProfiler[],something");
        assertEquals(",something", s);
        assertNotNull(profilersConfig.get("myProfiler"));
        assertEquals(0, states.size());
    }

    @Test
    public void parseNameOneParams() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, Map<String, String>> profilersConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerState(states, profilersConfig);
        states.push(state);
        String s = state.parse("myProfiler[a=b],something");
        assertEquals("a=b", s);
        assertEquals(",something", ((AgentConfig.ConfigParser.ProfilerParamsState)states.peek()).nextString);
        assertNotNull(profilersConfig.get("myProfiler"));
        assertEquals(1, states.size());
    }

    @Test
    public void parseNameMultipleParams() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        Map<String, Map<String, String>> profilersConfig = new HashMap<>();
        AgentConfig.ConfigParser.State state = new AgentConfig.ConfigParser.ProfilerState(states, profilersConfig);
        states.push(state);
        String s = state.parse("myProfiler[a=b,c=d],something");
        assertEquals("a=b,c=d", s);
        assertEquals(",something", ((AgentConfig.ConfigParser.ProfilerParamsState)states.peek()).nextString);
        assertNotNull(profilersConfig.get("myProfiler"));
        assertEquals(1, states.size());
    }

}