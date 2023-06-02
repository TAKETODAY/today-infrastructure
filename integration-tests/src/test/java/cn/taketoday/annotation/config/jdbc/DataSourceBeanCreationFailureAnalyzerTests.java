/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 14:02
 */
@ClassPathExclusions({ "h2-*.jar", "hsqldb-*.jar", "derby-*.jar" })
class DataSourceBeanCreationFailureAnalyzerTests {

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void failureAnalysisIsPerformed() {
    FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
    assertThat(failureAnalysis.getDescription()).contains("'url' attribute is not specified",
            "no embedded datasource could be configured", "Failed to determine a suitable driver class");
    assertThat(failureAnalysis.getAction()).contains(
            "If you want an embedded database (H2, HSQL or Derby), please put it on the classpath",
            "If you have database settings to be loaded from a particular profile you may need to activate it",
            "(no profiles are currently active)");
  }

  @Test
  void failureAnalysisIsPerformedWithActiveProfiles() {
    this.environment.setActiveProfiles("first", "second");
    FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
    assertThat(failureAnalysis.getAction()).contains("(the profiles first,second are currently active)");
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    BeanCreationException failure = createFailure(configuration);
    assertThat(failure).isNotNull();
    var failureAnalyzer = new DataSourceBeanCreationFailureAnalyzer(this.environment);
    return failureAnalyzer.analyze(failure);
  }

  private BeanCreationException createFailure(Class<?> configuration) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.setEnvironment(this.environment);
      context.register(configuration);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanCreationException ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class TestConfiguration {

  }

}