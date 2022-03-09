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

package cn.taketoday.aop.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PointcutsTests {

  public static Method TEST_BEAN_SET_AGE;
  public static Method TEST_BEAN_GET_AGE;
  public static Method TEST_BEAN_GET_NAME;
  public static Method TEST_BEAN_ABSQUATULATE;

  static {
    try {
      TEST_BEAN_SET_AGE = TestBean.class.getMethod("setAge", int.class);
      TEST_BEAN_GET_AGE = TestBean.class.getMethod("getAge");
      TEST_BEAN_GET_NAME = TestBean.class.getMethod("getName");
      TEST_BEAN_ABSQUATULATE = TestBean.class.getMethod("absquatulate");
    }
    catch (Exception ex) {
      throw new RuntimeException("Shouldn't happen: error in test suite");
    }
  }

  /**
   * Matches only TestBean class, not subclasses
   */
  public static Pointcut allTestBeanMethodsPointcut = new StaticMethodMatcherPointcut() {
    @Override
    public ClassFilter getClassFilter() {
      return type -> type.equals(TestBean.class);
    }

    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return true;
    }
  };

  public static Pointcut allClassSetterPointcut = Pointcuts.SETTERS;

  // Subclass used for matching
  public static class MyTestBean extends TestBean {
  }

  public static Pointcut myTestBeanSetterPointcut = new StaticMethodMatcherPointcut() {
    @Override
    public ClassFilter getClassFilter() {
      return new RootClassFilter(MyTestBean.class);
    }

    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith("set");
    }
  };

  // Will match MyTestBeanSubclass
  public static Pointcut myTestBeanGetterPointcut = new StaticMethodMatcherPointcut() {
    @Override
    public ClassFilter getClassFilter() {
      return new RootClassFilter(MyTestBean.class);
    }

    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith("get");
    }
  };

  // Still more specific class
  public static class MyTestBeanSubclass extends MyTestBean {
  }

  public static Pointcut myTestBeanSubclassGetterPointcut = new StaticMethodMatcherPointcut() {
    @Override
    public ClassFilter getClassFilter() {
      return new RootClassFilter(MyTestBeanSubclass.class);
    }

    @Override
    public boolean matches(Method m, @Nullable Class<?> targetClass) {
      return m.getName().startsWith("get");
    }
  };

  public static Pointcut allClassGetterPointcut = Pointcuts.GETTERS;

  public static Pointcut allClassGetAgePointcut = new NameMatchMethodPointcut().addMethodName("getAge");

  public static Pointcut allClassGetNamePointcut = new NameMatchMethodPointcut().addMethodName("getName");

  @Test
  public void testTrue() {
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(Pointcut.TRUE, TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
  }

  @Test
  public void testMatches() {
    assertThat(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(allClassSetterPointcut, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, 6)).isFalse();
    assertThat(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(allClassGetterPointcut, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
  }

  /**
   * Should match all setters and getters on any class
   */
  @Test
  public void testUnionOfSettersAndGetters() {
    Pointcut union = Pointcuts.union(allClassGetterPointcut, allClassSetterPointcut);
    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
  }

  @Test
  public void testUnionOfSpecificGetters() {
    Pointcut union = Pointcuts.union(allClassGetAgePointcut, allClassGetNamePointcut);
    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, 6)).isFalse();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();

    // Union with all setters
    union = Pointcuts.union(union, allClassSetterPointcut);
    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();

    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, 6)).isTrue();
  }

  /**
   * Tests vertical composition. First pointcut matches all setters.
   * Second one matches all getters in the MyTestBean class. TestBean getters shouldn't pass.
   */
  @Test
  public void testUnionOfAllSettersAndSubclassSetters() {
    assertThat(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_SET_AGE, TestBean.class, 6)).isFalse();
    assertThat(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_SET_AGE, MyTestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(myTestBeanSetterPointcut, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();

    Pointcut union = Pointcuts.union(myTestBeanSetterPointcut, allClassGetterPointcut);
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBean.class)).isTrue();
    // Still doesn't match superclass setter
    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, MyTestBean.class, 6)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_SET_AGE, TestBean.class, 6)).isFalse();
  }

  /**
   * Intersection should be MyTestBean getAge() only:
   * it's the union of allClassGetAge and subclass getters
   */
  @Test
  public void testIntersectionOfSpecificGettersAndSubclassGetters() {
    assertThat(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(allClassGetAgePointcut, TEST_BEAN_GET_AGE, MyTestBean.class)).isTrue();
    assertThat(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_NAME, MyTestBean.class)).isTrue();
    assertThat(Pointcuts.matches(myTestBeanGetterPointcut, TEST_BEAN_GET_AGE, MyTestBean.class)).isTrue();

    Pointcut intersection = Pointcuts.intersection(allClassGetAgePointcut, myTestBeanGetterPointcut);
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBean.class)).isTrue();
    // Matches subclass of MyTestBean
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class)).isTrue();

    // Now intersection with MyTestBeanSubclass getters should eliminate MyTestBean target
    intersection = Pointcuts.intersection(intersection, myTestBeanSubclassGetterPointcut);
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBean.class)).isFalse();
    // Still matches subclass of MyTestBean
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class)).isTrue();

    // Now union with all TestBean methods
    Pointcut union = Pointcuts.union(intersection, allTestBeanMethodsPointcut);
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_NAME, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_NAME, MyTestBean.class)).isFalse();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBean.class)).isFalse();
    // Still matches subclass of MyTestBean
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_NAME, MyTestBeanSubclass.class)).isFalse();
    assertThat(Pointcuts.matches(union, TEST_BEAN_GET_AGE, MyTestBeanSubclass.class)).isTrue();

    assertThat(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, TestBean.class)).isTrue();
    assertThat(Pointcuts.matches(union, TEST_BEAN_ABSQUATULATE, MyTestBean.class)).isFalse();
  }

  /**
   * The intersection of these two pointcuts leaves nothing.
   */
  @Test
  public void testSimpleIntersection() {
    Pointcut intersection = Pointcuts.intersection(allClassGetterPointcut, allClassSetterPointcut);
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_SET_AGE, TestBean.class, 6)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_GET_AGE, TestBean.class)).isFalse();
    assertThat(Pointcuts.matches(intersection, TEST_BEAN_ABSQUATULATE, TestBean.class)).isFalse();
  }

}
