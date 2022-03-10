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

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests semantics of declaring {@link BeanFactoryPostProcessor}-returning @Bean
 * methods, specifically as regards static @Bean methods and the avoidance of
 * container lifecycle issues when BFPPs are in the mix.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class ConfigurationClassAndBFPPTests {

  @Test
  public void autowiringFailsWithBFPPAsInstanceMethod() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsInstanceMethod.class);
    ctx.refresh();
    // instance method BFPP interferes with lifecycle -> autowiring fails!
    // WARN-level logging should have been issued about returning BFPP from non-static @Bean method
    assertThat(ctx.getBean(AutowiredConfigWithBFPPAsInstanceMethod.class).autowiredTestBean).isNull();
  }

  @Test
  public void autowiringSucceedsWithBFPPAsStaticMethod() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsStaticMethod.class);
    ctx.refresh();
    // static method BFPP does not interfere with lifecycle -> autowiring succeeds
    assertThat(ctx.getBean(AutowiredConfigWithBFPPAsStaticMethod.class).autowiredTestBean).isNotNull();
  }

  @Configuration
  static class TestBeanConfig {
    @Bean
    public TestBean testBean() {
      return new TestBean();
    }
  }

  @Configuration
  static class AutowiredConfigWithBFPPAsInstanceMethod {
    @Autowired
    TestBean autowiredTestBean;

    @Bean
    public BeanFactoryPostProcessor bfpp() {
      return beanFactory -> {
        // no-op
      };
    }
  }

  @Configuration
  static class AutowiredConfigWithBFPPAsStaticMethod {
    @Autowired
    TestBean autowiredTestBean;

    @Bean
    public static final BeanFactoryPostProcessor bfpp() {
      return beanFactory -> {
        // no-op
      };
    }
  }

  @Test
  public void staticBeanMethodsDoNotRespectScoping() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithStaticBeanMethod.class);
    ctx.refresh();
    assertThat(ConfigWithStaticBeanMethod.testBean()).isNotSameAs(ConfigWithStaticBeanMethod.testBean());
  }

  @Configuration
  static class ConfigWithStaticBeanMethod {
    @Bean
    public static TestBean testBean() {
      return new TestBean("foo");
    }
  }

}
