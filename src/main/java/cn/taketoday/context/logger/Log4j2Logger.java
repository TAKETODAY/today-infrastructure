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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    @Override
    protected void logInternal(Level level, String msg, Throwable t, Object[] args) {

        if (t == null) {
            switch (level) {
                case DEBUG :
                    logger.debug(msg, args);
                    break;
                case INFO :
                    logger.info(msg, args);
                    break;
                case WARN :
                    logger.warn(msg, args);
                    break;
                case ERROR :
                    logger.error(msg, args);
                    break;
                case TRACE :
                    logger.trace(msg, args);
                    break;
                default:
                    logger.info(msg, args);
                    break;
            }
        }
        else
            switch (level) {
                case DEBUG :
                    logger.debug(msg, args, t);
                    break;
                case INFO :
                    logger.info(msg, args, t);
                    break;
                case WARN :
                    logger.warn(msg, args, t);
                    break;
                case ERROR :
                    logger.error(msg, args, t);
                    break;
                case TRACE :
                    logger.trace(msg, args, t);
                    break;
                default:
                    logger.info(msg, args, t);
                    break;
            }
    }

}
