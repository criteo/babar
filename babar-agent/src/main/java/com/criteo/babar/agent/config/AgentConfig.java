package com.criteo.babar.agent.config;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentConfig extends Config {

    final Map<String, Config> profilersConfig;

    private AgentConfig(Map<String, String> mainConfig, Map<String, Map<String, String>> profilersConfig) {
        super(mainConfig);
        this.profilersConfig = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> c: profilersConfig.entrySet()) {
            this.profilersConfig.put(c.getKey(), new Config(c.getValue()));
        }
    }

    public static AgentConfig parse(String args) {
        ConfigParser parser = new ConfigParser(args);
        return new AgentConfig(parser.mainConfig, parser.profilersConfig);
    }

    public Boolean isProfilerEnabled(String profilerName) {
        return profilersConfig.containsKey(profilerName);
    }

    public Config getProfilerConfig(String profilerName) {
        return profilersConfig.get(profilerName);
    }

    static class ConfigParser {

        private final Deque<State> states = new ArrayDeque<State>();
        final Map<String, String> mainConfig = new HashMap<>();
        final Map<String, Map<String, String>> profilersConfig = new HashMap<>();

        ConfigParser(String s) {
            states.push(new TransitionState(states, mainConfig, profilersConfig));
            while(states.size() > 1 || (s != null && !s.isEmpty())) {
                s = states.peek().parse(s);
            }
        }

        static String kv = "([a-zA-Z0-9_\\-\\.]+)=([a-zA-Z0-9:_\\-\\./]+)";
        static Pattern kvPattern = Pattern.compile("^("+kv+")(.*)$");
        static Pattern profilerPattern = Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)(\\[(((" + kv + ")(,)?)*)\\])?(.*)$");

        static class TransitionState implements State {

            private final Deque<State> states;
            private final Map<String, String> mainConfig;
            private final Map<String, Map<String, String>> profilersConfig;

            TransitionState(Deque<State> states, Map<String, String> mainConfig, Map<String, Map<String, String>> profilersConfig) {
                this.states = states;
                this.mainConfig = mainConfig;
                this.profilersConfig = profilersConfig;
            }

            @Override
            public String parse(String s) {
                if (kvPattern.matcher(s).find()) {
                    states.push(new KVState(states, mainConfig));
                    return s;
                }
                if (profilerPattern.matcher(s).find()) {
                    states.push(new ProfilerState(states, profilersConfig));
                    return s;
                }
                if (s.startsWith(",")) {
                    return s.substring(1);
                }
                throw new IllegalStateException("Could not parse rest of the arguments: " + s);
            }
        }

        static class KVState implements State {

            private final Deque<State> states;
            private final Map<String, String> config;

            KVState(Deque<State> states, Map<String, String> config) {
                this.states = states;
                this.config = config;
            }

            @Override
            public String parse(String s) {
                Matcher kvMatcher = kvPattern.matcher(s);
                if (kvMatcher.find()) {
                    config.put(kvMatcher.group(2), kvMatcher.group(3));
                    states.pop();
                    return kvMatcher.group(4);
                }
                throw new IllegalStateException("Could not parse key-value in arguments: " + s);
            }
        }

        static class ProfilerState implements State {

            final Deque<State> states;
            final Map<String, Map<String, String>> profilersConfig;

            ProfilerState(Deque<State> states, Map<String, Map<String, String>> profilersConfig) {
                this.states = states;
                this.profilersConfig = profilersConfig;
            }

            @Override
            public String parse(String s) {
                Matcher profilerMatcher = profilerPattern.matcher(s);
                if (profilerMatcher.find()) {
                    Map<String, String> profilerConfig = new HashMap<>();
                    profilersConfig.put(profilerMatcher.group(1), profilerConfig);
                    states.pop();
                    if (profilerMatcher.group(3) != null && !profilerMatcher.group(3).isEmpty()) {
                        states.push(new ProfilerParamsState(states, profilerConfig, profilerMatcher.group(9)));
                        return profilerMatcher.group(3);
                    }
                    return profilerMatcher.group(9);
                }
                throw new IllegalStateException("Could not parse profiler in arguments: " + s);
            }
        }

        static class ProfilerParamsState implements State {

            final Deque<State> states;
            final Map<String, String> profilerConfig;
            final String nextString;

            ProfilerParamsState(Deque<State> states, Map<String, String> profilerConfig, String nextString) {
                this.states = states;
                this.profilerConfig = profilerConfig;
                this.nextString = nextString;
            }

            @Override
            public String parse(String s) {
                // CAUTION: s is here a substring of originalString
                if (s== null || s.isEmpty()) {
                    states.pop();
                    return nextString;
                }
                if (s.startsWith(",")) {
                    states.push(new KVState(states, profilerConfig));
                    return s.substring(1);
                }
                if (kvPattern.matcher(s).find()) {
                    states.push(new KVState(states, profilerConfig));
                    return s;
                }
                throw new IllegalStateException("Could not parse profiler params in: " + s);
            }
        }

        interface State {
            String parse(String s);
        }
    }
}
