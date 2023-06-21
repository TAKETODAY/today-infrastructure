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

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.framework.DefaultMethodInvocation;
import cn.taketoday.beans.testfixture.beans.IOther;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class MethodMatchersTests {

  private final Method EXCEPTION_GETMESSAGE;

  private final Method ITESTBEAN_SETAGE;

  private final Method ITESTBEAN_GETAGE;

  private final Method IOTHER_ABSQUATULATE;

  public MethodMatchersTests() throws Exception {
    EXCEPTION_GETMESSAGE = Exception.class.getMethod("getMessage");
    ITESTBEAN_GETAGE = ITestBean.class.getMethod("getAge");
    ITESTBEAN_SETAGE = ITestBean.class.getMethod("setAge", int.class);
    IOTHER_ABSQUATULATE = IOther.class.getMethod("absquatulate");
  }

  @Test
  public void testDefaultMatchesAll() throws Exception {
    MethodMatcher defaultMm = MethodMatcher.TRUE;
    assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
    assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
  }

  @Test
  public void testMethodMatcherTrueSerializable() throws Exception {
    assertThat(MethodMatcher.TRUE).isSameAs(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE));
  }

  @Test
  public void testSingle() throws Exception {
    MethodMatcher defaultMm = MethodMatcher.TRUE;
    assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
    assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isTrue();
    defaultMm = MethodMatchers.intersection(defaultMm, new StartsWithMatcher("get"));

    assertThat(defaultMm.matches(EXCEPTION_GETMESSAGE, Exception.class)).isTrue();
    assertThat(defaultMm.matches(ITESTBEAN_SETAGE, TestBean.class)).isFalse();
  }

  @Test
  public void testDynamicAndStaticMethodMatcherIntersection() throws Exception {
    MethodMatcher mm1 = MethodMatcher.TRUE;
    MethodMatcher mm2 = new TestDynamicMethodMatcherWhichMatches();
    MethodMatcher intersection = MethodMatchers.intersection(mm1, mm2);
    assertThat(intersection.isRuntime()).as("Intersection is a dynamic matcher").isTrue();
    assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class)).as("2Matched setAge method").isTrue();

    DefaultMethodInvocation defaultMethodInvocation = new DefaultMethodInvocation(
            null, new TestBean(), ITESTBEAN_SETAGE, TestBean.class, new Object[] { 5 }, null);

    assertThat(intersection.matches(defaultMethodInvocation)).as("3Matched setAge method").isTrue();
    // Knock out dynamic part
    intersection = MethodMatchers.intersection(intersection, new TestDynamicMethodMatcherWhichDoesNotMatch());
    assertThat(intersection.isRuntime()).as("Intersection is a dynamic matcher").isTrue();
    assertThat(intersection.matches(ITESTBEAN_SETAGE, TestBean.class)).as("2Matched setAge method").isTrue();
    assertThat(intersection.matches(defaultMethodInvocation)).as("3 - not Matched setAge method").isFalse();
  }

  @Test
  public void testStaticMethodMatcherUnion() throws Exception {
    MethodMatcher getterMatcher = new StartsWithMatcher("get");
    MethodMatcher setterMatcher = new StartsWithMatcher("set");
    MethodMatcher union = MethodMatchers.union(getterMatcher, setterMatcher);

    assertThat(union.isRuntime()).as("Union is a static matcher").isFalse();
    assertThat(union.matches(ITESTBEAN_SETAGE, TestBean.class)).as("Matched setAge method").isTrue();
    assertThat(union.matches(ITESTBEAN_GETAGE, TestBean.class)).as("Matched getAge method").isTrue();
    assertThat(union.matches(IOTHER_ABSQUATULATE, TestBean.class)).as("Didn't matched absquatulate method").isFalse();
  }

  @Test
  public void testUnionEquals() {
    MethodMatcher first = MethodMatchers.union(MethodMatcher.TRUE, MethodMatcher.TRUE);
    MethodMatcher second = new ComposablePointcut(MethodMatcher.TRUE).union(new ComposablePointcut(MethodMatcher.TRUE)).getMethodMatcher();
    assertThat(first.equals(second)).isTrue();
    assertThat(second.equals(first)).isTrue();
  }

  public static class StartsWithMatcher extends StaticMethodMatcher {

    private final String prefix;

    public StartsWithMatcher(String s) {
      this.prefix = s;
    }

    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith(prefix);
    }
  }

  private static class TestDynamicMethodMatcherWhichMatches extends DynamicMethodMatcher {

    @Override
    public boolean matches(MethodInvocation invocation) {
      return true;
    }
  }

  private static class TestDynamicMethodMatcherWhichDoesNotMatch extends DynamicMethodMatcher {

    @Override
    public boolean matches(MethodInvocation invocation) {
      return false;
    }
  }

}
