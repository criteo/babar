package com.criteo.babar.agent.profiler;

import com.google.common.base.Joiner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class StackTraceFormatter {
    private StackTraceFormatter() { }

    /**
     * Formats a StackTraceElement as a String, excluding the line number
     *
     * @param element The StackTraceElement to format
     * @return A String representing the given StackTraceElement
     */
    public static String formatStackTraceElement(StackTraceElement element) {
        return String.format("%s.%s:%d", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }

    /**
     * Formats an entire stack trace as a String
     *
     * @param stack The stack trace to format
     * @return A String representing the given stack trace
     */
    public static String formatStackTrace(String threadName, StackTraceElement[] stack) {
        Deque<String> lines = new ArrayDeque<>(stack.length);
        for (StackTraceElement element : stack) {
            lines.addFirst(formatStackTraceElement(element));
        }
        lines.addFirst(threadName);

        return Joiner.on("|").join(lines);
    }
}
