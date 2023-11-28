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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UnboundConfigurationPropertyFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class UnboundConfigurationPropertyFailureAnalyzerTests {

  @BeforeEach
  void setup() {
    LocaleContextHolder.setLocale(Locale.US);
  }

  @AfterEach
  void cleanup() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void bindExceptionDueToUnboundElements() {
    FailureAnalysis analysis = performAnalysis(UnboundElementsFailureConfiguration.class,
            "test.foo.listValue[0]=hello", "test.foo.listValue[2]=world");
    assertThat(analysis.getDescription()).contains(
            failure("test.foo.listvalue[2]", "world", "\"test.foo.listValue[2]\" from property source \"test\"",
                    "The elements [test.foo.listvalue[2]] were left unbound."));
  }

  private static String failure(String property, String value, String origin, String reason) {
    return String.format("Property: %s%n    Value: %s%n    Origin: %s%n    Reason: %s", property, value, origin,
            reason);
  }

  private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
    BeanCreationException failure = createFailure(configuration, environment);
    assertThat(failure).isNotNull();
    return new UnboundConfigurationPropertyFailureAnalyzer().analyze(failure);
  }

  private BeanCreationException createFailure(Class<?> configuration, String... environment) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      addEnvironment(context, environment);
      context.register(configuration);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanCreationException ex) {
      return ex;
    }
  }

  private void addEnvironment(AnnotationConfigApplicationContext context, String[] environment) {
    PropertySources sources = context.getEnvironment().getPropertySources();
    Map<String, Object> map = new HashMap<>();
    for (String pair : environment) {
      int index = pair.indexOf('=');
      String key = (index > 0) ? pair.substring(0, index) : pair;
      String value = (index > 0) ? pair.substring(index + 1) : "";
      map.put(key.trim(), value.trim());
    }
    sources.addFirst(new MapPropertySource("test", map));
  }

  @EnableConfigurationProperties(UnboundElementsFailureProperties.class)
  static class UnboundElementsFailureConfiguration {

  }

  @ConfigurationProperties("test.foo")
  static class UnboundElementsFailureProperties {

    private List<String> listValue;

    List<String> getListValue() {
      return this.listValue;
    }

    void setListValue(List<String> listValue) {
      this.listValue = listValue;
    }

  }

}
