/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
