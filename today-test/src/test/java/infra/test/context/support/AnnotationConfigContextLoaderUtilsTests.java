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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Configuration;

import static infra.test.context.support.AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AnnotationConfigContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class AnnotationConfigContextLoaderUtilsTests {

  @Test
  void detectDefaultConfigurationClassesWithNullDeclaringClass() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            detectDefaultConfigurationClasses(null));
  }

  @Test
  void detectDefaultConfigurationClassesWithoutConfigurationClass() {
    Class<?>[] configClasses = detectDefaultConfigurationClasses(NoConfigTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses.length).isEqualTo(0);
  }

  @Test
  void detectDefaultConfigurationClassesWithExplicitConfigurationAnnotation() {
    Class<?>[] configClasses = detectDefaultConfigurationClasses(ExplicitConfigTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).isEqualTo(new Class<?>[] { ExplicitConfigTestCase.Config.class });
  }

  @Test
  void detectDefaultConfigurationClassesWithConfigurationMetaAnnotation() {
    Class<?>[] configClasses = detectDefaultConfigurationClasses(MetaAnnotatedConfigTestCase.class);
    assertThat(configClasses).isNotNull();
    assertThat(configClasses).isEqualTo(new Class<?>[] { MetaAnnotatedConfigTestCase.Config.class });
  }

  private static class NoConfigTestCase {

  }

  private static class ExplicitConfigTestCase {

    @Configuration
    static class Config {
    }
  }

  @Configuration
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private static @interface MetaConfig {
  }

  private static class MetaAnnotatedConfigTestCase {

    @MetaConfig
    static class Config {
    }
  }

}
