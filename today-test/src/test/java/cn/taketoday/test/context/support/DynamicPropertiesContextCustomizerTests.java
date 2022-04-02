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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.test.context.DynamicPropertyRegistry;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DynamicPropertiesContextCustomizer}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DynamicPropertiesContextCustomizerTests {

  @Test
  void createWhenNonStaticDynamicPropertiesMethodThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> customizerFor("nonStatic"))
            .withMessage("@DynamicPropertySource method 'nonStatic' must be static");
  }

  @Test
  void createWhenBadDynamicPropertiesSignatureThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> customizerFor("badArgs"))
            .withMessage("@DynamicPropertySource method 'badArgs' must accept a single DynamicPropertyRegistry argument");
  }

  @Test
  void nullPropertyNameResultsInException() throws Exception {
    DynamicPropertiesContextCustomizer customizer = customizerFor("nullName");
    ConfigurableApplicationContext context = new StaticApplicationContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
            .withMessage("'name' must not be null or blank");
  }

  @Test
  void emptyPropertyNameResultsInException() throws Exception {
    DynamicPropertiesContextCustomizer customizer = customizerFor("emptyName");
    ConfigurableApplicationContext context = new StaticApplicationContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
            .withMessage("'name' must not be null or blank");
  }

  @Test
  void nullValueSupplierResultsInException() throws Exception {
    DynamicPropertiesContextCustomizer customizer = customizerFor("nullValueSupplier");
    ConfigurableApplicationContext context = new StaticApplicationContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
            .withMessage("'valueSupplier' must not be null");
  }

  @Test
  void customizeContextAddsPropertySource() throws Exception {
    ConfigurableApplicationContext context = new StaticApplicationContext();
    DynamicPropertiesContextCustomizer customizer = customizerFor("valid1", "valid2");
    customizer.customizeContext(context, mock(MergedContextConfiguration.class));
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.getRequiredProperty("p1a")).isEqualTo("v1a");
    assertThat(environment.getRequiredProperty("p1b")).isEqualTo("v1b");
    assertThat(environment.getRequiredProperty("p2a")).isEqualTo("v2a");
    assertThat(environment.getRequiredProperty("p2b")).isEqualTo("v2b");
  }

  @Test
  void equalsAndHashCode() {
    DynamicPropertiesContextCustomizer c1 = customizerFor("valid1", "valid2");
    DynamicPropertiesContextCustomizer c2 = customizerFor("valid1", "valid2");
    DynamicPropertiesContextCustomizer c3 = customizerFor("valid1");
    assertThat(c1.hashCode()).isEqualTo(c1.hashCode()).isEqualTo(c2.hashCode());
    assertThat(c1).isEqualTo(c1).isEqualTo(c2).isNotEqualTo(c3);
  }

  private static DynamicPropertiesContextCustomizer customizerFor(String... methods) {
    return new DynamicPropertiesContextCustomizer(findMethods(methods));
  }

  private static Set<Method> findMethods(String... names) {
    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(DynamicPropertySourceTestCase.class,
            method -> ObjectUtils.containsElement(names, method.getName()));
    return new LinkedHashSet<>(Arrays.asList(methods));
  }

  static class DynamicPropertySourceTestCase {

    void nonStatic(DynamicPropertyRegistry registry) {
    }

    static void badArgs(String bad) {
    }

    static void nullName(DynamicPropertyRegistry registry) {
      registry.add(null, () -> "A");
    }

    static void emptyName(DynamicPropertyRegistry registry) {
      registry.add("   ", () -> "A");
    }

    static void nullValueSupplier(DynamicPropertyRegistry registry) {
      registry.add("name", null);
    }

    static void valid1(DynamicPropertyRegistry registry) {
      registry.add("p1a", () -> "v1a");
      registry.add("p1b", () -> "v1b");
    }

    static void valid2(DynamicPropertyRegistry registry) {
      registry.add("p2a", () -> "v2a");
      registry.add("p2b", () -> "v2b");
    }

  }

}
