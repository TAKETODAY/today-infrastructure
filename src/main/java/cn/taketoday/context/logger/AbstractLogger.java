/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.logger;

/**
 * @author TODAY <br>
 *         2019-11-03 15:12
 */
public abstract class AbstractLogger implements Logger {

    protected static final String FQCN = AbstractLogger.class.getName();

    @Override
    public void trace(String msg) {
        logInternal(Level.TRACE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logInternal(Level.TRACE, format, new Object[] { arg });
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logInternal(Level.TRACE, format, new Object[] { arg1, arg2 });
    }

    @Override
    public void trace(String format, Object... arguments) {
        logInternal(Level.TRACE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logInternal(Level.TRACE, msg, t);
    }

    @Override
    public void debug(String msg) {
        logInternal(Level.DEBUG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logInternal(Level.DEBUG, format, new Object[] { arg });
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logInternal(Level.DEBUG, format, new Object[] { arg1, arg2 });
    }

    @Override
    public void debug(String format, Object... arguments) {
        logInternal(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logInternal(Level.DEBUG, msg);
    }

    @Override
    public void info(String msg) {
        logInternal(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        logInternal(Level.INFO, format, new Object[] { arg });
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logInternal(Level.INFO, format, new Object[] { arg1, arg2 });
    }

    @Override
    public void info(String format, Object... arguments) {
        logInternal(Level.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logInternal(Level.INFO, msg, t);
    }

    @Override
    public void warn(String msg) {
        logInternal(Level.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logInternal(Level.WARN, format, new Object[] { arg });
    }

    @Override
    public void warn(String format, Object... arguments) {
        logInternal(Level.WARN, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logInternal(Level.WARN, format, new Object[] { arg1, arg2 });
    }

    @Override
    public void warn(String msg, Throwable t) {
        logInternal(Level.WARN, msg, t);
    }

    @Override
    public void error(String msg) {
        logInternal(Level.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        logInternal(Level.ERROR, format, new Object[] { arg });
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logInternal(Level.ERROR, format, new Object[] { arg1, arg2 });
    }

    @Override
    public void error(String format, Object... arguments) {
        logInternal(Level.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logInternal(Level.ERROR, msg, t);
    }

    /**
     * Return if logging level is enabled.
     */
    public final boolean isLevelEnabled(Level level) {

        switch (level) { //@off
            case TRACE :    return isTraceEnabled();
            case DEBUG :    return isDebugEnabled();
            case INFO :     return isInfoEnabled();
            case WARN :     return isWarnEnabled();
            case ERROR :    return isErrorEnabled();
            default:        return isInfoEnabled();
        } //@on
    }

    protected void logInternal(Level level, String msg) {
        logInternal(level, msg, (Object[]) null);
    }

    protected void logInternal(Level level, String msg, Throwable t) {
        logInternal(level, msg, t, null);
    }

    protected void logInternal(Level level, String msg, Object[] args) {
        logInternal(level, msg, null, args);
    }

    protected abstract void logInternal(Level level, String msg, Throwable t, Object[] args);

}
