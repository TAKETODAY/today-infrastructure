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

package infra.annotation.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import infra.annotation.config.jackson.JacksonAutoConfiguration;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.annotation.config.ImportAutoConfigurationImportSelector;
import infra.core.annotation.AliasFor;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.mock.env.MockEnvironment;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:04
 */
class ImportAutoConfigurationImportSelectorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final MockEnvironment environment = new MockEnvironment();

  private final ImportAutoConfigurationImportSelector importSelector
      = new TestImportAutoConfigurationImportSelector(new BootstrapContext(environment, beanFactory));

  @Test
  void importsAreSelected() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportFreeMarker.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsExactly(WebMvcAutoConfiguration.class.getName());
  }

  @Test
  void importsAreSelectedUsingClassesAttribute() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportFreeMarkerUsingClassesAttribute.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsExactly(WebMvcAutoConfiguration.class.getName());
  }

  @Test
  void importsAreSelectedFromImportsFile() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(FromImportsFile.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsExactly(
        "infra.freemarker.config.FreeMarkerAutoConfiguration",
        "infra.annotation.config.missing.MissingAutoConfiguration");
  }

  @Test
  void importsSelectedFromImportsFileIgnoreMissingOptionalClasses() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(
        FromImportsFileIgnoresMissingOptionalClasses.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsExactly(
        "infra.freemarker.config.FreeMarkerAutoConfiguration",
        "infra.annotation.config.thymeleaf.ThymeleafAutoConfiguration");
  }

  @Test
  void propertyExclusionsAreApplied() throws IOException {
    this.environment.setProperty("infra.auto-configuration.exclude", WebMvcAutoConfiguration.class.getName());
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(MultipleImports.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsExactly(JacksonAutoConfiguration.class.getName());
  }

  @Test
  void multipleImportsAreFound() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(MultipleImports.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsOnly(WebMvcAutoConfiguration.class.getName(),
        JacksonAutoConfiguration.class.getName());
  }

  @Test
  void selfAnnotatingAnnotationDoesNotCauseStackOverflow() throws IOException {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportWithSelfAnnotatingAnnotation.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsOnly(JacksonAutoConfiguration.class.getName());
  }

  @Test
  void exclusionsAreApplied() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(MultipleImportsWithExclusion.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsOnly(WebMvcAutoConfiguration.class.getName());
  }

  @Test
  void exclusionsWithoutImport() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(ExclusionWithoutImport.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).containsOnly(WebMvcAutoConfiguration.class.getName());
  }

  @Test
  void exclusionsAliasesAreApplied() throws Exception {
    AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportWithSelfAnnotatingAnnotationExclude.class);
    String[] imports = this.importSelector.selectImports(annotationMetadata);
    assertThat(imports).isEmpty();
  }

  @Test
  void determineImportsWhenUsingMetaWithoutClassesShouldBeEqual() throws Exception {
    Set<Object> set1 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedOne.class));
    Set<Object> set2 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedTwo.class));
    assertThat(set1).isEqualTo(set2);
    assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
  }

  @Test
  void determineImportsWhenUsingNonMetaWithoutClassesShouldBeSame() throws Exception {
    Set<Object> set1 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportAutoConfigurationWithUnrelatedOne.class));
    Set<Object> set2 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportAutoConfigurationWithUnrelatedTwo.class));
    assertThat(set1).isEqualTo(set2);
  }

  @Test
  void determineImportsWhenUsingNonMetaWithClassesShouldBeSame() throws Exception {
    Set<Object> set1 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportAutoConfigurationWithItemsOne.class));
    Set<Object> set2 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportAutoConfigurationWithItemsTwo.class));
    assertThat(set1).isEqualTo(set2);
  }

  @Test
  void determineImportsWhenUsingMetaExcludeWithoutClassesShouldBeEqual() throws Exception {
    Set<Object> set1 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
    Set<Object> set2 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedTwo.class));
    assertThat(set1).isEqualTo(set2);
    assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
  }

  @Test
  void determineImportsWhenUsingMetaDifferentExcludeWithoutClassesShouldBeDifferent() throws Exception {
    Set<Object> set1 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
    Set<Object> set2 = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedTwo.class));
    assertThat(set1).isNotEqualTo(set2);
  }

  @Test
  void determineImportsShouldNotSetPackageImport() throws Exception {
    Class<?> packageImportsClass = ClassUtils.resolveClassName(
        "infra.context.annotation.config.AutoConfigurationPackages.PackageImports", null);
    Set<Object> selectedImports = this.importSelector
        .determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
    for (Object selectedImport : selectedImports) {
      assertThat(selectedImport).isNotInstanceOf(packageImportsClass);
    }
  }

  private AnnotationMetadata getAnnotationMetadata(Class<?> source) throws IOException {
    return new SimpleMetadataReaderFactory().getMetadataReader(source.getName()).getAnnotationMetadata();
  }

  @ImportAutoConfiguration(WebMvcAutoConfiguration.class)
  static class ImportFreeMarker {

  }

  @ImportAutoConfiguration(classes = WebMvcAutoConfiguration.class)
  static class ImportFreeMarkerUsingClassesAttribute {

  }

  @ImportOne
  @ImportTwo
  static class MultipleImports {

  }

  @ImportOne
  @ImportTwo
  @ImportAutoConfiguration(exclude = JacksonAutoConfiguration.class)
  static class MultipleImportsWithExclusion {

  }

  @ImportOne
  @ImportAutoConfiguration(exclude = JacksonAutoConfiguration.class)
  static class ExclusionWithoutImport {

  }

  @SelfAnnotating
  static class ImportWithSelfAnnotatingAnnotation {

  }

  @SelfAnnotating(excludeAutoConfiguration = JacksonAutoConfiguration.class)
  static class ImportWithSelfAnnotatingAnnotationExclude {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ImportAutoConfiguration(WebMvcAutoConfiguration.class)
  @interface ImportOne {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ImportAutoConfiguration(JacksonAutoConfiguration.class)
  @interface ImportTwo {

  }

  @MetaImportAutoConfiguration
  @UnrelatedOne
  static class ImportMetaAutoConfigurationWithUnrelatedOne {

  }

  @MetaImportAutoConfiguration
  @UnrelatedTwo
  static class ImportMetaAutoConfigurationWithUnrelatedTwo {

  }

  @ImportAutoConfiguration
  @UnrelatedOne
  static class ImportAutoConfigurationWithUnrelatedOne {

  }

  @ImportAutoConfiguration
  @UnrelatedTwo
  static class ImportAutoConfigurationWithUnrelatedTwo {

  }

  @ImportAutoConfiguration(classes = JacksonAutoConfiguration.class)
  @UnrelatedOne
  static class ImportAutoConfigurationWithItemsOne {

  }

  @ImportAutoConfiguration(classes = JacksonAutoConfiguration.class)
  @UnrelatedTwo
  static class ImportAutoConfigurationWithItemsTwo {

  }

  @MetaImportAutoConfiguration(exclude = JacksonAutoConfiguration.class)
  @UnrelatedOne
  static class ImportMetaAutoConfigurationExcludeWithUnrelatedOne {

  }

  @MetaImportAutoConfiguration(exclude = JacksonAutoConfiguration.class)
  @UnrelatedTwo
  static class ImportMetaAutoConfigurationExcludeWithUnrelatedTwo {

  }

  @ImportAutoConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaImportAutoConfiguration {

    @AliasFor(annotation = ImportAutoConfiguration.class)
    Class<?>[] exclude() default {

    };

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface UnrelatedOne {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface UnrelatedTwo {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ImportAutoConfiguration(JacksonAutoConfiguration.class)
  @SelfAnnotating
  @interface SelfAnnotating {

    @AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
    Class<?>[] excludeAutoConfiguration() default {

    };

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ImportAutoConfiguration
  @interface FromImportsFile {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ImportAutoConfiguration
  @interface FromImportsFileIgnoresMissingOptionalClasses {

  }

  static class TestImportAutoConfigurationImportSelector extends ImportAutoConfigurationImportSelector {

    public TestImportAutoConfigurationImportSelector(BootstrapContext bootstrapContext) {
      setBootstrapContext(bootstrapContext);
    }

    @Override
    protected Collection<String> getStrategiesNames(Class<?> source) {
      if (source == MetaImportAutoConfiguration.class) {
        return Arrays.asList(JacksonAutoConfiguration.class.getName(),
            WebMvcAutoConfiguration.class.getName());
      }
      return super.getStrategiesNames(source);
    }

  }

}
