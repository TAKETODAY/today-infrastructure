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

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Juergen Hoeller
 */
public class Spr15275Tests {

  @Test
  public void testWithFactoryBean() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithFactoryBean.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    assertThat(context.getBean(Bar.class).foo).isSameAs(context.getBean(FooInterface.class));
  }

  @Test
  public void testWithAbstractFactoryBean() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithAbstractFactoryBean.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    assertThat(context.getBean(Bar.class).foo).isSameAs(context.getBean(FooInterface.class));
  }

  @Test
  public void testWithAbstractFactoryBeanForInterface() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithAbstractFactoryBeanForInterface.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    assertThat(context.getBean(Bar.class).foo).isSameAs(context.getBean(FooInterface.class));
  }

  @Test
  public void testWithAbstractFactoryBeanAsReturnType() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithAbstractFactoryBeanAsReturnType.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    assertThat(context.getBean(Bar.class).foo).isSameAs(context.getBean(FooInterface.class));
  }

  @Test
  public void testWithFinalFactoryBean() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithFinalFactoryBean.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    assertThat(context.getBean(Bar.class).foo).isSameAs(context.getBean(FooInterface.class));
  }

  @Test
  public void testWithFinalFactoryBeanAsReturnType() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithFinalFactoryBeanAsReturnType.class);
    assertThat(context.getBean(Bar.class).foo.toString()).isEqualTo("x");
    // not same due to fallback to raw FinalFactoryBean instance with repeated getObject() invocations
    assertThat(context.getBean(Bar.class).foo).isNotSameAs(context.getBean(FooInterface.class));
  }

  @Configuration
  protected static class ConfigWithFactoryBean {

    @Bean
    public FactoryBean<Foo> foo() {
      return new FactoryBean<Foo>() {
        @Override
        public Foo getObject() {
          return new Foo("x");
        }

        @Override
        public Class<?> getObjectType() {
          return Foo.class;
        }
      };
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  @Configuration
  protected static class ConfigWithAbstractFactoryBean {

    @Bean
    public FactoryBean<Foo> foo() {
      return new AbstractFactoryBean<Foo>() {
        @Override
        public Foo createBeanInstance() {
          return new Foo("x");
        }

        @Override
        public Class<?> getObjectType() {
          return Foo.class;
        }
      };
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  @Configuration
  protected static class ConfigWithAbstractFactoryBeanForInterface {

    @Bean
    public FactoryBean<FooInterface> foo() {
      return new AbstractFactoryBean<FooInterface>() {
        @Override
        public FooInterface createBeanInstance() {
          return new Foo("x");
        }

        @Override
        public Class<?> getObjectType() {
          return FooInterface.class;
        }
      };
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  @Configuration
  protected static class ConfigWithAbstractFactoryBeanAsReturnType {

    @Bean
    public AbstractFactoryBean<FooInterface> foo() {
      return new AbstractFactoryBean<FooInterface>() {
        @Override
        public FooInterface createBeanInstance() {
          return new Foo("x");
        }

        @Override
        public Class<?> getObjectType() {
          return Foo.class;
        }
      };
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  @Configuration
  protected static class ConfigWithFinalFactoryBean {

    @Bean
    public FactoryBean<FooInterface> foo() {
      return new FinalFactoryBean();
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  @Configuration
  protected static class ConfigWithFinalFactoryBeanAsReturnType {

    @Bean
    public FinalFactoryBean foo() {
      return new FinalFactoryBean();
    }

    @Bean
    public Bar bar() throws Exception {
      assertThat(foo().isSingleton()).isTrue();
      return new Bar(foo().getObject());
    }
  }

  private static final class FinalFactoryBean implements FactoryBean<FooInterface> {

    @Override
    public Foo getObject() {
      return new Foo("x");
    }

    @Override
    public Class<?> getObjectType() {
      return FooInterface.class;
    }
  }

  protected interface FooInterface {
  }

  protected static class Foo implements FooInterface {

    private final String value;

    public Foo(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  protected static class Bar {

    public final FooInterface foo;

    public Bar(FooInterface foo) {
      this.foo = foo;
    }
  }

}
