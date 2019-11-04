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

import java.util.function.Function;

import cn.taketoday.context.utils.ClassUtils;

/**
 * Factory that creates {@link Logger} instances.
 * 
 * @author TODAY <br>
 *         2019-11-04 19:06
 */
public abstract class LoggerFactory {

    public static final String LOG_TYPE_SYSTEM_PROPERTY = "logger.type";

    private static LoggerType loggerType;

    protected abstract Logger createLogger(String name);

    /**
     * Return a logger associated with a particular class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Return a logger associated with a particular class name.
     */
    public static Logger getLogger(String name) {
        return loggerType.create(name);
    }

    static {

        final String type = System.getProperty(LOG_TYPE_SYSTEM_PROPERTY);
        if (type != null) {
            try {
                loggerType = LoggerType.valueOf(type);
            }
            catch (Throwable e) {
                loggerType = LoggerType.DEFAULT;
                e.printStackTrace();
                System.err.println("Could not find valid log-type from system property '" +
                        LOG_TYPE_SYSTEM_PROPERTY + "', value '" + type + "'");
            }
        }
        else {
            for (final LoggerType logType : LoggerType.values()) {
                if (logType.isAvailable()) {
                    loggerType = logType;
                }
            }
        }
    }

    public enum LoggerType {

        SLF4J("org.slf4j.Logger", Slf4jLogger::new),
        COMMONS("org.apache.commons.logging.Log", CommonsLogger::new),
        LOG4J2("org.apache.logging.log4j.Logger", Log4j2Logger::new),
        DEFAULT("java.util.logging.Logger", JavaLoggingLogger::new);

        private final String name;
        private final LoggerFactory factory;

        LoggerType(String name, Function<String, Logger> factory) {
            this.name = name;
            this.factory = new LoggerFactory() {
                @Override
                protected Logger createLogger(String name) {
                    return factory.apply(name);
                }
            };
        }

        public boolean isAvailable() {
            return ClassUtils.isPresent(name);
        }

        public Logger create(String name) {
            return factory.createLogger(name);
        }
    }
}
