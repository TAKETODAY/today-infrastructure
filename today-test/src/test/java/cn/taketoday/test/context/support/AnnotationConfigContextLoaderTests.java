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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;

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
            new Class<?>[] { AnnotatedFooConfigInnerClassTestCase.FooConfig.class },
            EMPTY_STRING_ARRAY, contextLoader);
    ApplicationContext context = contextLoader.loadContextForAotProcessing(mergedConfig);
    assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
    ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
    assertThat(cac.isActive()).as("ApplicationContext is active").isFalse();
    assertThat(Arrays.stream(context.getBeanDefinitionNames())).anyMatch(name -> name.contains("FooConfig"));
    cac.close();
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
