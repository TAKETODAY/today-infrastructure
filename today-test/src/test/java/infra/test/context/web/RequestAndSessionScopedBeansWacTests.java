/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.context.web;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.TestBean;
import infra.mock.web.HttpMockRequestImpl;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for request and session scoped beans
 * in conjunction with the TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig
class RequestAndSessionScopedBeansWacTests {

  @Autowired
  WebApplicationContext wac;

  @Autowired
  HttpMockRequestImpl request;

  @Autowired
  Session session;

  @EnableSession
  static class Config {

  }

  @Test
  void requestScope() throws Exception {
    String beanName = "requestScopedTestBean";

    assertThat(request.getAttribute(beanName)).isNull();

    TestBean testBean = wac.getBean(beanName, TestBean.class);

    assertThat(testBean.getName()).isEqualTo("");
    assertThat(request.getAttribute(beanName)).isSameAs(testBean);
    assertThat(wac.getBean(beanName, TestBean.class)).isSameAs(testBean);
  }

  @Test
  void sessionScope() throws Exception {
    String beanName = "sessionScopedTestBean";

    assertThat(session.getAttribute(beanName)).isNull();

    TestBean testBean = wac.getBean(beanName, TestBean.class);

    assertThat(session.getAttribute(beanName)).isSameAs(testBean);
    assertThat(wac.getBean(beanName, TestBean.class)).isSameAs(testBean);
  }

}
