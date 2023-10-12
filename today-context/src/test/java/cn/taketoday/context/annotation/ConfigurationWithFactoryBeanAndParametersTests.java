/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case cornering the bug initially raised with SPR-8762, in which a
 * NullPointerException would be raised if a FactoryBean-returning @Bean method also
 * accepts parameters
 *
 * @author Chris Beams
 * @since 4.0
 */
public class ConfigurationWithFactoryBeanAndParametersTests {

  @Test
  public void test() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, Bar.class);
    assertThat(ctx.getBean(Bar.class).foo).isNotNull();
  }

  @Configuration
  static class Config {

    @Bean
    public FactoryBean<Foo> fb(@Value("42") String answer) {
      return new FooFactoryBean();
    }
  }

  static class Foo {
  }

  static class Bar {

    Foo foo;

    @Autowired
    public Bar(Foo foo) {
      this.foo = foo;
    }
  }

  static class FooFactoryBean implements FactoryBean<Foo> {

    @Override
    public Foo getObject() {
      return new Foo();
    }

    @Override
    public Class<Foo> getObjectType() {
      return Foo.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

}
