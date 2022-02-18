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

import java.util.List;

import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.interceptor.SimpleTraceInterceptor;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
  public void beanMethodDetectedOnSuperClass() {
    StandardApplicationContext ctx = new StandardApplicationContext(Config.class);
    assertThat(ctx.getBean("testBean", TestBean.class)).isNotNull();
  }

  @Test
  public void beanMethodOverriding() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(OverridingConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", TestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  public void beanMethodOverridingOnASM() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("config", new BeanDefinition("config", OverridingConfig.class.getName()));
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", TestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  public void beanMethodOverridingWithNarrowedReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(NarrowedOverridingConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", TestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

  @Test
  public void beanMethodOverridingWithNarrowedReturnTypeOnASM() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("config", new BeanDefinition("config", NarrowedOverridingConfig.class.getName()));
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(ctx.getBean("testBean", TestBean.class).toString()).isEqualTo("overridden");
    assertThat(ctx.getBeanFactory().containsSingleton("testBean")).isTrue();
  }

/*  @Test
  public void beanMethodOverloadingWithoutInheritance() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ConfigWithOverloading.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBean(String.class)).isEqualTo("regular");
  }*/

  @Test
  public void beanMethodOverloadingWithoutInheritanceAndExtraDependency() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ConfigWithOverloading.class);
    ctx.getBeanFactory().registerSingleton("anInt", 5);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
  }
/*

  @Test
  public void beanMethodOverloadingWithAdditionalMetadata() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("regular");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }
*/

  @Test
  public void beanMethodOverloadingWithAdditionalMetadataButOtherMethodExecuted() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
    ctx.getBeanFactory().registerSingleton("anInt", 5);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  @Test
  public void beanMethodOverloadingWithInheritance() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(SubConfig.class);
    ctx.setAllowBeanDefinitionOverriding(false);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isFalse();
    assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
    assertThat(ctx.getBeanFactory().containsSingleton("aString")).isTrue();
  }

  // SPR-11025
  @Test
  public void beanMethodOverloadingWithInheritanceAndList() {
    StandardApplicationContext ctx = new StandardApplicationContext();
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
  public void beanMethodShadowing() {
    StandardApplicationContext ctx = new StandardApplicationContext(ShadowConfig.class);
    assertThat(ctx.getBean(String.class)).isEqualTo("shadow");
  }

  @Test
  public void beanMethodThroughAopProxy() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config.class);
//    ctx.register(AnnotationAwareAspectJAutoProxyCreator.class);
    ctx.register(TestAdvisor.class);
    ctx.refresh();
    ctx.getBean("testBean", TestBean.class);
  }

  @Configuration
  static class BaseConfig {

    @Bean
    public TestBean testBean() {
      return new TestBean();
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
    public TestBean testBean() {
      return new TestBean() {
        @Override
        public String toString() {
          return "overridden";
        }
      };
    }
  }

  static class ExtendedTestBean extends TestBean {
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

  @Configuration
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

  @Configuration
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
