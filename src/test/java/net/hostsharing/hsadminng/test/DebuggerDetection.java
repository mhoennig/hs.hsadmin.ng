package net.hostsharing.hsadminng.test;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;

@UtilityClass
public class DebuggerDetection {
    public static boolean isDebuggerAttached() {
        // check for typical debug arguments in the JVM input arguments
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(arg -> arg.contains("-agentlib:jdwp"));
    }
}
