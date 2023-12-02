/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jmx.export.naming;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.jmx.export.annotation.AnnotationJmxAttributeSource;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/2 14:43
 */
class MetadataNamingStrategyTests {

  private static final TestBean TEST_BEAN = new TestBean();

  private final MetadataNamingStrategy strategy;

  MetadataNamingStrategyTests() {
    this.strategy = new MetadataNamingStrategy();
    this.strategy.setDefaultDomain("com.example");
    this.strategy.setAttributeSource(new AnnotationJmxAttributeSource());
  }

  @Test
  void getObjectNameWhenBeanNameIsSimple() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "myBean");
    assertThat(name.getDomain()).isEqualTo("com.example");
    assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "myBean"));
  }

  @Test
  void getObjectNameWhenBeanNameIsValidObjectName() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "com.another:name=myBean");
    assertThat(name.getDomain()).isEqualTo("com.another");
    assertThat(name.getKeyPropertyList()).containsOnly(entry("name", "myBean"));
  }

  @Test
  void getObjectNameWhenBeanNamContainsComma() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "myBean,");
    assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"myBean,\""));
  }

  @Test
  void getObjectNameWhenBeanNamContainsEquals() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "my=Bean");
    assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"my=Bean\""));
  }

  @Test
  void getObjectNameWhenBeanNamContainsColon() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "my:Bean");
    assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"my:Bean\""));
  }

  @Test
  void getObjectNameWhenBeanNamContainsQuote() throws MalformedObjectNameException {
    ObjectName name = this.strategy.getObjectName(TEST_BEAN, "\"myBean\"");
    assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"\\\"myBean\\\"\""));
  }

  private Consumer<ObjectName> hasDefaultProperties(Object instance, String expectedName) {
    return objectName -> assertThat(objectName.getKeyPropertyList()).containsOnly(
            entry("type", ClassUtils.getShortName(instance.getClass())),
            entry("name", expectedName));
  }

  static class TestBean { }

}