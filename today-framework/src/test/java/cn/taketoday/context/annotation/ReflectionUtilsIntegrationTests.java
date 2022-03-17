/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ReflectionUtils methods as used against CGLIB-generated classes created
 * by ConfigurationClassEnhancer.
 *
 * @author Chris Beams
 * @see cn.taketoday.util.ReflectionUtilsTests
 * @since 4.0
 */
public class ReflectionUtilsIntegrationTests {

  @Test
  public void getUniqueDeclaredMethods_withCovariantReturnType_andCglibRewrittenMethodNames() throws Exception {
    Class<?> cglibLeaf = new ConfigurationClassEnhancer().enhance(Leaf.class, null);
    int m1MethodCount = 0;
    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(cglibLeaf);
    for (Method method : methods) {
      if (method.getName().equals("m1")) {
        m1MethodCount++;
      }
    }
    assertThat(m1MethodCount).isEqualTo(1);
    for (Method method : methods) {
      if (method.getName().contains("m1")) {
        assertThat(Integer.class).isEqualTo(method.getReturnType());
      }
    }
  }

  @Configuration
  static abstract class Parent {
    public abstract Number m1();
  }

  @Configuration
  static class Leaf extends Parent {
    @Override
    @Bean
    public Integer m1() {
      return 42;
    }
  }

}
