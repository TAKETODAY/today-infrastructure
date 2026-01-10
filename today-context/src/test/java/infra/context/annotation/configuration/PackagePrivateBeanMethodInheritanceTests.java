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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

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
  public static class ReproConfig extends infra.context.annotation.configuration.a.BaseConfig {
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
  public static class WorkaroundConfig extends infra.context.annotation.configuration.a.BaseConfig {
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

