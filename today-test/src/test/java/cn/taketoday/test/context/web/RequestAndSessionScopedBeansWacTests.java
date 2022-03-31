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
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.web.servlet.WebServletApplicationContext;

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
  WebServletApplicationContext wac;

  @Autowired
  MockHttpServletRequest request;

  @Autowired
  MockHttpSession session;

  @Test
  void requestScope() throws Exception {
    String beanName = "requestScopedTestBean";
    String contextPath = "/path";

    assertThat(request.getAttribute(beanName)).isNull();

    request.setContextPath(contextPath);
    TestBean testBean = wac.getBean(beanName, TestBean.class);

    assertThat(testBean.getName()).isEqualTo(contextPath);
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
