/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.testfixture;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * JUnit Platform {@link TestExecutionListener} that configures Java Util Logging
 * (JUL) from a file named {@code jul-test.properties} in the root of the classpath.
 *
 * <p>This allows for projects to configure JUL for a test suite, analogous to
 * log4j's support via {@code log4j2-test.xml}.
 *
 * <p>This listener can be automatically registered on the JUnit Platform by
 * adding the fully qualified name of this class to a file named
 * {@code /META-INF/services/org.junit.platform.launcher.TestExecutionListener}
 * &mdash; for example, under {@code src/test/resources}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class JavaUtilLoggingConfigurer implements TestExecutionListener {

  public static final String JUL_TEST_PROPERTIES_FILE = "jul-test.properties";

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JUL_TEST_PROPERTIES_FILE)) {
      LogManager.getLogManager().readConfiguration(inputStream);
    }
    catch (Exception ex) {
      System.err.println("WARNING: failed to configure Java Util Logging from classpath resource " +
              JUL_TEST_PROPERTIES_FILE);
      System.err.println(ex);
    }
  }
}
