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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces SPR-8756, which has been marked as "won't fix" for reasons
 * described in the issue. Also demonstrates the suggested workaround.
 *
 * @author Chris Beams
 */
public class PackagePrivateBeanMethodInheritanceTests {

  @Test
  public void repro() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ReproConfig.class);
    ctx.refresh();
    Foo foo1 = ctx.getBean("foo1", Foo.class);
    Foo foo2 = ctx.getBean("foo2", Foo.class);
    ctx.getBean("packagePrivateBar", Bar.class); // <-- i.e. @Bean was registered
    assertThat(foo1.bar).isNotEqualTo(foo2.bar); // <-- i.e. @Bean *not* enhanced
  }

  @Test
  public void workaround() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(WorkaroundConfig.class);
    ctx.refresh();
    Foo foo1 = ctx.getBean("foo1", Foo.class);
    Foo foo2 = ctx.getBean("foo2", Foo.class);
    ctx.getBean("protectedBar", Bar.class);   // <-- i.e. @Bean was registered
    assertThat(foo1.bar).isEqualTo(foo2.bar); // <-- i.e. @Bean *was* enhanced
  }

  public static class Foo {
    final Bar bar;

    public Foo(Bar bar) {
      this.bar = bar;
    }
  }

  public static class Bar {
  }

  @Configuration
  public static class ReproConfig extends cn.taketoday.context.annotation.configuration.a.BaseConfig {
    @Bean
    public Foo foo1() {
      return new Foo(reproBar());
    }

    @Bean
    public Foo foo2() {
      return new Foo(reproBar());
    }
  }

  @Configuration
  public static class WorkaroundConfig extends cn.taketoday.context.annotation.configuration.a.BaseConfig {
    @Bean
    public Foo foo1() {
      return new Foo(workaroundBar());
    }

    @Bean
    public Foo foo2() {
      return new Foo(workaroundBar());
    }
  }
}

