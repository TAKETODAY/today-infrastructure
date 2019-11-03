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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author TODAY <br>
 *         2019-11-03 15:35
 */
public class CommonsLogger extends AbstractLogger {

    private final Log log;
    private final String name;

    public CommonsLogger(String name) {
        this.name = name;
        this.log = LogFactory.getLog(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    protected void logInternal(Level level, String msg, Throwable t, Object[] args) {

        switch (level) {
            case DEBUG :
                log.debug(buildMessage(msg, args), t);
                break;
            case INFO :
                log.info(buildMessage(msg, args), t);
                break;
            case WARN :
                log.warn(buildMessage(msg, args), t);
                break;
            case ERROR :
                log.error(buildMessage(msg, args), t);
                break;
            case TRACE :
                log.trace(buildMessage(msg, args), t);
                break;
            default:
                log.info(buildMessage(msg, args), t);
                break;
        }
    }
 
}
