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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author TODAY <br>
 *         2019-12-06 23:09
 */
public class LoggerTest {

    @Test
    public void testSlf4jLogger() throws Exception {
        LoggerFactory.setFactory(new Slf4jLoggerFactory());

        final Logger logger = LoggerFactory.getLogger(getClass());

        assertTrue(logger instanceof Slf4jLogger);
        assertEquals(logger.getName(), getClass().getName());

        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());

        logger.info("testSlf4jLogger");
        logger.warn("testSlf4jLogger");
        logger.error("testSlf4jLogger");
        logger.debug("testSlf4jLogger");
        logger.trace("testSlf4jLogger");
    }

    @Test
    public void testLog4jLogger() throws Exception {
        LoggerFactory.setFactory(new Log4j2LoggerFactory());
        final Logger logger = LoggerFactory.getLogger(getClass());

        assertTrue(logger instanceof Log4j2Logger);
        assertEquals(logger.getName(), getClass().getName());

        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());

        logger.info("testLog4jLogger");
        logger.warn("testLog4jLogger");
        logger.error("testLog4jLogger");
        logger.debug("testLog4jLogger");
        logger.trace("testLog4jLogger");

        LoggerFactory.setFactory(new Slf4jLoggerFactory());

    }

    @Test
    public void testJavaLoggingLogger() throws Exception {
        LoggerFactory.setFactory(new JavaLoggingFactory());
        final Logger logger = LoggerFactory.getLogger(getClass());

        assertTrue(logger instanceof JavaLoggingLogger);
        assertEquals(logger.getName(), getClass().getName());

        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(!logger.isDebugEnabled());
        assertTrue(!logger.isTraceEnabled());

        logger.info("testLog4jLogger");
        logger.warn("testLog4jLogger");
        logger.error("testLog4jLogger");
        logger.debug("testLog4jLogger");
        logger.trace("testLog4jLogger");

        LoggerFactory.setFactory(new Slf4jLoggerFactory());

    }
}
