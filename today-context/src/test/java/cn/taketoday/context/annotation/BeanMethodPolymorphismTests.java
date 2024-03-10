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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import cn.taketoday.aop.interceptor.SimpleTraceInterceptor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests regarding overloading and overriding of bean methods.
 * <p>Related to SPR-6618.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
@SuppressWarnings("resource")
public class BeanMethodPolymorphismTests {

  @Test
  void beanMethodDetectedOnSuperClass() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

    assertThat(ctx.getBean("testBean", BaseTestBean.class)).isNotNull();
  }

  @Test
  void beanMethodOverriding() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(OverridingConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  void beanMethodOverridingOnASM() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(OverridingConfig.class.getName()));
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  void beanMethodOverridingWithDifferentBeanName() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(OverridingConfigWithDifferentBeanName.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("myTestBean")).isFalse();
    assertThat(ctx.getBean("myTestBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("myTestBean")).isTrue();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
  }

  @Test
  void beanMethodOverridingWithDifferentBeanNameOnASM() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(OverridingConfigWithDifferentBeanName.class.getName()));
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("myTestBean")).isFalse();
    assertThat(ctx.getBean("myTestBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("myTestBean")).isTrue();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
  }

  @Test
  void beanMethodOverridingWithNarrowedReturnType() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(NarrowedOverridingConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  void beanMethodOverridingWithNarrowedReturnTypeOnASM() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(NarrowedOverridingConfig.class.getName()));
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  void beanMethodOverloadingWithoutInheritance() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithOverloading.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBean(String.class)).isEqualTo("regular");
  }

  @Test
  void beanMethodOverloadingWithoutInheritanceAndExtraDependency() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithOverloading.class);
    ctx.getBeanFactory().registerSingleton("anInt", 5);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
  }

  @Test
  void beanMethodOverloadingWithAdditionalMetadata() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("regular");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  @Test
  void beanMethodOverloadingWithAdditionalMetadataButOtherMethodExecuted() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
    ctx.getBeanFactory().registerSingleton("anInt", 5);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  @Test
  void beanMethodOverloadingWithInheritance() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(SubConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  @Test
    // SPR-11025
  void beanMethodOverloadingWithInheritanceAndList() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(SubConfigWithList.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  /**
   * When inheritance is not involved, it is still possible to override a bean method from
   * the container's point of view. This is not strictly 'overloading' of a method per se,
   * so it's referred to here as 'shadowing' to distinguish the difference.
   */
  @Test
  void beanMethodShadowing() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ShadowConfig.class);

    assertThat(ctx.getBean(String.class)).isEqualTo("shadow");
  }

  @Test
  void beanMethodThroughAopProxy() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class);
    ctx.register(AnnotationAwareAspectJAutoProxyCreator.class);
    ctx.register(TestAdvisor.class);
    ctx.refresh();
    ctx.getBean("testBean", BaseTestBean.class);
  }

  static class BaseTestBean {
  }

  static class ExtendedTestBean extends BaseTestBean {
  }

  @Configuration
  static class BaseConfig {

    @Bean
    public BaseTestBean testBean() {
      return new BaseTestBean();
    }
  }

  @Configuration
  static class Config extends BaseConfig {
  }

  @Configuration
  static class OverridingConfig extends BaseConfig {

    @Bean
    @Lazy
    @Override
    public BaseTestBean testBean() {
      return new BaseTestBean() {
        @Override
        public String toString() {
          return "overridden";
        }
      };
    }
  }

  @Configuration
  static class OverridingConfigWithDifferentBeanName extends BaseConfig {

    @Bean("myTestBean")
    @Lazy
    @Override
    public BaseTestBean testBean() {
      return new BaseTestBean() {
        @Override
        public String toString() {
          return "overridden";
        }
      };
    }
  }

  @Configuration
  static class NarrowedOverridingConfig extends BaseConfig {

    @Bean
    @Lazy
    @Override
    public ExtendedTestBean testBean() {
      return new ExtendedTestBean() {
        @Override
        public String toString() {
          return "overridden";
        }
      };
    }
  }

  @Configuration(enforceUniqueMethods = false)
  static class ConfigWithOverloading {

    @Bean
    String aString() {
      return "regular";
    }

    @Bean
    String aString(Integer dependency) {
      return "overloaded" + dependency;
    }
  }

  @Configuration(enforceUniqueMethods = false)
  static class ConfigWithOverloadingAndAdditionalMetadata {

    @Bean
    @Lazy
    String aString() {
      return "regular";
    }

    @Bean
    @Lazy
    String aString(Integer dependency) {
      return "overloaded" + dependency;
    }
  }

  @Configuration
  static class SuperConfig {

    @Bean
    String aString() {
      return "super";
    }
  }

  @Configuration
  static class SubConfig extends SuperConfig {

    @Bean
    Integer anInt() {
      return 5;
    }

    @Bean
    @Lazy
    String aString(Integer dependency) {
      return "overloaded" + dependency;
    }
  }

  @Configuration
  static class SubConfigWithList extends SuperConfig {

    @Bean
    Integer anInt() {
      return 5;
    }

    @Bean
    @Lazy
    String aString(List<Integer> dependency) {
      return "overloaded" + dependency.get(0);
    }
  }

  @Configuration
  @Import(SubConfig.class)
  static class ShadowConfig {

    @Bean
    String aString() {
      return "shadow";
    }
  }

  @SuppressWarnings("serial")
  public static class TestAdvisor extends DefaultPointcutAdvisor {

    public TestAdvisor() {
      super(new SimpleTraceInterceptor());
    }
  }

}
