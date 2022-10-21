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

package cn.taketoday.web.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.beans.TestBean;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class RequestAndSessionScopedBeanTests {

  @Test
  @SuppressWarnings("resource")
  public void testPutBeanInRequest() {
    String targetBeanName = "target";

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setScope(RequestContext.SCOPE_REQUEST);
    bd.getPropertyValues().add("name", "abc");
    wac.registerBeanDefinition(targetBeanName, bd);
    wac.refresh();

    HttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.set(new ServletRequestContext(null, request, null));
    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target);

    TestBean target2 = (TestBean) wac.getBean(targetBeanName);
    assertThat(target2.getName()).isEqualTo("abc");
    assertThat(target).isSameAs(target2);
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target2);

    request = new MockHttpServletRequest();
    RequestContextHolder.set(new ServletRequestContext(null, request, null));
    TestBean target3 = (TestBean) wac.getBean(targetBeanName);
    assertThat(target3.getName()).isEqualTo("abc");
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target3);
    assertThat(target).isNotSameAs(target3);

    RequestContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

  @Test
  @SuppressWarnings("resource")
  public void testPutBeanInSession() {
    String targetBeanName = "target";
    HttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.set(new ServletRequestContext(null, request, null));

    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setScope(RequestContext.SCOPE_SESSION);
    bd.getPropertyValues().add("name", "abc");
    wac.registerBeanDefinition(targetBeanName, bd);
    wac.refresh();

    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(request.getSession().getAttribute(targetBeanName)).isSameAs(target);

    RequestContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

}
