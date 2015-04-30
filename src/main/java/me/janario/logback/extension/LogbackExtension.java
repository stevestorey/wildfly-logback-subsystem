package me.janario.logback.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.log.JDKModuleLogger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.spi.MDCAdapter;

import ch.qos.logback.classic.util.LogbackMDCAdapter;

/**
 * @author Janario Oliveira
 */
public class LogbackExtension implements Extension {

    public static final String NAMESPACE = "urn:me.janario.logback:1.0";
    public static final String SUBSYSTEM_NAME = "logback";

    private final SubsystemParser parser = new SubsystemParser();
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = LogbackExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, LogbackExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        fixMDCAdapter();
        installJul();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(LogbackDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(parser);
    }

    private void fixMDCAdapter() {
        // workaround to avoid NPE on EJB async calls
        // NPE in async(MDC map) org.jboss.as.ejb3.component.interceptors.LogDiagnosticContextRecoveryInterceptor.processInvocation{MDC.getMap() &gt; LogbackMDCAdapter.getCopyOfContextMap}
        final MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        try {
            final Field copyOnInheritThreadLocal = LogbackMDCAdapter.class.getDeclaredField("copyOnInheritThreadLocal");
            copyOnInheritThreadLocal.setAccessible(true);

            copyOnInheritThreadLocal.set(mdcAdapter, new InheritableThreadLocal<Map<String, String>>() {
                @Override
                protected Map<String, String> initialValue() {
                    return new ConcurrentHashMap<>();
                }
            });
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void installJul() {
        if (!SLF4JBridgeHandler.isInstalled()) {
            java.util.logging.Logger root = LogManager.getLogManager().getLogger("");
            for (java.util.logging.Handler h : root.getHandlers()) {
                root.removeHandler(h);
            }
            SLF4JBridgeHandler.install();
        }
        Module.setModuleLogger(new JDKModuleLogger());
    }

    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(LogbackExtension.NAMESPACE, false);
            writer.writeEndElement();
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoContent(reader);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            list.add(subsystem);
        }
    }
}
