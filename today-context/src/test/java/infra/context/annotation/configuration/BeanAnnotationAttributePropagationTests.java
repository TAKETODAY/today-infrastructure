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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationClassPostProcessor;
import infra.context.annotation.DependsOn;
import infra.context.annotation.Lazy;
import infra.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests proving that the various attributes available via the {@link Bean}
 * annotation are correctly reflected in the {@link BeanDefinition} created when
 * processing the {@link Configuration} class.
 *
 * <p>Also includes tests proving that using {@link Lazy} and {@link Primary}
 * annotations in conjunction with Bean propagate their respective metadata
 * correctly into the resulting BeanDefinition
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class BeanAnnotationAttributePropagationTests {

  @Test
  public void autowireCandidateMetadataIsPropagated() {
    @Configuration
    class Config {
      @Bean(autowireCandidate = false)
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isAutowireCandidate()).as("autowire candidate flag was not propagated").isFalse();
  }

  @Test
  public void initMethodMetadataIsPropagated() {
    @Configuration
    class Config {
      @Bean(initMethods = "start")
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).getInitMethodName()).as("init method name was not propagated").isEqualTo("start");
  }

  @Test
  public void destroyMethodMetadataIsPropagated() {
    @Configuration
    class Config {
      @Bean(destroyMethod = "destroy")
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).getDestroyMethodName()).as("destroy method name was not propagated").isEqualTo("destroy");
  }

  @Test
  public void dependsOnMetadataIsPropagated() {
    @Configuration
    class Config {
      @Bean()
      @DependsOn({ "bar", "baz" })
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).getDependsOn()).as("dependsOn metadata was not propagated").isEqualTo(new String[] { "bar", "baz" });
  }

  @Test
  public void primaryMetadataIsPropagated() {
    @Configuration
    class Config {
      @Primary
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isPrimary()).as("primary metadata was not propagated").isTrue();
  }

  @Test
  public void primaryMetadataIsFalseByDefault() {
    @Configuration
    class Config {
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isPrimary()).as("@Bean methods should be non-primary by default").isFalse();
  }

  @Test
  public void lazyMetadataIsPropagated() {
    @Configuration
    class Config {
      @Lazy
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isLazyInit()).as("lazy metadata was not propagated").isTrue();
  }

  @Test
  public void lazyMetadataIsFalseByDefault() {
    @Configuration
    class Config {
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isLazyInit()).as("@Bean methods should be non-lazy by default").isFalse();
  }

  @Test
  public void defaultLazyConfigurationPropagatesToIndividualBeans() {
    @Lazy
    @Configuration
    class Config {
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isLazyInit()).as("@Bean methods declared in a @Lazy @Configuration should be lazily instantiated").isTrue();
  }

  @Test
  public void eagerBeanOverridesDefaultLazyConfiguration() {
    @Lazy
    @Configuration
    class Config {
      @Lazy(false)
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isLazyInit()).as("@Lazy(false) @Bean methods declared in a @Lazy @Configuration should be eagerly instantiated").isFalse();
  }

  @Test
  public void eagerConfigurationProducesEagerBeanDefinitions() {
    @Lazy(false)
    @Configuration
    class Config {  // will probably never happen, doesn't make much sense
      @Bean
      Object foo() { return null; }
    }

    assertThat(beanDef(Config.class).isLazyInit()).as("@Lazy(false) @Configuration should produce eager bean definitions").isFalse();
  }

  private AbstractBeanDefinition beanDef(Class<?> configClass) {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("config", new RootBeanDefinition(configClass));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
    pp.postProcessBeanFactory(factory);
    return (AbstractBeanDefinition) factory.getBeanDefinition("foo");
  }

}
