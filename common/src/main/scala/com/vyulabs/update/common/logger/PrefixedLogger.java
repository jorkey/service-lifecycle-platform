package com.vyulabs.update.common.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class PrefixedLogger implements Logger {
    private String prefix;
    private Logger log;

    public PrefixedLogger(String profiledServiceName, Logger log) {
        this.prefix = profiledServiceName;
        this.log = log;
    }

    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        log.trace(fmt(msg));
    }

    @Override
    public void trace(String format, Object arg) {
        log.trace(fmt(format), arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log.trace(fmt(format), arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log.trace(fmt(format), arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log.trace(fmt(msg), t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        log.trace(marker, fmt(msg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log.trace(marker, fmt(format), arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log.trace(marker, fmt(format), arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        log.trace(marker, fmt(format), argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log.trace(marker, fmt(msg), t);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {
        log.debug(fmt(msg));
    }

    @Override
    public void debug(String format, Object arg) {
        log.debug(fmt(format), arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log.debug(fmt(format), arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log.debug(fmt(format), arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log.debug(fmt(msg), t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return log.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        log.debug(marker, fmt(msg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log.debug(marker, fmt(format), arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log.debug(marker, fmt(format), arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        log.debug(marker, fmt(format), arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log.debug(marker, fmt(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        log.info(fmt(msg));
    }

    @Override
    public void info(String format, Object arg) {
        log.info(fmt(format), arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log.info(fmt(format), arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log.info(fmt(format), arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log.info(fmt(msg), t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        log.info(marker, fmt(msg));
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log.info(marker, fmt(format), arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log.info(marker, fmt(format), arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        log.info(marker, fmt(format), arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log.info(marker, fmt(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        log.warn(fmt(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        log.warn(fmt(format), arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log.warn(fmt(format), arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log.warn(fmt(format), arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log.warn(fmt(msg), t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return log.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log.warn(marker, fmt(msg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log.warn(marker, fmt(format), arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log.warn(marker, fmt(format), arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        log.warn(marker, fmt(format), arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log.warn(marker, fmt(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        log.error(fmt(msg));
    }

    @Override
    public void error(String format, Object arg) {
        log.error(fmt(format), arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log.error(fmt(format), arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log.error(fmt(format), arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log.error(fmt(msg), t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return log.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        log.error(marker, fmt(msg));
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log.error(marker, fmt(format), arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log.error(marker, fmt(format), arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        log.error(marker, fmt(format), arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log.error(marker, fmt(msg), t);
    }

    private String fmt(String msg) {
        return prefix + msg;
    }
}
