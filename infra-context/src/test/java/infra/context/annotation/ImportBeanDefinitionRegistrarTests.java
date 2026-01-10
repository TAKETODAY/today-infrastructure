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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.BeansException;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.MessageSource;
import infra.context.ResourceLoaderAware;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotationMetadata;

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
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
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
