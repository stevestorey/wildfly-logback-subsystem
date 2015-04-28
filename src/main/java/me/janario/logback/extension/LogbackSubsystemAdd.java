package me.janario.logback.extension;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import me.janario.logback.deployment.LogbackDeploymentRegisterProcessor;
import me.janario.logback.deployment.LogbackLoggingBridgeProcessor;

/**
 * @author Janario Oliveira
 */
class LogbackSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final LogbackSubsystemAdd INSTANCE = new LogbackSubsystemAdd();

    private LogbackSubsystemAdd() {
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(LogbackExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_LOGGING, new LogbackLoggingBridgeProcessor());
                processorTarget.addDeploymentProcessor(LogbackExtension.SUBSYSTEM_NAME, Phase.FIRST_MODULE_USE,
                        Phase.FIRST_MODULE_USE_INTERCEPTORS, LogbackDeploymentRegisterProcessor.INSTANCE);
            }
        }, Stage.RUNTIME);
    }
}
