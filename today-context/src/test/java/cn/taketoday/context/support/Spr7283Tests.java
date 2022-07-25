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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Scott Andrews
 * @author Juergen Hoeller
 */
public class Spr7283Tests {

  @Test
  public void testListWithInconsistentElementType() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spr7283.xml", getClass());
    List<?> list = ctx.getBean("list", List.class);
    assertThat(list.size()).isEqualTo(2);
    boolean condition1 = list.get(0) instanceof A;
    assertThat(condition1).isTrue();
    boolean condition = list.get(1) instanceof B;
    assertThat(condition).isTrue();
  }

  public static class A {
    public A() { }
  }

  public static class B {
    public B() { }
  }

}
