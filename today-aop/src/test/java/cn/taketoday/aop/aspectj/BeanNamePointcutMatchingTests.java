/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.aop.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for matching of bean() pointcut designator.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class BeanNamePointcutMatchingTests {

  @Test
  public void testMatchingPointcuts() {
    assertMatch("someName", "bean(someName)");

    // Infra bean names are less restrictive compared to AspectJ names (methods, types etc.)
    // MVC Controller-kind
    assertMatch("someName/someOtherName", "bean(someName/someOtherName)");
    assertMatch("someName/foo/someOtherName", "bean(someName/*/someOtherName)");
    assertMatch("someName/foo/bar/someOtherName", "bean(someName/*/someOtherName)");
    assertMatch("someName/*/**", "bean(someName/*)");
    // JMX-kind
    assertMatch("service:name=traceService", "bean(service:name=traceService)");
    assertMatch("service:name=traceService", "bean(service:name=*)");
    assertMatch("service:name=traceService", "bean(*:name=traceService)");

    // Wildcards
    assertMatch("someName", "bean(*someName)");
    assertMatch("someName", "bean(*Name)");
    assertMatch("someName", "bean(*)");
    assertMatch("someName", "bean(someName*)");
    assertMatch("someName", "bean(some*)");
    assertMatch("someName", "bean(some*Name)");
    assertMatch("someName", "bean(*some*Name*)");
    assertMatch("someName", "bean(*s*N*)");

    // Or, and, not expressions
    assertMatch("someName", "bean(someName) || bean(someOtherName)");
    assertMatch("someOtherName", "bean(someName) || bean(someOtherName)");

    assertMatch("someName", "!bean(someOtherName)");

    assertMatch("someName", "bean(someName) || !bean(someOtherName)");
    assertMatch("someName", "bean(someName) && !bean(someOtherName)");
  }

  @Test
  public void testNonMatchingPointcuts() {
    assertMisMatch("someName", "bean(someNamex)");
    assertMisMatch("someName", "bean(someX*Name)");

    // And, not expressions
    assertMisMatch("someName", "bean(someName) && bean(someOtherName)");
    assertMisMatch("someName", "!bean(someName)");
    assertMisMatch("someName", "!bean(someName) && bean(someOtherName)");
    assertMisMatch("someName", "!bean(someName) || bean(someOtherName)");
  }

  private void assertMatch(String beanName, String pcExpression) {
    assertThat(matches(beanName, pcExpression)).as("Unexpected mismatch for bean \"" + beanName + "\" for pcExpression \"" + pcExpression + "\"").isTrue();
  }

  private void assertMisMatch(String beanName, String pcExpression) {
    assertThat(matches(beanName, pcExpression)).as("Unexpected match for bean \"" + beanName + "\" for pcExpression \"" + pcExpression + "\"").isFalse();
  }

  private static boolean matches(final String beanName, String pcExpression) {
    @SuppressWarnings("serial")
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut() {
      @Override
      protected String getCurrentProxiedBeanName() {
        return beanName;
      }
    };
    pointcut.setExpression(pcExpression);
    return pointcut.matches(TestBean.class);
  }

}
