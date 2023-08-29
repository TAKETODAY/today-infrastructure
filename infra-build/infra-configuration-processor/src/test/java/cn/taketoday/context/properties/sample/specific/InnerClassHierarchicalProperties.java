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

package cn.taketoday.context.properties.sample.specific;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.NestedConfigurationProperty;

/**
 * Demonstrate inner classes end up in metadata regardless of position in hierarchy and
 * without the use of {@link NestedConfigurationProperty @NestedConfigurationProperty}.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassHierarchicalProperties {

  private Foo foo;

  public Foo getFoo() {
    return this.foo;
  }

  public void setFoo(Foo foo) {
    this.foo = foo;
  }

  public static class Foo {

    private Bar bar;

    public Bar getBar() {
      return this.bar;
    }

    public void setBar(Bar bar) {
      this.bar = bar;
    }

    public static class Baz {

      private String blah;

      public String getBlah() {
        return this.blah;
      }

      public void setBlah(String blah) {
        this.blah = blah;
      }

    }

  }

  public static class Bar {

    private String bling;

    private Foo.Baz baz;

    public String getBling() {
      return this.bling;
    }

    public void setBling(String foo) {
      this.bling = foo;
    }

    public Foo.Baz getBaz() {
      return this.baz;
    }

    public void setBaz(Foo.Baz baz) {
      this.baz = baz;
    }

  }

}
