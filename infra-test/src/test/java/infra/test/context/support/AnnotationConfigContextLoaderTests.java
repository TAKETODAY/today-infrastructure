/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import infra.aot.hint.RuntimeHints;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class AnnotationConfigContextLoaderTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

  private final AnnotationConfigContextLoader contextLoader = new AnnotationConfigContextLoader();

  @Test
  void loadContextWithConfigContainingLocationsResultsInException() {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
            new String[] { "config.xml" }, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, contextLoader);
    assertThatIllegalStateException()
            .isThrownBy(() -> contextLoader.loadContext(mergedConfig))
            .withMessageContaining("does not support resource locations");
  }

  @Test
  void loadContextRefreshesContext() throws Exception {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
            AnnotatedFooConfigInnerClassTestCase.class, EMPTY_STRING_ARRAY,
            new Class<?>[] { AnnotatedFooConfigInnerClassTestCase.FooConfig.class },
            EMPTY_STRING_ARRAY, contextLoader);
    ApplicationContext context = contextLoader.loadContext(mergedConfig);
    assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
    ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
    assertThat(cac.isActive()).as("ApplicationContext is active").isTrue();
    assertThat(context.getBean(String.class)).isEqualTo("foo");
    cac.close();
  }

  @Test
  void loadContextForAotProcessingDoesNotRefreshContext() throws Exception {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
            AnnotatedFooConfigInnerClassTestCase.class, EMPTY_STRING_ARRAY,
            new Class<?>[] { AnnotatedFooConfigInnerClassTestCase.FooConfig.class }, EMPTY_STRING_ARRAY, contextLoader);
    ConfigurableApplicationContext context = contextLoader.loadContextForAotProcessing(mergedConfig, new RuntimeHints());
    assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
    assertThat(context.isActive()).as("ApplicationContext is active").isFalse();
    assertThat(Arrays.stream(context.getBeanDefinitionNames())).anyMatch(name -> name.contains("FooConfig"));
    context.close();
  }

  @Test
  void detectDefaultConfigurationClassesForAnnotatedInnerClass() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(ContextConfigurationInnerClassTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("annotated static ContextConfiguration should be considered.").hasSize(1);

    configClasses = contextLoader.detectDefaultConfigurationClasses(AnnotatedFooConfigInnerClassTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("annotated static FooConfig should be considered.").hasSize(1);
  }

  @Test
  void detectDefaultConfigurationClassesForMultipleAnnotatedInnerClasses() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(MultipleStaticConfigurationClassesTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("multiple annotated static classes should be considered.").hasSize(2);
  }

  @Test
  void detectDefaultConfigurationClassesForNonAnnotatedInnerClass() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PlainVanillaFooConfigInnerClassTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("non-annotated static FooConfig should NOT be considered.").isEmpty();
  }

  @Test
  void detectDefaultConfigurationClassesForFinalAnnotatedInnerClass() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(FinalConfigInnerClassTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("final annotated static Config should NOT be considered.").isEmpty();
  }

  @Test
  void detectDefaultConfigurationClassesForPrivateAnnotatedInnerClass() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PrivateConfigInnerClassTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("private annotated inner classes should NOT be considered.").isEmpty();
  }

  @Test
  void detectDefaultConfigurationClassesForNonStaticAnnotatedInnerClass() {
    Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(NonStaticConfigInnerClassesTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).as("non-static annotated inner classes should NOT be considered.").isEmpty();
  }

}
