package me.janario.logback.deployment;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.ModuleClassLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ch.qos.logback.classic.util.ContextSelectorStaticBinder;

/**
 * @author Janario Oliveira
 */
public final class LogbackDeploymentRegisterProcessor implements DeploymentUnitProcessor {
    public static final LogbackDeploymentRegisterProcessor INSTANCE = new LogbackDeploymentRegisterProcessor();

    private final ConcurrentMap<ClassLoader, String> contextNameByClassLoader = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClassLoader> classLoaderByContextName = new ConcurrentHashMap<>();

    public static final AttachmentKey<String> LOGBACK_CONTEXT_NAME_KEY = AttachmentKey.create(String.class);

    private LogbackDeploymentRegisterProcessor() {
    }

    final String getDeploymentName(ClassLoader classLoader) {
        return contextNameByClassLoader.get(classLoader);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (phaseContext.hasAttachment(LOGBACK_CONTEXT_NAME_KEY)) {
            return;
        }

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleClassLoader classLoader = deploymentUnit.getAttachment(Attachments.MODULE).getClassLoader();

        final URL logbackXml = classLoader.getResource("/logback.xml");
        //no logback.xml will use default context
        if (logbackXml == null) {
            return;
        }

        String contextName = readContextNameFromXml(logbackXml);
        if (contextName == null) {
            contextName = deploymentUnit.getName();
        }
        if (classLoaderByContextName.containsKey(contextName)) {
            throw new IllegalStateException("Context " + contextName + " already registred");
        }

        contextNameByClassLoader.put(classLoader, contextName);
        classLoaderByContextName.put(contextName, classLoader);
        deploymentUnit.putAttachment(LOGBACK_CONTEXT_NAME_KEY, contextName);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        if (!context.hasAttachment(LOGBACK_CONTEXT_NAME_KEY)) {
            return;
        }

        final String contextName = context.getAttachment(LOGBACK_CONTEXT_NAME_KEY);
        ContextSelectorStaticBinder.getSingleton().getContextSelector().detachLoggerContext(contextName);

        final ClassLoader remove = classLoaderByContextName.remove(contextName);
        if (remove != null) {
            contextNameByClassLoader.remove(remove);
        }
    }

    private String readContextNameFromXml(URL logback) {
        try (BufferedInputStream is = new BufferedInputStream(logback.openStream())) {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            final NodeList configuration = doc.getFirstChild().getChildNodes();
            for (int i = 0; i < configuration.getLength(); i++) {
                final Node contextName = configuration.item(i);
                if (contextName.getNodeName().equals("contextName")) {
                    return contextName.getChildNodes().item(0).getNodeValue();
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException("Error reading contextName from " + logback, e);
        }
        return null;
    }
}
