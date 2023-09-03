/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ClassFilters}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassFiltersTests {

  private final ClassFilter exceptionFilter = new RootClassFilter(Exception.class);

  private final ClassFilter interfaceFilter = new RootClassFilter(ITestBean.class);

  private final ClassFilter hasRootCauseFilter = new RootClassFilter(NestedRuntimeException.class);

  @Test
  void union() {
    assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
    assertThat(exceptionFilter.matches(TestBean.class)).isFalse();
    assertThat(interfaceFilter.matches(Exception.class)).isFalse();
    assertThat(interfaceFilter.matches(TestBean.class)).isTrue();
    ClassFilter union = ClassFilters.union(exceptionFilter, interfaceFilter);
    assertThat(union.matches(RuntimeException.class)).isTrue();
    assertThat(union.matches(TestBean.class)).isTrue();
    assertThat(union.toString())
            .matches("^.+UnionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+TestBean\\]$");
  }

  @Test
  void intersection() {
    assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
    assertThat(hasRootCauseFilter.matches(NestedRuntimeException.class)).isTrue();
    ClassFilter intersection = ClassFilters.intersection(exceptionFilter, hasRootCauseFilter);
    assertThat(intersection.matches(RuntimeException.class)).isFalse();
    assertThat(intersection.matches(TestBean.class)).isFalse();
    assertThat(intersection.matches(NestedRuntimeException.class)).isTrue();
    assertThat(intersection.toString())
            .matches("^.+IntersectionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+NestedRuntimeException\\]$");
  }

  @Test
  void negateClassFilter() {
    ClassFilter filter = mock(ClassFilter.class);
    given(filter.matches(String.class)).willReturn(true);
    ClassFilter negate = ClassFilters.negate(filter);
    assertThat(negate.matches(String.class)).isFalse();
    verify(filter).matches(String.class);
  }

  @Test
  void negateTrueClassFilter() {
    ClassFilter negate = ClassFilters.negate(ClassFilter.TRUE);
    assertThat(negate.matches(String.class)).isFalse();
    assertThat(negate.matches(Object.class)).isFalse();
    assertThat(negate.matches(Integer.class)).isFalse();
  }

  @Test
  void negateTrueClassFilterAppliedTwice() {
    ClassFilter negate = ClassFilters.negate(ClassFilters.negate(ClassFilter.TRUE));
    assertThat(negate.matches(String.class)).isTrue();
    assertThat(negate.matches(Object.class)).isTrue();
    assertThat(negate.matches(Integer.class)).isTrue();
  }

  @Test
  void negateIsNotEqualsToOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter negate = ClassFilters.negate(original);
    assertThat(original).isNotEqualTo(negate);
  }

  @Test
  void negateOnSameFilterIsEquals() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter first = ClassFilters.negate(original);
    ClassFilter second = ClassFilters.negate(original);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void negateHasNotSameHashCodeAsOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter negate = ClassFilters.negate(original);
    assertThat(original).doesNotHaveSameHashCodeAs(negate);
  }

  @Test
  void negateOnSameFilterHasSameHashCode() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter first = ClassFilters.negate(original);
    ClassFilter second = ClassFilters.negate(original);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void toStringIncludesRepresentationOfOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    assertThat(ClassFilters.negate(original)).hasToString("Negate " + original);
  }

}
