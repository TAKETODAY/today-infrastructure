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

package infra.aop.support;

import org.junit.jupiter.api.Test;

import infra.aop.ClassFilter;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ClassFilter}.
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
    ClassFilter union = ClassFilter.union(exceptionFilter, interfaceFilter);
    assertThat(union.matches(RuntimeException.class)).isTrue();
    assertThat(union.matches(TestBean.class)).isTrue();
    assertThat(union.toString())
            .matches("^.+UnionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+TestBean\\]$");
  }

  @Test
  void intersection() {
    assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
    assertThat(hasRootCauseFilter.matches(NestedRuntimeException.class)).isTrue();
    ClassFilter intersection = ClassFilter.intersection(exceptionFilter, hasRootCauseFilter);
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
    ClassFilter negate = ClassFilter.negate(filter);
    assertThat(negate.matches(String.class)).isFalse();
    verify(filter).matches(String.class);
  }

  @Test
  void negateTrueClassFilter() {
    ClassFilter negate = ClassFilter.negate(ClassFilter.TRUE);
    assertThat(negate.matches(String.class)).isFalse();
    assertThat(negate.matches(Object.class)).isFalse();
    assertThat(negate.matches(Integer.class)).isFalse();
  }

  @Test
  void negateTrueClassFilterAppliedTwice() {
    ClassFilter negate = ClassFilter.negate(ClassFilter.negate(ClassFilter.TRUE));
    assertThat(negate.matches(String.class)).isTrue();
    assertThat(negate.matches(Object.class)).isTrue();
    assertThat(negate.matches(Integer.class)).isTrue();
  }

  @Test
  void negateIsNotEqualsToOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter negate = ClassFilter.negate(original);
    assertThat(original).isNotEqualTo(negate);
  }

  @Test
  void negateOnSameFilterIsEquals() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter first = ClassFilter.negate(original);
    ClassFilter second = ClassFilter.negate(original);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void negateHasNotSameHashCodeAsOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter negate = ClassFilter.negate(original);
    assertThat(original).doesNotHaveSameHashCodeAs(negate);
  }

  @Test
  void negateOnSameFilterHasSameHashCode() {
    ClassFilter original = ClassFilter.TRUE;
    ClassFilter first = ClassFilter.negate(original);
    ClassFilter second = ClassFilter.negate(original);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void toStringIncludesRepresentationOfOriginalFilter() {
    ClassFilter original = ClassFilter.TRUE;
    assertThat(ClassFilter.negate(original)).hasToString("Negate " + original);
  }

}
