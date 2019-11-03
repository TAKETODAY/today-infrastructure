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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.logger;

import java.util.logging.Logger;

/**
 * @author TODAY <br>
 *         2019-11-03 14:45
 */
public class JavaLoggingLogger extends AbstractLogger {

    private final Logger logger;

    public JavaLoggingLogger(String name) {
        this.logger = java.util.logging.Logger.getLogger(name);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(java.util.logging.Level.FINEST);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(java.util.logging.Level.FINER);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(java.util.logging.Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(java.util.logging.Level.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(java.util.logging.Level.SEVERE);
    }

    private final java.util.logging.Level levelToJavaLevel(Level level) {
        switch (level) {
            case TRACE :
                return java.util.logging.Level.FINER;
            case DEBUG :
                return java.util.logging.Level.FINE;
            case INFO :
                return java.util.logging.Level.INFO;
            case WARN :
                return java.util.logging.Level.WARNING;
            case ERROR :
                return java.util.logging.Level.SEVERE;
            default:
                return java.util.logging.Level.INFO;
        }
    }

    @Override
    protected void logInternal(Level level, String msg, Throwable t, Object[] args) {
        logger.log(levelToJavaLevel(level), buildMessage(msg, args), t);
    }

}
