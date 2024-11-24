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

package infra.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.support.GenericApplicationContext;
import infra.core.annotation.AliasFor;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.InfraRunner;
import infra.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that demonstrates how to register one or more {@code @Configuration}
 * classes via an {@link ApplicationContextInitializer} in a composed annotation so
 * that certain {@code @Configuration} classes are always registered whenever the composed
 * annotation is used, even if the composed annotation is used to declare additional
 * {@code @Configuration} classes.
 *
 * <p>This class has been implemented in response to the following Stack Overflow question:
 * <a href="https://stackoverflow.com/questions/35733344/can-contextconfiguration-in-a-custom-annotation-be-merged">
 * Can {@code @ContextConfiguration} in a custom annotation be merged?</a>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@InitializerConfiguredViaMetaAnnotationTests.ComposedContextConfiguration(BarConfig.class)
public class InitializerConfiguredViaMetaAnnotationTests {

  @Autowired
  String foo;

  @Autowired
  String bar;

  @Autowired
  List<String> strings;

  @Test
  public void beansFromInitializerAndComposedAnnotation() {
    assertThat(strings.size()).isEqualTo(2);
    assertThat(foo).isEqualTo("foo");
    assertThat(bar).isEqualTo("bar");
  }

  static class FooConfigInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      if (applicationContext instanceof GenericApplicationContext context) {
        new AnnotatedBeanDefinitionReader(context).register(FooConfig.class);
      }
    }
  }

  @ContextConfiguration(loader = AnnotationConfigContextLoader.class, initializers = FooConfigInitializer.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface ComposedContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
    Class<?>[] value() default {};
  }

}
