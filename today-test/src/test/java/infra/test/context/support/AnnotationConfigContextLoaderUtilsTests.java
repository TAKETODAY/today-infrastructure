/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
