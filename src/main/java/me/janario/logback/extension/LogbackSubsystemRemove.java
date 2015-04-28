package me.janario.logback.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;

/**
 * @author Janario Oliveira
 */
class LogbackSubsystemRemove extends AbstractRemoveStepHandler {

    static final LogbackSubsystemRemove INSTANCE = new LogbackSubsystemRemove();

    private LogbackSubsystemRemove() {
    }
}
