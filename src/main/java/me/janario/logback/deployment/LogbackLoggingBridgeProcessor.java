package me.janario.logback.deployment;

import org.jboss.as.server.deployment.*;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * @author Janario Oliveira
 */
public class LogbackLoggingBridgeProcessor implements DeploymentUnitProcessor {

    private static final String[] LOG_DEPS = new String[]{
            "org.slf4j",
            "org.slf4j.impl",
            "org.jboss.logging.jul-to-slf4j-stub",
            "org.apache.commons.logging",
            "org.jboss.logging"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader bootModule = Module.getBootModuleLoader();
        for (String moduleId : LOG_DEPS) {
            try {
                bootModule.loadModule(moduleId);
                moduleSpecification.addSystemDependency(new ModuleDependency(bootModule, moduleId, false, false, false, false));
            } catch (ModuleLoadException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
    }
}
