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

import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import jakarta.inject.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationMatchingPointcut}.
 *
 * @author Sam Brannen
 */
public class AnnotationMatchingPointcutTests {

  @Test
  public void classLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
    assertThat(pointcut2.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
    assertThat(pointcut3.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

  @Test
  public void methodLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(null, Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut2.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut3.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

  @Test
  public void classLevelAndMethodLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut1.getMethodMatcher().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

}
