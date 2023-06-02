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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class MockitoContextCustomizerFactoryTests {

  private final MockitoContextCustomizerFactory factory = new MockitoContextCustomizerFactory();

  @Test
  void getContextCustomizerWithoutAnnotationReturnsCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(NoMockBeanAnnotation.class, null);
    assertThat(customizer).isNotNull();
  }

  @Test
  void getContextCustomizerWithAnnotationReturnsCustomizer() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(WithMockBeanAnnotation.class, null);
    assertThat(customizer).isNotNull();
  }

  @Test
  void getContextCustomizerUsesMocksAsCacheKey() {
    ContextCustomizer customizer = this.factory.createContextCustomizer(WithMockBeanAnnotation.class, null);
    assertThat(customizer).isNotNull();
    ContextCustomizer same = this.factory.createContextCustomizer(WithSameMockBeanAnnotation.class, null);
    assertThat(customizer).isNotNull();
    ContextCustomizer different = this.factory.createContextCustomizer(WithDifferentMockBeanAnnotation.class, null);
    assertThat(different).isNotNull();
    assertThat(customizer.hashCode()).isEqualTo(same.hashCode());
    assertThat(customizer.hashCode()).isNotEqualTo(different.hashCode());
    assertThat(customizer).isEqualTo(customizer);
    assertThat(customizer).isEqualTo(same);
    assertThat(customizer).isNotEqualTo(different);
  }

  static class NoMockBeanAnnotation {

  }

  @MockBean({ Service1.class, Service2.class })
  static class WithMockBeanAnnotation {

  }

  @MockBean({ Service2.class, Service1.class })
  static class WithSameMockBeanAnnotation {

  }

  @MockBean({ Service1.class })
  static class WithDifferentMockBeanAnnotation {

  }

  interface Service1 {

  }

  interface Service2 {

  }

}
