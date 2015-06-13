package me.janario.logback.deployment;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.helpers.Util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.StatusPrinter;
import me.janario.logback.deployment.impl.ContextualAppenderAttachable;
import me.janario.logback.deployment.impl.ContextualLevelDecisionTurboFilter;

/**
 * @author Janario Oliveira
 */
public class LogbackContextSelector implements ContextSelector {

    private final LoggerContext defaultContext;
    private final ConcurrentMap<String, LoggerContext> contextByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassLoader, LoggerContext> contextByClassLoader = new ConcurrentHashMap<>();
    private final ConcurrentMap<LoggerContext, ClassLoader> classLoaderByContext = new ConcurrentHashMap<>();

    private final ThreadLocal<LoggerContext> threadContext = new ThreadLocal<>();
    private final ContextualAppenderAttachable<ILoggingEvent> contextualAppenders;

    public LogbackContextSelector(LoggerContext defaultContext) {
        this.defaultContext = defaultContext;
        contextualAppenders = new ContextualAppenderAttachable<>(this);
        wrapFilters(defaultContext);
    }

    private void wrapFilters(LoggerContext context) {
        final ContextualLevelDecisionTurboFilter
                levelFilter = new ContextualLevelDecisionTurboFilter(this, context.getTurboFilterList());
        context.getTurboFilterList().clear();
        context.getTurboFilterList().add(levelFilter);

        contextualAppenders.registerContextual(context);
    }

    private void unwrapFilters(LoggerContext context) {
        TurboFilter turboFilter = context.getTurboFilterList().stream().findFirst().orElse(null);
        if (turboFilter instanceof ContextualLevelDecisionTurboFilter) {
            ContextualLevelDecisionTurboFilter levelFilter = (ContextualLevelDecisionTurboFilter) turboFilter;
            context.getTurboFilterList().clear();
            context.getTurboFilterList().addAll(levelFilter.getOthers());
        }
        contextualAppenders.unregisterContextual(context);
    }

    @Override
    public LoggerContext getDefaultLoggerContext() {
        return defaultContext;
    }

    @Override
    public LoggerContext getLoggerContext() {
        final LoggerContext context = threadContext.get();
        if (context != null) {
            return context;
        }

        final ClassLoader tcl = Loader.getTCL();
        if (tcl == null) {
            return defaultContext;
        }
        return contextByClassLoader.computeIfAbsent(tcl, cl -> {
            final URL logbackXml = cl.getResource("/logback.xml");
            if (logbackXml == null) {
                return defaultContext;
            }
            String contextName = LogbackDeploymentRegisterProcessor.INSTANCE.getDeploymentName(cl);
            if (contextName == null) {
                return defaultContext;
            }

            LoggerContext loggerContext = new LoggerContext();
            loggerContext.setName(contextName);
            loggerContext.reset();
            //configureByResource calls recursive getLoggerContext
            threadContext.set(loggerContext);
            try {
                new ContextInitializer(loggerContext).configureByResource(logbackXml);
            } catch (JoranException je) {
                Util.report("Failed to auto configure default logger context", je);
            } finally {
                threadContext.remove();
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);

            if (contextByName.containsKey(contextName)) {
                throw new IllegalStateException("ContextName already registred " + contextName);
            }
            wrapFilters(loggerContext);
            contextByName.put(contextName, loggerContext);
            classLoaderByContext.put(loggerContext, tcl);
            return loggerContext;
        });
    }

    @Override
    public LoggerContext detachLoggerContext(String loggerContextName) {
        final LoggerContext loggerContext = contextByName.remove(loggerContextName);
        if (loggerContext != null && loggerContext != defaultContext) {
            loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
                    .debug("Stopping logger context {}", loggerContextName);
            unwrapFilters(loggerContext);
            loggerContext.stop();
            ClassLoader cl = classLoaderByContext.remove(loggerContext);
            if (cl != null) {
                contextByClassLoader.remove(cl);
            }
        }

        return loggerContext;
    }

    @Override
    public List<String> getContextNames() {
        return new ArrayList<>(contextByName.keySet());
    }

    @Override
    public LoggerContext getLoggerContext(String name) {
        return contextByName.get(name);
    }
}
