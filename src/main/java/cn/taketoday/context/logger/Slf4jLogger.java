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

/**
 * @author TODAY <br>
 *         2019-11-03 13:55
 */
public class Slf4jLogger implements Logger {

    private final org.slf4j.Logger target;

    public Slf4jLogger(String className) {
        this.target = org.slf4j.LoggerFactory.getLogger(className);
    }

    @Override
    public boolean isTraceEnabled() {
        return target.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return target.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return target.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return target.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return target.isErrorEnabled();
    }
    //

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public void trace(String msg) {
        target.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        target.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        target.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        target.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        target.trace(msg, t);
    }

    @Override
    public void debug(String msg) {
        target.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        target.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        target.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        target.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        target.debug(msg, t);
    }

    @Override
    public void info(String msg) {
        target.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        target.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        target.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        target.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        target.info(msg, t);
    }

    @Override
    public void warn(String msg) {
        target.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        target.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        target.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        target.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        target.warn(msg, t);
    }

    @Override
    public void error(String msg) {
        target.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        target.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        target.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        target.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        target.error(msg, t);
    }

}
