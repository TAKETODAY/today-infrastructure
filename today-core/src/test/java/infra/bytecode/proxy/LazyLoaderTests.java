/*
 * Copyright 2017 - 2024 the original author or authors.
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
package infra.bytecode.proxy;

import org.junit.jupiter.api.Test;

import infra.bytecode.proxy.Enhancer;
import infra.bytecode.proxy.LazyLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyLoaderTests {

  private static class LazyBean {
    private String name;

    public LazyBean() { }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  public void testLazyLoader() {
    LazyLoader loader = new LazyLoader() {

      @Override
      public LazyBean loadObject() {
        System.err.println("loading object");
        final LazyBean lazyBean = new LazyBean();
        lazyBean.setName("TEST");
        return lazyBean;
      }
    };
    LazyBean obj = (LazyBean) Enhancer.create(LazyBean.class, loader);

    System.err.println(obj.toString());
    System.err.println(obj.getClass());
    System.err.println(obj.getName());

    assertEquals("TEST", obj.getName());
  }

}
