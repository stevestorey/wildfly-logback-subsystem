package me.janario.logback.deployment.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import me.janario.logback.deployment.LogbackContextSelector;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Janario Oliveira
 */
public final class ContextualAppenderAttachable<E extends ILoggingEvent>
        extends AppenderAttachableImpl<E> {
    private final LogbackContextSelector contextSelector;

    private final Map<LoggerContext, AppenderAttachableImpl<E>> appendersByContext = new ConcurrentHashMap<>();

    public ContextualAppenderAttachable(LogbackContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    private AppenderAttachableImpl<E> getContextual() {
        AppenderAttachableImpl<E> a = appendersByContext.get(contextSelector.getLoggerContext());
        if (a != null) {
            return a;
        }
        return appendersByContext.computeIfAbsent(contextSelector.getLoggerContext(), c -> new AppenderAttachableImpl<E>());
    }

    /*
    appenders from <loggers> won't work. e.g.

    app.war
    <logger name="org.hibernate.SQL" level="DEBUG" additivity="false">
       <appender-ref ref="OTHER_APPENDER"/>
    </logger>

    Should do something like:

    my contexts {default, app.war}

    class ContextualAppender<E> extends AppenderAttachableImpl<E> {
        String loggerName;
        Map<LoggerContext, AppenderAttachableImpl<E>> realAppenders;
        //delegate by context selector
    }
    Logger[org.hibernate.SQL].aai replaced to ContextualAppender and realAppenders:
    - in default context to rootAppender
    - in app.war context to declared appender-ref

    This should be done to:
    - app.war context Loggers with appenders > add in other contexts Loggers
    - other contexts(default) Loggers with appenders > add in app.war if not already replaced

    But after all that additivity=false will be broken(even worst super level will log in root).
    TODO think more contextual in Logback since creation of Logger(aai)
    */
    public void registerContextual(LoggerContext context) {
        //replace root appender to a one that select based on context
        try {
            final Field aai = getAaiField();

            final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            @SuppressWarnings("unchecked")
            AppenderAttachableImpl<E> appenders = (AppenderAttachableImpl<E>) aai.get(root);
            if (appenders == null) {
                appenders = new AppenderAttachableImpl<>();
            }

            appendersByContext.put(context, appenders);
            aai.set(root, this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void unregisterContextual(LoggerContext context) {
        try {
            AppenderAttachableImpl<E> appenders = appendersByContext.remove(context);
            final Field aai = getAaiField();

            final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            aai.set(root, appenders);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Field getAaiField() throws NoSuchFieldException {
        final Field aai = Logger.class.getDeclaredField("aai");
        aai.setAccessible(true);
        return aai;
    }

    @Override
    public int appendLoopOnAppenders(E e) {
        return getContextual().appendLoopOnAppenders(e);
    }

    @Override
    public void addAppender(Appender<E> newAppender) {
        getContextual().addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return getContextual().iteratorForAppenders();
    }

    @Override
    public Appender<E> getAppender(String name) {
        return getContextual().getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<E> appender) {
        return getContextual().isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        final AppenderAttachable<E> remove = appendersByContext.remove(contextSelector.getLoggerContext());
        if (remove != null) {
            remove.detachAndStopAllAppenders();
        }
    }

    @Override
    public boolean detachAppender(Appender<E> appender) {
        return getContextual().detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return getContextual().detachAppender(name);
    }
}
