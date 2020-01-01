/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.LocationAwareLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * @author TODAY <br>
 *         2019-11-03 16:09
 */
public class Log4j2Logger extends AbstractLogger {

    private final Logger logger;

    public Log4j2Logger(String name) {
        this.logger = LogManager.getLogger(name);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    protected org.apache.logging.log4j.Level getLevel(Level level) {
        switch (level) {
            case DEBUG :
                return org.apache.logging.log4j.Level.DEBUG;
            case WARN :
                return org.apache.logging.log4j.Level.WARN;
            case ERROR :
                return org.apache.logging.log4j.Level.ERROR;
            case TRACE :
                return org.apache.logging.log4j.Level.ERROR;
            case INFO :
            default:
                return org.apache.logging.log4j.Level.INFO;
        }
    }

    @Override
    protected void logInternal(Level level, String format, Throwable t, Object[] args) {

        final Message message = new Message() {

            private static final long serialVersionUID = 1L;
            private String msg;

            @Override
            public Throwable getThrowable() {
                return t;
            }

            @Override
            public Object[] getParameters() {
                return args;
            }

            @Override
            public String getFormattedMessage() {
                if (msg == null) {
                    msg = MessageFormatter.format(format, args);
                }
                return msg;
            }

            @Override
            public String getFormat() {
                return msg;
            }
        };

        if (logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).logMessage(getLevel(level),
                                                      null, 
                                                      FQCN,
                                                      StackLocatorUtil.calcLocation(FQCN),
                                                      message, 
                                                      t);
        }
        else {
            logger.log(getLevel(level), message, t);
        }
    }

}

final class Log4j2LoggerFactory extends LoggerFactory {

    @Override
    protected cn.taketoday.context.logger.Logger createLogger(String name) {
        return new Log4j2Logger(name);
    }
}
