package me.janario.logback.deployment.impl;

import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import me.janario.logback.deployment.LogbackContextSelector;

/**
 * @author Janario Oliveira
 */
public final class ContextualLevelDecisionTurboFilter extends TurboFilter {

    private final LogbackContextSelector contextSelector;
    private final CopyOnWriteArrayList<TurboFilter> others;

    public ContextualLevelDecisionTurboFilter(LogbackContextSelector contextSelector,
                                              CopyOnWriteArrayList<TurboFilter> others) {
        this.contextSelector = contextSelector;
        this.others = new CopyOnWriteArrayList<>(others);
    }

    public CopyOnWriteArrayList<TurboFilter> getOthers() {
        return others;
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        //will get from cache
        final Logger contextualLogger = contextSelector.getLoggerContext().getLogger(logger.getName());
        FilterReply levelDecision = level.isGreaterOrEqual(contextualLogger.getEffectiveLevel())
                ? FilterReply.ACCEPT : FilterReply.DENY;

        if (levelDecision == FilterReply.DENY) {
            return FilterReply.DENY;
        }

        final FilterReply othersDecision = getOthersDecision(marker, logger, level, format, params, t);
        if (othersDecision == FilterReply.NEUTRAL) {
            //levelDecision has accepted
            return FilterReply.ACCEPT;
        }
        return othersDecision;
    }

    public FilterReply getOthersDecision(
            Marker marker, Logger logger, Level level, String format,
            Object[] params, Throwable t) {
        FilterReply lastDecision = FilterReply.NEUTRAL;
        for (TurboFilter other : others) {
            lastDecision = other.decide(marker, logger, level, format, params, t);
            if (lastDecision == FilterReply.DENY || lastDecision == FilterReply.ACCEPT) {
                return lastDecision;
            }
        }
        return lastDecision;
    }
}
