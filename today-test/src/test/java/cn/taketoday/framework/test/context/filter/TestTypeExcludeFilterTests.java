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

package cn.taketoday.framework.test.context.filter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestTypeExcludeFilter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TestTypeExcludeFilterTests {

  private final TestTypeExcludeFilter filter = new TestTypeExcludeFilter();

  private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

  @Test
  void matchesJUnit4TestClass() throws Exception {
    assertThat(this.filter.match(getMetadataReader(TestTypeExcludeFilterTests.class), this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesJUnitJupiterTestClass() throws Exception {
    assertThat(this.filter.match(getMetadataReader(JupiterTestExample.class), this.metadataReaderFactory)).isTrue();
  }

  @Test
  void matchesJUnitJupiterRepeatedTestClass() throws Exception {
    assertThat(this.filter.match(getMetadataReader(JupiterRepeatedTestExample.class), this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesJUnitJupiterTestFactoryClass() throws Exception {
    assertThat(this.filter.match(getMetadataReader(JupiterTestFactoryExample.class), this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesNestedConfiguration() throws Exception {
    assertThat(this.filter.match(getMetadataReader(NestedConfig.class), this.metadataReaderFactory)).isTrue();
  }

  @Test
  void matchesNestedConfigurationClassWithoutTestMethodsIfItHasRunWith() throws Exception {
    assertThat(this.filter.match(getMetadataReader(AbstractTestWithConfigAndRunWith.Config.class),
            this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesNestedConfigurationClassWithoutTestMethodsIfItHasExtendWith() throws Exception {
    assertThat(this.filter.match(getMetadataReader(AbstractJupiterTestWithConfigAndExtendWith.Config.class),
            this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesNestedConfigurationClassWithoutTestMethodsIfItHasTestable() throws Exception {
    assertThat(this.filter.match(getMetadataReader(AbstractJupiterTestWithConfigAndTestable.Config.class),
            this.metadataReaderFactory))
            .isTrue();
  }

  @Test
  void matchesTestConfiguration() throws Exception {
    assertThat(this.filter.match(getMetadataReader(SampleTestConfig.class), this.metadataReaderFactory)).isTrue();
  }

  @Test
  void doesNotMatchRegularConfiguration() throws Exception {
    assertThat(this.filter.match(getMetadataReader(SampleConfig.class), this.metadataReaderFactory)).isFalse();
  }

  @Test
  void matchesNestedConfigurationClassWithoutTestNgAnnotation() throws Exception {
    assertThat(this.filter.match(getMetadataReader(AbstractTestNgTestWithConfig.Config.class),
            this.metadataReaderFactory))
            .isTrue();
  }

  private MetadataReader getMetadataReader(Class<?> source) throws IOException {
    return this.metadataReaderFactory.getMetadataReader(source.getName());
  }

  @Configuration(proxyBeanMethods = false)
  static class NestedConfig {

  }

}
