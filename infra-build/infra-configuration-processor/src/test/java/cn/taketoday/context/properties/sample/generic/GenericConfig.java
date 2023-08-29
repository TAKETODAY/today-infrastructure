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

package cn.taketoday.context.properties.sample.generic;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.NestedConfigurationProperty;

/**
 * Demonstrate that only relevant generics are stored in the metadata.
 *
 * @param <T> the type of the config
 * @author Stephane Nicoll
 */
@ConfigurationProperties("generic")
public class GenericConfig<T> {

  private final Foo foo = new Foo();

  public Foo getFoo() {
    return this.foo;
  }

  public static class Foo {

    private String name;

    @NestedConfigurationProperty
    private final Bar<String> bar = new Bar<>();

    private final Map<String, Bar<Integer>> stringToBar = new HashMap<>();

    private final Map<String, Integer> stringToInteger = new HashMap<>();

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Bar<String> getBar() {
      return this.bar;
    }

    public Map<String, Bar<Integer>> getStringToBar() {
      return this.stringToBar;
    }

    public Map<String, Integer> getStringToInteger() {
      return this.stringToInteger;
    }

  }

  public static class Bar<U> {

    private String name;

    @NestedConfigurationProperty
    private final Biz<String> biz = new Biz<>();

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Biz<String> getBiz() {
      return this.biz;
    }

    public static class Biz<V> {

      private String name;

      public String getName() {
        return this.name;
      }

      public void setName(String name) {
        this.name = name;
      }

    }

  }

}
