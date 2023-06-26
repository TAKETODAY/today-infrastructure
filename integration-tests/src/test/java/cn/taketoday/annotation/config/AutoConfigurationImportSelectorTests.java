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

package cn.taketoday.annotation.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.annotation.config.context.LifecycleAutoConfiguration;
import cn.taketoday.annotation.config.context.MessageSourceAutoConfiguration;
import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigurationImportEvent;
import cn.taketoday.context.annotation.config.AutoConfigurationImportFilter;
import cn.taketoday.context.annotation.config.AutoConfigurationImportListener;
import cn.taketoday.context.annotation.config.AutoConfigurationImportSelector;
import cn.taketoday.context.annotation.config.AutoConfigurationMetadata;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.annotation.config.ImportCandidates;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 22:55
 */
class AutoConfigurationImportSelectorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final MockEnvironment environment = new MockEnvironment();

  private List<AutoConfigurationImportFilter> filters = new ArrayList<>();

  private final TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector();

  @Test
  void importsAreSelectedWhenUsingEnableAutoConfiguration() {
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSameSizeAs(getAutoConfigurationClassNames());
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions()).isEmpty();
  }

  @Test
  void classExclusionsAreApplied() {
    String[] imports = selectImports(EnableAutoConfigurationWithClassExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(LifecycleAutoConfiguration.class.getName());
  }

  @Test
  void classExclusionsAreAppliedWhenUsingSpringBootApplication() {
    String[] imports = selectImports(ApplicationWithClassExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(LifecycleAutoConfiguration.class.getName());
  }

  @Test
  void classNamesExclusionsAreApplied() {
    String[] imports = selectImports(EnableAutoConfigurationWithClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(PropertyPlaceholderAutoConfiguration.class.getName());
  }

  @Test
  void classNamesExclusionsAreAppliedWhenUsingInfraApplication() {
    String[] imports = selectImports(ApplicationWithClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(PropertyPlaceholderAutoConfiguration.class.getName());
  }

  @Test
  void propertyExclusionsAreApplied() {
    this.environment.setProperty("infra.autoconfigure.exclude", LifecycleAutoConfiguration.class.getName());
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(LifecycleAutoConfiguration.class.getName());
  }

  @Test
  void severalPropertyExclusionsAreApplied() {
    this.environment.setProperty("infra.autoconfigure.exclude",
            LifecycleAutoConfiguration.class.getName() + "," + PropertyPlaceholderAutoConfiguration.class.getName());
    testSeveralPropertyExclusionsAreApplied();
  }

  @Test
  void severalPropertyExclusionsAreAppliedWithExtraSpaces() {
    this.environment.setProperty("infra.autoconfigure.exclude",
            LifecycleAutoConfiguration.class.getName() + " , " + PropertyPlaceholderAutoConfiguration.class.getName() + " ");
    testSeveralPropertyExclusionsAreApplied();
  }

  @Test
  void severalPropertyYamlExclusionsAreApplied() {
    this.environment.setProperty("infra.autoconfigure.exclude[0]", LifecycleAutoConfiguration.class.getName());
    this.environment.setProperty("infra.autoconfigure.exclude[1]", PropertyPlaceholderAutoConfiguration.class.getName());
    testSeveralPropertyExclusionsAreApplied();
  }

  private void testSeveralPropertyExclusionsAreApplied() {
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions())
            .contains(LifecycleAutoConfiguration.class.getName(), PropertyPlaceholderAutoConfiguration.class.getName());
  }

  @Test
  void combinedExclusionsAreApplied() {
    this.environment.setProperty("infra.autoconfigure.exclude", MessageSourceAutoConfiguration.class.getName());
    String[] imports = selectImports(EnableAutoConfigurationWithClassAndClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions()).contains(
            LifecycleAutoConfiguration.class.getName(), PropertyPlaceholderAutoConfiguration.class.getName(),
            MessageSourceAutoConfiguration.class.getName());
  }

  @Test
  void nonAutoConfigurationClassExclusionsShouldThrowException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassExclude.class));
  }

  @Test
  void nonAutoConfigurationClassNameExclusionsWhenPresentOnClassPathShouldThrowException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassNameExclude.class));
  }

  @Test
  void nonAutoConfigurationPropertyExclusionsWhenPresentOnClassPathShouldThrowException() {
//    /AutoConfigurationImportSelectorTests.java:165
    this.environment.setProperty("infra.autoconfigure.exclude",
            "cn.taketoday.annotation.config.AutoConfigurationImportSelectorTests.TestConfiguration");
    assertThatIllegalStateException().isThrownBy(() -> selectImports(BasicEnableAutoConfiguration.class));
  }

  @Test
  void nameAndPropertyExclusionsWhenNotPresentOnClasspathShouldNotThrowException() {
    this.environment.setProperty("infra.autoconfigure.exclude",
            "cn.taketoday.context.annotation.config.DoesNotExist2");
    selectImports(EnableAutoConfigurationWithAbsentClassNameExclude.class);
    Assertions.assertThat(this.importSelector.getLastEvent().getExclusions()).containsExactlyInAnyOrder(
            "cn.taketoday.context.annotation.config.DoesNotExist1",
            "cn.taketoday.context.annotation.config.DoesNotExist2");
  }

  @Test
  void filterShouldFilterImports() {
    String[] defaultImports = selectImports(BasicEnableAutoConfiguration.class);
    this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 1));
    this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 3, 4));
    String[] filtered = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(filtered).hasSize(defaultImports.length - 3);
    assertThat(filtered).doesNotContain(defaultImports[1], defaultImports[3], defaultImports[4]);
  }

  @Test
  void getExclusionFilterReuseFilters() {
    String[] allImports = new String[] { "com.example.A", "com.example.B", "com.example.C" };
    this.filters.add(new TestAutoConfigurationImportFilter(allImports, 0));
    this.filters.add(new TestAutoConfigurationImportFilter(allImports, 2));
    Assertions.assertThat(this.importSelector.getExclusionFilter().test("com.example.A")).isTrue();
    Assertions.assertThat(this.importSelector.getExclusionFilter().test("com.example.B")).isFalse();
    Assertions.assertThat(this.importSelector.getExclusionFilter().test("com.example.C")).isTrue();
  }

  private String[] selectImports(Class<?> source) {
    return this.importSelector.selectImports(AnnotationMetadata.introspect(source));
  }

  private List<String> getAutoConfigurationClassNames() {
    List<String> autoConfigurationClassNames = new ArrayList<>();
    ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
            .forEach(autoConfigurationClassNames::add);
    return autoConfigurationClassNames;
  }

  private class TestAutoConfigurationImportSelector extends AutoConfigurationImportSelector {

    private AutoConfigurationImportEvent lastEvent;

    private TestAutoConfigurationImportSelector() {
      super(new BootstrapContext(beanFactory, environment));
    }

    @Override
    protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
      return AutoConfigurationImportSelectorTests.this.filters;
    }

    @Override
    protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
      return Collections.singletonList((event) -> this.lastEvent = event);
    }

    AutoConfigurationImportEvent getLastEvent() {
      return this.lastEvent;
    }

  }

  static class TestAutoConfigurationImportFilter implements AutoConfigurationImportFilter, BeanFactoryAware {

    private final Set<String> nonMatching = new HashSet<>();

    private BeanFactory beanFactory;

    TestAutoConfigurationImportFilter(String[] configurations, int... nonMatching) {
      for (int i : nonMatching) {
        this.nonMatching.add(configurations[i]);
      }
    }

    @Override
    public boolean[] match(String[] autoConfigClasses, AutoConfigurationMetadata autoConfigMetadata) {
      boolean[] result = new boolean[autoConfigClasses.length];
      for (int i = 0; i < result.length; i++) {
        result[i] = !this.nonMatching.contains(autoConfigClasses[i]);
      }
      return result;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  private class TestConfiguration {

  }

  @EnableAutoConfiguration
  private class BasicEnableAutoConfiguration {

  }

  @EnableAutoConfiguration(exclude = LifecycleAutoConfiguration.class)
  private class EnableAutoConfigurationWithClassExclusions {

  }

  @EnableAutoConfiguration(exclude = LifecycleAutoConfiguration.class)
  private class ApplicationWithClassExclusions {

  }

  @EnableAutoConfiguration(excludeName = "cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration")
  private class EnableAutoConfigurationWithClassNameExclusions {

  }

  @EnableAutoConfiguration(exclude = PropertyPlaceholderAutoConfiguration.class,
                           excludeName = "cn.taketoday.annotation.config.context.LifecycleAutoConfiguration")
  private class EnableAutoConfigurationWithClassAndClassNameExclusions {

  }

  @EnableAutoConfiguration(exclude = TestConfiguration.class)
  private class EnableAutoConfigurationWithFaultyClassExclude {

  }

  @EnableAutoConfiguration(
          excludeName = "cn.taketoday.annotation.config.AutoConfigurationImportSelectorTests.TestConfiguration")
  private class EnableAutoConfigurationWithFaultyClassNameExclude {

  }

  @EnableAutoConfiguration(excludeName = "cn.taketoday.context.annotation.config.DoesNotExist1")
  private class EnableAutoConfigurationWithAbsentClassNameExclude {

  }

  @EnableAutoConfiguration(excludeName = "cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration")
  private class ApplicationWithClassNameExclusions {

  }

}
