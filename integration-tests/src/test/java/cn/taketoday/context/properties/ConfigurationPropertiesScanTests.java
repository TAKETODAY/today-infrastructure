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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.config.TypeExcludeFilter;
import cn.taketoday.context.properties.scan.valid.a.AScanConfiguration;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration.BFirstProperties;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration.BProperties;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration.BSecondProperties;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link ConfigurationPropertiesScan @ConfigurationPropertiesScan}.
 *
 * @author Madhura Bhave
 * @author Johnny Lim
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesScanTests {

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setup() {
    this.context = new AnnotationConfigApplicationContext();
  }

  @AfterEach
  void teardown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void scanImportBeanRegistrarShouldBeEnvironmentAwareWithRequiredProfile() {
    this.context.getEnvironment().addActiveProfile("test");
    load(TestConfiguration.class);
    assertThat(this.context.containsBean(
            "profile-cn.taketoday.context.properties.scan.valid.a.AScanConfiguration$MyProfileProperties"))
            .isTrue();
  }

  @Test
  void scanImportBeanRegistrarShouldBeEnvironmentAwareWithoutRequiredProfile() {
    load(TestConfiguration.class);
    assertThat(this.context.containsBean(
            "profile-cn.taketoday.context.properties.scan.valid.a.AScanConfiguration$MyProfileProperties"))
            .isFalse();
  }

  @Test
  void scanImportBeanRegistrarShouldBeResourceLoaderAwareWithRequiredResource() {
    DefaultResourceLoader resourceLoader = mock(DefaultResourceLoader.class);
    this.context.setResourceLoader(resourceLoader);
    willCallRealMethod().given(resourceLoader).getClassLoader();
    given(resourceLoader.getResource("test")).willReturn(new ByteArrayResource("test".getBytes()));
    load(TestConfiguration.class);
    assertThat(this.context.containsBean(
            "resource-cn.taketoday.context.properties.scan.valid.a.AScanConfiguration$MyResourceProperties"))
            .isTrue();
  }

  @Test
  void scanImportBeanRegistrarShouldBeResourceLoaderAwareWithoutRequiredResource() {
    load(TestConfiguration.class);
    assertThat(this.context.containsBean(
            "resource-cn.taketoday.context.properties.scan.valid.a.AScanConfiguration$MyResourceProperties"))
            .isFalse();
  }

  @Test
  void scanImportBeanRegistrarShouldUsePackageName() {
    load(TestAnotherPackageConfiguration.class);
    assertThat(this.context.getBeanNamesForType(BProperties.class)).containsOnly(
            "b.first-cn.taketoday.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties",
            "b.second-cn.taketoday.context.properties.scan.valid.b.BScanConfiguration$BSecondProperties");
  }

  @Test
  void scanImportBeanRegistrarShouldApplyTypeExcludeFilter() {
    this.context.getBeanFactory().registerSingleton("filter", new ConfigurationPropertiesTestTypeExcludeFilter());
    this.context.register(TestAnotherPackageConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(BProperties.class)).containsOnly(
            "b.first-cn.taketoday.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties");
  }

  @Test
  void scanShouldBindConfigurationProperties() {
    load(TestAnotherPackageConfiguration.class, "b.first.name=constructor", "b.second.number=42");
    assertThat(this.context.getBean(BFirstProperties.class).getName()).isEqualTo("constructor");
    assertThat(this.context.getBean(BSecondProperties.class).getNumber()).isEqualTo(42);
  }

  private void load(Class<?> configuration, String... inlinedProperties) {
    this.context.register(configuration);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, inlinedProperties);
    this.context.refresh();
  }

  @ConfigurationPropertiesScan(basePackageClasses = AScanConfiguration.class)
  static class TestConfiguration {

  }

  @ConfigurationPropertiesScan(basePackages = "cn.taketoday.context.properties.scan.valid.b")
  static class TestAnotherPackageConfiguration {

  }

  static class ConfigurationPropertiesTestTypeExcludeFilter extends TypeExcludeFilter {

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
            throws IOException {
      AssignableTypeFilter typeFilter = new AssignableTypeFilter(BFirstProperties.class);
      return !typeFilter.match(metadataReader, metadataReaderFactory);
    }

    @Override
    public boolean equals(Object o) {
      return (this == o);
    }

    @Override
    public int hashCode() {
      return Objects.hash(42);
    }

  }

}
