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

package infra.aop.config;

import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.beans.testfixture.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AopNamespaceHandlerProxyTargetClassTests extends AopNamespaceHandlerTests {

  @Test
  public void testIsClassProxy() {
    ITestBean bean = getTestBean();
    assertThat(AopUtils.isCglibProxy(bean)).as("Should be a CGLIB proxy").isTrue();
    assertThat(((Advised) bean).isExposeProxy()).as("Should expose proxy").isTrue();
  }

}
