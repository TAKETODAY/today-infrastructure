/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.support;

import org.junit.Test;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.proxy.AopProxyUtilsTests;
import cn.taketoday.aop.proxy.AopProxyUtilsTests.TestBean;
import cn.taketoday.context.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/2/3 23:44
 */
public class ClassFiltersTests {

  private final ClassFilter exceptionFilter = new RootClassFilter(Exception.class);

  private final ClassFilter interfaceFilter = new RootClassFilter(AopProxyUtilsTests.ITestBean.class);

  private final ClassFilter hasRootCauseFilter = new RootClassFilter(NestedRuntimeException.class);

  @Test
  public void union() {
    assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
    assertThat(exceptionFilter.matches(TestBean.class)).isFalse();
    assertThat(interfaceFilter.matches(Exception.class)).isFalse();
    assertThat(interfaceFilter.matches(AopProxyUtilsTests.TestBean.class)).isTrue();
    ClassFilter union = ClassFilters.union(exceptionFilter, interfaceFilter);
    assertThat(union.matches(RuntimeException.class)).isTrue();
    assertThat(union.matches(TestBean.class)).isTrue();
    assertThat(union.toString())
            .matches("^.+UnionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+TestBean\\]$");
  }

  @Test
  public void intersection() {
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

