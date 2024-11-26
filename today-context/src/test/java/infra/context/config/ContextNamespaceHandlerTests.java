/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import infra.beans.FatalBeanException;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.context.support.MockEnvironment;
import infra.core.io.ClassPathResource;
import infra.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 17:51
 */
public class ContextNamespaceHandlerTests {

  @AfterEach
  void tearDown() {
    System.getProperties().remove("foo");
  }

  @Test
  void propertyPlaceholder() {
    ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "contextNamespaceHandlerTests-replace.xml", getClass());
    assertThat(applicationContext.getBean("string")).isEqualTo("bar");
    assertThat(applicationContext.getBean("nullString")).isEqualTo("null");
    applicationContext.close();
  }

  @Test
  void propertyPlaceholderSystemProperties() {
    String value = System.setProperty("foo", "spam");
    try {
      ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
              "contextNamespaceHandlerTests-system.xml", getClass());
      assertThat(applicationContext.getBean("string")).isEqualTo("spam");
      assertThat(applicationContext.getBean("fallback")).isEqualTo("none");
      applicationContext.close();
    }
    finally {
      if (value != null) {
        System.setProperty("foo", value);
      }
    }
  }

  @Test
  void propertyPlaceholderEnvironmentProperties() {
    MockEnvironment env = new MockEnvironment().withProperty("foo", "spam");
    GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
    applicationContext.setEnvironment(env);
    applicationContext.load(new ClassPathResource("contextNamespaceHandlerTests-simple.xml", getClass()));
    applicationContext.refresh();
    assertThat(applicationContext.getBean("string")).isEqualTo("spam");
    assertThat(applicationContext.getBean("fallback")).isEqualTo("none");
    applicationContext.close();
  }

  @Test
  void propertyPlaceholderLocation() {
    ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "contextNamespaceHandlerTests-location.xml", getClass());
    assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
    assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
    assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
    applicationContext.close();
  }

  @Test
  void propertyPlaceholderLocationWithSystemPropertyForOneLocation() {
    System.setProperty("properties",
            "classpath*:/infra/context/config/test-*.properties");
    try {
      ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
              "contextNamespaceHandlerTests-location-placeholder.xml", getClass());
      assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
      assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
      assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
      applicationContext.close();
    }
    finally {
      System.clearProperty("properties");
    }
  }

  @Test
  void propertyPlaceholderLocationWithSystemPropertyForMultipleLocations() {
    System.setProperty("properties",
            "classpath*:/infra/context/config/test-*.properties," +
                    "classpath*:/infra/context/config/empty-*.properties," +
                    "classpath*:/infra/context/config/missing-*.properties");
    try {
      ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
              "contextNamespaceHandlerTests-location-placeholder.xml", getClass());
      assertThat(applicationContext.getBean("foo")).isEqualTo("bar");
      assertThat(applicationContext.getBean("bar")).isEqualTo("foo");
      assertThat(applicationContext.getBean("spam")).isEqualTo("maps");
      applicationContext.close();
    }
    finally {
      System.clearProperty("properties");
    }
  }

  @Test
  void propertyPlaceholderLocationWithSystemPropertyMissing() {
    assertThatExceptionOfType(FatalBeanException.class)
            .isThrownBy(() -> new ClassPathXmlApplicationContext("contextNamespaceHandlerTests-location-placeholder.xml", getClass()))
            .havingRootCause()
            .isInstanceOf(PlaceholderResolutionException.class)
            .withMessage("Could not resolve placeholder 'foo' in value \"${foo}\"");
  }

  @Test
  void propertyPlaceholderIgnored() {
    ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "contextNamespaceHandlerTests-replace-ignore.xml", getClass());
    assertThat(applicationContext.getBean("string")).isEqualTo("${bar}");
    assertThat(applicationContext.getBean("nullString")).isEqualTo("null");
    applicationContext.close();
  }

  @Test
  void propertyOverride() {
    ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "contextNamespaceHandlerTests-override.xml", getClass());
    Date date = (Date) applicationContext.getBean("date");
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(42);
    applicationContext.close();
  }

}
