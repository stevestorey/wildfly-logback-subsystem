package me.janario.logback;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

import me.janario.logback.deployment.LogbackContextSelector;

/**
 * Just to override org.jboss.logmanager.LogManager so LogManager won't be initialized
 * Once it is the first class loaded sets some properties
 *
 * @see /META-INF/services/java.util.logging.LogManager
 * @see org.jboss.as.server.Main
 *
 *
 * @author Janario Oliveira
 */
public class LogbackLoggerManager extends LogManager {
    public LogbackLoggerManager() {
        configure();
    }

    private void configure() {
        String logging = System.getProperty("logging.configuration");
        try {
            Path loggingProperties = Paths.get(new URI(logging));
            Path logbackDefault = loggingProperties.getParent().resolve("logback-default.xml");

            //force to avoid custom modules to change auto detection
            System.setProperty("org.jboss.logging.provider", "slf4j");
            System.setProperty("logback.ContextSelector", LogbackContextSelector.class.getName());
            System.setProperty("logback.configurationFile", logbackDefault.toFile().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not find file " + logging);
        }

    }
}
