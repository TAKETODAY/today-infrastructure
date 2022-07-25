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

package cn.taketoday.aop.aspectj;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.aspectj.AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException;

/**
 * Tests just the annotation binding part of {@link AspectJAdviceParameterNameDiscoverer};
 * see supertype for remaining tests.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class TigerAspectJAdviceParameterNameDiscovererTests extends AspectJAdviceParameterNameDiscovererTests {

  @Test
  public void testAtThis() {
    assertParameterNames(getMethod("oneAnnotation"), "@this(a)", new String[] { "a" });
  }

  @Test
  public void testAtTarget() {
    assertParameterNames(getMethod("oneAnnotation"), "@target(a)", new String[] { "a" });
  }

  @Test
  public void testAtArgs() {
    assertParameterNames(getMethod("oneAnnotation"), "@args(a)", new String[] { "a" });
  }

  @Test
  public void testAtWithin() {
    assertParameterNames(getMethod("oneAnnotation"), "@within(a)", new String[] { "a" });
  }

  @Test
  public void testAtWithincode() {
    assertParameterNames(getMethod("oneAnnotation"), "@withincode(a)", new String[] { "a" });
  }

  @Test
  public void testAtAnnotation() {
    assertParameterNames(getMethod("oneAnnotation"), "@annotation(a)", new String[] { "a" });
  }

  @Test
  public void testAmbiguousAnnotationTwoVars() {
    assertException(getMethod("twoAnnotations"), "@annotation(a) && @this(x)", AmbiguousBindingException.class,
            "Found 2 potential annotation variable(s), and 2 potential argument slots");
  }

  @Test
  public void testAmbiguousAnnotationOneVar() {
    assertException(getMethod("oneAnnotation"), "@annotation(a) && @this(x)", IllegalArgumentException.class,
            "Found 2 candidate annotation binding variables but only one potential argument binding slot");
  }

  @Test
  public void testAnnotationMedley() {
    assertParameterNames(getMethod("annotationMedley"), "@annotation(a) && args(count) && this(foo)",
            null, "ex", new String[] { "ex", "foo", "count", "a" });
  }

  public void oneAnnotation(MyAnnotation ann) { }

  public void twoAnnotations(MyAnnotation ann, MyAnnotation anotherAnn) { }

  public void annotationMedley(Throwable t, Object foo, int x, MyAnnotation ma) { }

  @interface MyAnnotation { }

}
