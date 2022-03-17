/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ImportBeanDefinitionRegistrar}.
 *
 * @author Oliver Gierke
 * @author Chris Beams
 */
public class ImportBeanDefinitionRegistrarTests {

  @Test
  public void shouldInvokeAwareMethodsInImportBeanDefinitionRegistrar() {
    StandardApplicationContext context = new StandardApplicationContext(Config.class);
    context.getBean(MessageSource.class);

    assertThat(SampleRegistrar.beanFactory).isEqualTo(context.getBeanFactory());
    assertThat(SampleRegistrar.classLoader).isEqualTo(context.getBeanFactory().getBeanClassLoader());
    assertThat(SampleRegistrar.resourceLoader).isNotNull();
    assertThat(SampleRegistrar.environment).isEqualTo(context.getEnvironment());
  }

  @Sample
  @Configuration
  static class Config {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(SampleRegistrar.class)
  public @interface Sample {
  }

  private static class SampleRegistrar
          implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware,
          ResourceLoaderAware, BeanFactoryAware, EnvironmentAware {

    static ClassLoader classLoader;
    static ResourceLoader resourceLoader;
    static BeanFactory beanFactory;
    static Environment environment;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      SampleRegistrar.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      SampleRegistrar.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      SampleRegistrar.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
      SampleRegistrar.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

    }
  }

}
