package me.janario.logback.extension;

import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author Janario Oliveira
 */
public class LogbackDefinition extends SimpleResourceDefinition {
    public static final LogbackDefinition INSTANCE = new LogbackDefinition();

    private LogbackDefinition() {
        super(LogbackExtension.SUBSYSTEM_PATH,
                LogbackExtension.getResourceDescriptionResolver(null),
                LogbackSubsystemAdd.INSTANCE, LogbackSubsystemRemove.INSTANCE);
    }
}
