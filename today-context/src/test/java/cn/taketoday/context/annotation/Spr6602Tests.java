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
import cn.taketoday.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that FactoryBean semantics are the same in Configuration
 * classes as in XML.
 *
 * @author Chris Beams
 */
public class Spr6602Tests {

  @Test
  public void testConfigurationClassBehavior() throws Exception {
    doAssertions(new AnnotationConfigApplicationContext(FooConfig.class));
  }

  private void doAssertions(ApplicationContext ctx) throws Exception {
    Foo foo = ctx.getBean(Foo.class);

    Bar bar1 = ctx.getBean(Bar.class);
    Bar bar2 = ctx.getBean(Bar.class);
    assertThat(bar1).isEqualTo(bar2);
    assertThat(bar1).isEqualTo(foo.bar);

    BarFactory barFactory1 = ctx.getBean(BarFactory.class);
    BarFactory barFactory2 = ctx.getBean(BarFactory.class);
    assertThat(barFactory1).isEqualTo(barFactory2);

    Bar bar3 = barFactory1.getObject();
    Bar bar4 = barFactory1.getObject();
    assertThat(bar3).isNotEqualTo(bar4);
  }

  @Configuration
  public static class FooConfig {

    @Bean
    public Foo foo() throws Exception {
      return new Foo(barFactory().getObject());
    }

    @Bean
    public BarFactory barFactory() {
      return new BarFactory();
    }
  }

  public static class Foo {

    final Bar bar;

    public Foo(Bar bar) {
      this.bar = bar;
    }
  }

  public static class Bar { }

  public static class BarFactory implements FactoryBean<Bar> {

    @Override
    public Bar getObject() throws Exception {
      return new Bar();
    }

    @Override
    public Class<? extends Bar> getObjectType() {
      return Bar.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }

}
