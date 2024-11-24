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

package infra.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

/**
 * Additional parameter name discover tests that need Java 5.
 * Yes this will re-run the tests from the superclass, but that
 * doesn't matter in the grand scheme of things...
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AspectJAdviceParameterNameDiscoverAnnotationTests extends AspectJAdviceParameterNameDiscovererTests {

  @Test
  public void testAnnotationBinding() {
    assertParameterNames(getMethod("pjpAndAnAnnotation"),
            "execution(* *(..)) && @annotation(ann)",
            new String[] { "thisJoinPoint", "ann" });
  }

  public void pjpAndAnAnnotation(ProceedingJoinPoint pjp, MyAnnotation ann) { }

  @interface MyAnnotation { }

}
