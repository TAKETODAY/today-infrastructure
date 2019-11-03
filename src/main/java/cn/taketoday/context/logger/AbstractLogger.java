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

import java.lang.reflect.Array;

/**
 * @author TODAY <br>
 *         2019-11-03 15:12
 */
public abstract class AbstractLogger implements Logger {

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
        logInternal(level, msg, null, null);
    }

    protected abstract void logInternal(Level level, String msg, Throwable t, Object[] args);

    // 
    // -----------------------------------

    private final static String ARG_PLACE_HOLDER = "{}";
    private final static int DEFAULT_MESSAGE_LENGTH = 128;

    /**
     * Build a message like a slf4j
     * 
     * @param format
     *            Input message format
     * @param arguments
     *            Message arguments
     * @return a formatted message
     */
    public static String buildMessage(String format, Object[] arguments) {
        StringBuilder sb = null;
        int lastIndex = 0;
        int argCount = 0;
        while (true) {
            int argIndex = format.indexOf(ARG_PLACE_HOLDER, lastIndex);
            // no more {} arguments?
            if (argIndex == -1) {
                break;
            }
            if (sb == null) {
                // we build this lazily in case there is no {} in the msg
                sb = new StringBuilder(DEFAULT_MESSAGE_LENGTH);
            }
            // add the string before the arg-string
            sb.append(format, lastIndex, argIndex);
            // shift our last-index past the arg-string
            lastIndex = argIndex + 2;
            // add the argument, if we still have any
            if (argCount < arguments.length) {
                appendArgument(sb, arguments[argCount]);
            }
            argCount++;
        }
        if (sb == null) {
            return format;
        }
        // spit out the end of the msg
        sb.append(format, lastIndex, format.length());
        return sb.toString();
    }

    public static void appendArgument(StringBuilder sb, Object arg) {

        if (arg.getClass().isArray()) {
            // if we have an array argument
            sb.append('[');
            int length = Array.getLength(arg);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                // recursive in case we have an array of arrays
                appendArgument(sb, Array.get(arg, i));
            }
            sb.append(']');
        }
        else {
            // might as well to the toString here because we know it isn't null
            sb.append(arg);
        }
    }

  
}
