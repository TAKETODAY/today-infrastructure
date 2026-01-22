/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import infra.app.InfraApplication;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.DeferredImportSelector.Group;
import infra.context.annotation.DeferredImportSelector.Group.Entry;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.AutoConfigurationImportEvent;
import infra.context.annotation.config.AutoConfigurationImportFilter;
import infra.context.annotation.config.AutoConfigurationImportListener;
import infra.context.annotation.config.AutoConfigurationImportSelector;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.context.annotation.config.AutoConfigureAfter;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.annotation.config.ImportCandidates;
import infra.core.type.AnnotationMetadata;
import infra.mock.env.MockEnvironment;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 22:55
 */
@WithResource(name = "META-INF/config/infra.context.annotation.config.AutoConfiguration.imports", content = """
        com.example.one.FirstAutoConfiguration
        com.example.two.SecondAutoConfiguration
        com.example.three.ThirdAutoConfiguration
        com.example.four.FourthAutoConfiguration
        com.example.five.FifthAutoConfiguration
        com.example.six.SixthAutoConfiguration
        infra.app.config.AutoConfigurationImportSelectorTests$SeventhAutoConfiguration
        """)
class AutoConfigurationImportSelectorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final MockEnvironment environment = new MockEnvironment();

  private List<AutoConfigurationImportFilter> filters = new ArrayList<>();

  private final TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(null);

  @BeforeEach
  void setup() {
    setupImportSelector(this.importSelector);
  }

  @Test
  void importsAreSelectedWhenUsingEnableAutoConfiguration() {
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSameSizeAs(getAutoConfigurationClassNames());
    assertThat(getLastEvent().getExclusions()).isEmpty();
  }

  @Test
  void classExclusionsAreApplied() {
    String[] imports = selectImports(EnableAutoConfigurationWithClassExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    assertThat(getLastEvent().getExclusions()).contains(SeventhAutoConfiguration.class.getName());
  }

  @Test
  void classExclusionsAreAppliedWhenUsingApplication() {
    String[] imports = selectImports(ApplicationWithClassExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    assertThat(getLastEvent().getExclusions()).contains(SeventhAutoConfiguration.class.getName());
  }

  @Test
  void classNamesExclusionsAreApplied() {
    String[] imports = selectImports(EnableAutoConfigurationWithClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    assertThat(getLastEvent().getExclusions()).contains("com.example.one.FirstAutoConfiguration");
  }

  @Test
  void classNamesExclusionsAreAppliedWhenUsingInfraApplication() {
    String[] imports = selectImports(ApplicationWithClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    assertThat(getLastEvent().getExclusions()).contains("com.example.three.ThirdAutoConfiguration");
  }

  @Test
  void propertyExclusionsAreApplied() {
    this.environment.setProperty("infra.auto-configuration.exclude", "com.example.three.ThirdAutoConfiguration");
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
    assertThat(getLastEvent().getExclusions()).contains("com.example.three.ThirdAutoConfiguration");
  }

  @Test
  void severalPropertyExclusionsAreApplied() {
    this.environment.setProperty("infra.auto-configuration.exclude",
            "com.example.two.SecondAutoConfiguration,com.example.four.FourthAutoConfiguration");
    testSeveralPropertyExclusionsAreApplied();
  }

  @Test
  void severalPropertyExclusionsAreAppliedWithExtraSpaces() {
    this.environment.setProperty("infra.auto-configuration.exclude",
            "com.example.two.SecondAutoConfiguration , com.example.four.FourthAutoConfiguration  ");
    testSeveralPropertyExclusionsAreApplied();
  }

  @Test
  void severalPropertyYamlExclusionsAreApplied() {
    this.environment.setProperty("infra.auto-configuration.exclude[0]", "com.example.two.SecondAutoConfiguration");
    this.environment.setProperty("infra.auto-configuration.exclude[1]", "com.example.four.FourthAutoConfiguration");
    testSeveralPropertyExclusionsAreApplied();
  }

  private void testSeveralPropertyExclusionsAreApplied() {
    String[] imports = selectImports(BasicEnableAutoConfiguration.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
    assertThat(getLastEvent().getExclusions()).contains("com.example.two.SecondAutoConfiguration",
            "com.example.four.FourthAutoConfiguration");
  }

  @Test
  void combinedExclusionsAreApplied() {
    this.environment.setProperty("infra.auto-configuration.exclude", "com.example.one.FirstAutoConfiguration");
    String[] imports = selectImports(EnableAutoConfigurationWithClassAndClassNameExclusions.class);
    assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
    assertThat(getLastEvent().getExclusions()).contains("com.example.one.FirstAutoConfiguration",
            "com.example.five.FifthAutoConfiguration", SeventhAutoConfiguration.class.getName());
  }

  @Test
  @WithTestAutoConfigurationImportsResource
  @WithTestAutoConfigurationReplacementsResource
  void removedExclusionsAreApplied() {
    TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(
            TestAutoConfiguration.class);
    setupImportSelector(importSelector);
    AnnotationMetadata metadata = AnnotationMetadata.introspect(BasicEnableAutoConfiguration.class);
    assertThat(importSelector.selectImports(metadata)).contains(ReplacementAutoConfiguration.class.getName());
    this.environment.setProperty("infra.auto-configuration.exclude", DeprecatedAutoConfiguration.class.getName());
    assertThat(importSelector.selectImports(metadata)).doesNotContain(ReplacementAutoConfiguration.class.getName());
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
    this.environment.setProperty("infra.auto-configuration.exclude",
            "infra.app.config.AutoConfigurationImportSelectorTests.TestConfiguration");
    assertThatIllegalStateException().isThrownBy(() -> selectImports(BasicEnableAutoConfiguration.class));
  }

  @Test
  void nameAndPropertyExclusionsWhenNotPresentOnClasspathShouldNotThrowException() {
    this.environment.setProperty("infra.auto-configuration.exclude",
            "infra.app.config.DoesNotExist2");
    selectImports(EnableAutoConfigurationWithAbsentClassNameExclude.class);
    assertThat(getLastEvent().getExclusions()).containsExactlyInAnyOrder(
            "infra.app.config.DoesNotExist1",
            "infra.app.config.DoesNotExist2");
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
  void filterShouldSupportAware() {
    TestAutoConfigurationImportFilter filter = new TestAutoConfigurationImportFilter(new String[] {});
    this.filters.add(filter);
    selectImports(BasicEnableAutoConfiguration.class);
    assertThat(filter.beanFactory).isEqualTo(this.beanFactory);
  }

  @Test
  void getExclusionFilterReuseFilters() {
    String[] allImports = new String[] { "com.example.A", "com.example.B", "com.example.C" };
    this.filters.add(new TestAutoConfigurationImportFilter(allImports, 0));
    this.filters.add(new TestAutoConfigurationImportFilter(allImports, 2));
    assertThat(this.importSelector.getExclusionFilter().test("com.example.A")).isTrue();
    assertThat(this.importSelector.getExclusionFilter().test("com.example.B")).isFalse();
    assertThat(this.importSelector.getExclusionFilter().test("com.example.C")).isTrue();
  }

  @Test
  @WithTestAutoConfigurationImportsResource
  @WithTestAutoConfigurationReplacementsResource
  void sortingConsidersReplacements() {
    TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(
            TestAutoConfiguration.class);
    setupImportSelector(importSelector);
    AnnotationMetadata metadata = AnnotationMetadata.introspect(BasicEnableAutoConfiguration.class);
    assertThat(importSelector.selectImports(metadata)).containsExactly(
            AfterDeprecatedAutoConfiguration.class.getName(), ReplacementAutoConfiguration.class.getName());

    Group group = importSelector.getBootstrapContext().instantiate(importSelector.getImportGroup());
    group.process(metadata, importSelector);
    Stream<Entry> imports = StreamSupport.stream(group.selectImports().spliterator(), false);
    assertThat(imports.map(Entry::importClassName)).containsExactly(ReplacementAutoConfiguration.class.getName(),
            AfterDeprecatedAutoConfiguration.class.getName());
  }

  private AutoConfigurationImportEvent getLastEvent() {
    AutoConfigurationImportEvent result = this.importSelector.getLastEvent();
    assertThat(result).isNotNull();
    return result;
  }

  private void setupImportSelector(TestAutoConfigurationImportSelector importSelector) {
    importSelector.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
  }

  private String[] selectImports(Class<?> source) {
    return this.importSelector.selectImports(AnnotationMetadata.introspect(source));
  }

  private List<String> getAutoConfigurationClassNames() {
    return ImportCandidates.load(AutoConfiguration.class, Thread.currentThread().getContextClassLoader())
            .getCandidates();
  }

  private class TestAutoConfigurationImportSelector extends AutoConfigurationImportSelector {

    private AutoConfigurationImportEvent lastEvent;

    TestAutoConfigurationImportSelector(@Nullable Class<?> autoConfigurationAnnotation) {
      super(autoConfigurationAnnotation);
      setBootstrapContext(new BootstrapContext(environment, beanFactory));
    }

    @Override
    protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
      List<AutoConfigurationImportFilter> filters1 = AutoConfigurationImportSelectorTests.this.filters;
      for (AutoConfigurationImportFilter filter : filters1) {
        getBootstrapContext().invokeAwareMethods(filter);
      }
      return filters1;
    }

    @Override
    protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
      return Collections.singletonList((event) -> this.lastEvent = event);
    }

    AutoConfigurationImportEvent getLastEvent() {
      return this.lastEvent;
    }

    public BootstrapContext getBootstrapContext() {
      return bootstrapContext;
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

  @EnableAutoConfiguration(exclude = SeventhAutoConfiguration.class)
  private class EnableAutoConfigurationWithClassExclusions {

  }

  @EnableAutoConfiguration(exclude = SeventhAutoConfiguration.class)
  private class ApplicationWithClassExclusions {

  }

  @EnableAutoConfiguration(excludeName = "com.example.one.FirstAutoConfiguration")
  private class EnableAutoConfigurationWithClassNameExclusions {

  }

  @EnableAutoConfiguration(exclude = SeventhAutoConfiguration.class,
          excludeName = "com.example.five.FifthAutoConfiguration")
  private class EnableAutoConfigurationWithClassAndClassNameExclusions {

  }

  @EnableAutoConfiguration(exclude = TestConfiguration.class)
  private class EnableAutoConfigurationWithFaultyClassExclude {

  }

  @EnableAutoConfiguration(
          excludeName = "infra.app.config.AutoConfigurationImportSelectorTests.TestConfiguration")
  private class EnableAutoConfigurationWithFaultyClassNameExclude {

  }

  @EnableAutoConfiguration(excludeName = "infra.app.config.DoesNotExist1")
  private class EnableAutoConfigurationWithAbsentClassNameExclude {

  }

  @InfraApplication(excludeName = "com.example.three.ThirdAutoConfiguration")
  private class ApplicationWithClassNameExclusions {

  }

  static class DeprecatedAutoConfiguration {

  }

  static class ReplacementAutoConfiguration {

  }

  @AutoConfigureAfter(DeprecatedAutoConfiguration.class)
  static class AfterDeprecatedAutoConfiguration {

  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAutoConfiguration {

  }

  @AutoConfiguration
  public static final class SeventhAutoConfiguration {

  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @WithResource(
          name = "META-INF/config/infra.app.config.AutoConfigurationImportSelectorTests$TestAutoConfiguration.imports",
          content = """
                  infra.app.config.AutoConfigurationImportSelectorTests$AfterDeprecatedAutoConfiguration
                  infra.app.config.AutoConfigurationImportSelectorTests$ReplacementAutoConfiguration
                  """)
  @interface WithTestAutoConfigurationImportsResource {

  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @WithResource(
          name = "META-INF/config/infra.app.config.AutoConfigurationImportSelectorTests$TestAutoConfiguration.replacements",
          content = """
                  infra.app.config.AutoConfigurationImportSelectorTests$DeprecatedAutoConfiguration=\
                  infra.app.config.AutoConfigurationImportSelectorTests$ReplacementAutoConfiguration
                  """)
  @interface WithTestAutoConfigurationReplacementsResource {

  }

}
