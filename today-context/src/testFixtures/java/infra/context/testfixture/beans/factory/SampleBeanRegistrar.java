/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.testfixture.beans.factory;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.core.env.Environment;
import jakarta.annotation.PostConstruct;

public class SampleBeanRegistrar implements BeanRegistrar {

  @Override
  public void register(BeanRegistry registry, Environment env) {
    registry.registerBean("foo", Foo.class);
    registry.registerBean("bar", Bar.class, spec -> spec
            .prototype()
            .lazyInit()
            .description("Custom description")
            .supplier(context -> new Bar(context.bean(Foo.class))));
    if (env.matchesProfiles("baz")) {
      registry.registerBean(Baz.class, spec -> spec
              .supplier(context -> new Baz("Hello World!")));
    }
    registry.registerBean(Init.class);
  }

  public record Foo() { }

  public record Bar(Foo foo) { }

  public record Baz(String message) { }

  public static class Init {

    public boolean initialized = false;

    @PostConstruct
    public void postConstruct() {
      initialized = true;
    }
  }
}
