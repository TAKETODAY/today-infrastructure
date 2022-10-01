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

package cn.taketoday.test.context.web;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.web.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit-based integration tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig
class ServletTestExecutionListenerJUnitIntegrationTests {

  @Configuration
  static class Config {
    /* no beans required for this test */
  }

  @Autowired
  private MockHttpServletRequest servletRequest;

  /**
   * Verifies bug fix for <a href="https://jira.spring.io/browse/SPR-11626">SPR-11626</a>.
   *
   * @see #ensureMocksAreReinjectedBetweenTests_2
   */
  @Test
  void ensureMocksAreReinjectedBetweenTests_1() {
    assertInjectedServletRequestEqualsRequestInRequestContextHolder();
  }

  /**
   * Verifies bug fix for <a href="https://jira.spring.io/browse/SPR-11626">SPR-11626</a>.
   *
   * @see #ensureMocksAreReinjectedBetweenTests_1
   */
  @Test
  void ensureMocksAreReinjectedBetweenTests_2() {
    assertInjectedServletRequestEqualsRequestInRequestContextHolder();
  }

  private void assertInjectedServletRequestEqualsRequestInRequestContextHolder() {
    assertThat(RequestContextHolder.get()).as("Injected ServletRequest must be stored in the RequestContextHolder")
            .isEqualTo(servletRequest);
  }

}
