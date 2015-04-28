package me.janario.logback.deployment;

import org.jboss.as.server.deployment.*;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * @author Janario Oliveira
 */
public class LogbackLoggingBridgeProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier[] LOG_DEPS = new ModuleIdentifier[]{
            ModuleIdentifier.create("org.slf4j"),
            ModuleIdentifier.create("org.slf4j.impl"),
            ModuleIdentifier.create("org.jboss.logging.jul-to-slf4j-stub"),
            ModuleIdentifier.create("org.apache.log4j"),
            ModuleIdentifier.create("org.apache.commons.logging"),
            ModuleIdentifier.create("org.jboss.logging")
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader bootModule = Module.getBootModuleLoader();
        for (ModuleIdentifier moduleId : LOG_DEPS) {
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
