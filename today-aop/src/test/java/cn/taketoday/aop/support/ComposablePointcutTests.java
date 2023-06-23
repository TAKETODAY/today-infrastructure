/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ComposablePointcutTests {

  public static MethodMatcher GETTER_METHOD_MATCHER = new StaticMethodMatcher() {
    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith("get");
    }
  };

  public static MethodMatcher GET_AGE_METHOD_MATCHER = new StaticMethodMatcher() {
    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().equals("getAge");
    }
  };

  public static MethodMatcher ABSQUATULATE_METHOD_MATCHER = new StaticMethodMatcher() {
    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().equals("absquatulate");
    }
  };

  public static MethodMatcher SETTER_METHOD_MATCHER = new StaticMethodMatcher() {
    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith("set");
    }
  };

  @Test
  public void testMatchAll() throws NoSuchMethodException {
    Pointcut pc = new ComposablePointcut();
    assertThat(pc.getClassFilter().matches(Object.class)).isTrue();
    assertThat(pc.getMethodMatcher().matches(Object.class.getMethod("hashCode"), Exception.class)).isTrue();
  }

  @Test
  public void testFilterByClass() throws NoSuchMethodException {
    ComposablePointcut pc = new ComposablePointcut();

    assertThat(pc.getClassFilter().matches(Object.class)).isTrue();

    ClassFilter cf = new RootClassFilter(Exception.class);
    pc.intersection(cf);
    assertThat(pc.getClassFilter().matches(Object.class)).isFalse();
    assertThat(pc.getClassFilter().matches(Exception.class)).isTrue();
    pc.intersection(new RootClassFilter(NestedRuntimeException.class));
    assertThat(pc.getClassFilter().matches(Exception.class)).isFalse();
    assertThat(pc.getClassFilter().matches(NestedRuntimeException.class)).isTrue();
    assertThat(pc.getClassFilter().matches(String.class)).isFalse();
    pc.union(new RootClassFilter(String.class));
    assertThat(pc.getClassFilter().matches(Exception.class)).isFalse();
    assertThat(pc.getClassFilter().matches(String.class)).isTrue();
    assertThat(pc.getClassFilter().matches(NestedRuntimeException.class)).isTrue();
  }

  @Test
  public void testUnionMethodMatcher() {
    // Matches the getAge() method in any class
    ComposablePointcut pc = new ComposablePointcut(ClassFilter.TRUE, GET_AGE_METHOD_MATCHER);
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isFalse();

    pc.union(GETTER_METHOD_MATCHER);
    // Should now match all getter methods
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();

    pc.union(ABSQUATULATE_METHOD_MATCHER);
    // Should now match absquatulate() as well
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    // But it doesn't match everything
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_SET_AGE, TestBean.class)).isFalse();
  }

  @Test
  public void testIntersectionMethodMatcher() {
    ComposablePointcut pc = new ComposablePointcut();
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    pc.intersection(GETTER_METHOD_MATCHER);
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(pc.getMethodMatcher().matches(PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    pc.intersection(GET_AGE_METHOD_MATCHER);
    // Use the Pointcuts matches method
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(pc, PointcutsTests.TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    ComposablePointcut pc1 = new ComposablePointcut();
    ComposablePointcut pc2 = new ComposablePointcut();

    assertThat(pc2).isEqualTo(pc1);
    assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

    pc1.intersection(GETTER_METHOD_MATCHER);

    assertThat(pc1.equals(pc2)).isFalse();
    assertThat(pc1.hashCode() == pc2.hashCode()).isFalse();

    pc2.intersection(GETTER_METHOD_MATCHER);

    assertThat(pc2).isEqualTo(pc1);
    assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

    pc1.union(GET_AGE_METHOD_MATCHER);
    pc2.union(GET_AGE_METHOD_MATCHER);

    assertThat(pc2).isEqualTo(pc1);
    assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());
  }

}
