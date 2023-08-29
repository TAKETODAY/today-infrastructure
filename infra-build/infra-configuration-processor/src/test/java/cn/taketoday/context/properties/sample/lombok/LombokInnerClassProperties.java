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

package cn.taketoday.context.properties.sample.lombok;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.NestedConfigurationProperty;
import lombok.Data;

/**
 * Demonstrate the auto-detection of inner config classes using Lombok.
 *
 * @author Stephane Nicoll
 */
@Data
@ConfigurationProperties(prefix = "config")
@SuppressWarnings("unused")
public class LombokInnerClassProperties {

  private final Foo first = new Foo();

  private Foo second = new Foo();

  @NestedConfigurationProperty
  private final SimpleLombokPojo third = new SimpleLombokPojo();

  private Fourth fourth;

  // Only there to record the source method
  public SimpleLombokPojo getThird() {
    return this.third;
  }

  @Data
  public static class Foo {

    private String name;

    private final Bar bar = new Bar();

    @Data
    public static class Bar {

      private String name;

    }

  }

  public enum Fourth {

    YES, NO

  }

}
