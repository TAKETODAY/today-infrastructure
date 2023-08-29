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
 * Demonstrate the auto-detection of inner config classes.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassProperties {

  private final Foo first = new Foo();

  private Foo second = new Foo();

  @NestedConfigurationProperty
  private final SimplePojo third = new SimplePojo();

  private Fourth fourth;

  public Foo getFirst() {
    return this.first;
  }

  public Foo getTheSecond() {
    return this.second;
  }

  public void setTheSecond(Foo second) {
    this.second = second;
  }

  public SimplePojo getThird() {
    return this.third;
  }

  public Fourth getFourth() {
    return this.fourth;
  }

  public void setFourth(Fourth fourth) {
    this.fourth = fourth;
  }

  public static class Foo {

    private String name;

    private final Bar bar = new Bar();

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Bar getBar() {
      return this.bar;
    }

    public static class Bar {

      private String name;

      public String getName() {
        return this.name;
      }

      public void setName(String name) {
        this.name = name;
      }

    }

  }

  public enum Fourth {

    YES, NO

  }

}
