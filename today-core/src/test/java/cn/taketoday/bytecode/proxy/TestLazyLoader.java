/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
package cn.taketoday.bytecode.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLazyLoader {

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
