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
 * Factory that creates {@link Logger} instances.
 * 
 * @author TODAY <br>
 *         2019-11-04 19:06
 */
public abstract class LoggerFactory {

    public static final String LOG_TYPE_SYSTEM_PROPERTY = "logger.factory";

    public static LoggerFactory factory;

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
        if (factory == null) {
            synchronized (LoggerFactory.class) {
                return getLoggerFactory(name);
            }
        }
        return factory.createLogger(name);
    }

    private static Logger getLoggerFactory(String name) {

        final String type = System.getProperty(LOG_TYPE_SYSTEM_PROPERTY);
        if (type != null) {
            try {
                factory = (LoggerFactory) Class.forName(type).newInstance();
                return factory.createLogger(name);
            }
            catch (Throwable e) {
                e.printStackTrace();
                System.err.println("Could not find valid log-type from system property '" +
                        LOG_TYPE_SYSTEM_PROPERTY + "', value '" + type + "'");
            }
        }

        try {
            factory = new Slf4jLoggerFactory();
            return factory.createLogger(name);
        }
        catch (Throwable e) {}
        try {
            factory = new Log4j2LoggerFactory();
            return factory.createLogger(name);
        }
        catch (Throwable e) {}
        
        factory = new LoggerFactory() {
            @Override
            protected Logger createLogger(String name) {
                return new JavaLoggingLogger(name);
            }
        };
        return factory.createLogger(name);
    }

    public static void setFactory(final LoggerFactory type) {
        factory = type;
    }

}
