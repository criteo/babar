package com.bhnte.babar.agent.config;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.Deque;

public class TransitionStateTest {

    @Test
    public void parseComma() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        String s = new AgentConfig.ConfigParser.TransitionState(states, null, null).parse(",");
        assertEquals("", s);
        assertEquals(0, states.size());
    }

    @Test
    public void parseCommaText() throws Exception {
        Deque<AgentConfig.ConfigParser.State> states = new ArrayDeque<>();
        String s = new AgentConfig.ConfigParser.TransitionState(states, null, null).parse(",something");
        assertEquals("something", s);
        assertEquals(0, states.size());
    }

}