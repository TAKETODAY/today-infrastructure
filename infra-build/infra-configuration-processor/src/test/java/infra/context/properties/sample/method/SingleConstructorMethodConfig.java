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

package infra.context.properties.sample.method;

import infra.context.properties.sample.ConfigurationProperties;

/**
 * Sample for testing method configuration that uses a constructor that should not be
 * associated to constructor binding.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class SingleConstructorMethodConfig {

  @ConfigurationProperties(prefix = "foo")
  public Foo foo() {
    return new Foo(new Object());
  }

  public static class Foo {

    private String name;

    private boolean flag;

    private final Object myService;

    public Foo(Object myService) {
      this.myService = myService;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isFlag() {
      return this.flag;
    }

    public void setFlag(boolean flag) {
      this.flag = flag;
    }

  }

}
