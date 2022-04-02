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

package cn.taketoday.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.Runner;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;

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
@RunWith(Runner.class)
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
