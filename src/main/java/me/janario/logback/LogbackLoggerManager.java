package me.janario.logback;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.LogManager;

import me.janario.logback.deployment.LogbackContextSelector;

/**
 * Just to override org.jboss.logmanager.LogManager so LogManager won't be initialized
 * Once it is the first class loaded sets some properties
 * <p>
 * see /META-INF/services/java.util.logging.LogManager
 *
 * @author Janario Oliveira
 * @see org.jboss.as.server.Main
 */
public class LogbackLoggerManager extends LogManager {
    public LogbackLoggerManager() {
        configure();
    }

    private void configure() {
        String logging = System.getProperty("logging.configuration");
        try {
            final String path = new URI(logging).getPath();
            File logbackDefault = new File(new File(path).getParent(), "logback-default.xml");

            //force to avoid custom modules to change auto detection
            System.setProperty("org.jboss.logging.provider", "slf4j");
            System.setProperty("logback.ContextSelector", LogbackContextSelector.class.getName());
            System.setProperty("logback.configurationFile", logbackDefault.getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not find file " + logging);
        }
    }
}
