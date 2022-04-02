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
import java.util.Collections;
import java.util.List;

import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.DynamicPropertyRegistry;
import cn.taketoday.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicPropertiesContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class DynamicPropertiesContextCustomizerFactoryTests {

  private final DynamicPropertiesContextCustomizerFactory factory = new DynamicPropertiesContextCustomizerFactory();

  private final List<ContextConfigurationAttributes> configAttributes = Collections.emptyList();

  @Test
  void createContextCustomizerWhenNoAnnotatedMethodsReturnsNull() {
    DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
            NoDynamicPropertySource.class, this.configAttributes);
    assertThat(customizer).isNull();
  }

  @Test
  void createContextCustomizerWhenSingleAnnotatedMethodReturnsCustomizer() {
    DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
            SingleDynamicPropertySource.class, this.configAttributes);
    assertThat(customizer).isNotNull();
    assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1");
  }

  @Test
  void createContextCustomizerWhenMultipleAnnotatedMethodsReturnsCustomizer() {
    DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
            MultipleDynamicPropertySources.class, this.configAttributes);
    assertThat(customizer).isNotNull();
    assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1", "p2", "p3");
  }

  @Test
  void createContextCustomizerWhenAnnotatedMethodsInBaseClassReturnsCustomizer() {
    DynamicPropertiesContextCustomizer customizer = this.factory.createContextCustomizer(
            SubDynamicPropertySource.class, this.configAttributes);
    assertThat(customizer).isNotNull();
    assertThat(customizer.getMethods()).flatExtracting(Method::getName).containsOnly("p1", "p2");
  }

  static class NoDynamicPropertySource {

    void empty() {
    }

  }

  static class SingleDynamicPropertySource {

    @DynamicPropertySource
    static void p1(DynamicPropertyRegistry registry) {
    }

  }

  static class MultipleDynamicPropertySources {

    @DynamicPropertySource
    static void p1(DynamicPropertyRegistry registry) {
    }

    @DynamicPropertySource
    static void p2(DynamicPropertyRegistry registry) {
    }

    @DynamicPropertySource
    static void p3(DynamicPropertyRegistry registry) {
    }

  }

  static class BaseDynamicPropertySource {

    @DynamicPropertySource
    static void p1(DynamicPropertyRegistry registry) {
    }

  }

  static class SubDynamicPropertySource extends BaseDynamicPropertySource {

    @DynamicPropertySource
    static void p2(DynamicPropertyRegistry registry) {
    }

  }

}
