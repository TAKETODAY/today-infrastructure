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

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;

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

}
