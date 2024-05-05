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

package cn.taketoday.web.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.session.WebSession;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.support.AnnotationConfigWebApplicationContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.mock.api.http.HttpMockRequest;

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

    HttpMockRequest request = new HttpMockRequestImpl();
    RequestContextHolder.set(new MockRequestContext(null, request, null));
    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target);

    TestBean target2 = (TestBean) wac.getBean(targetBeanName);
    assertThat(target2.getName()).isEqualTo("abc");
    assertThat(target).isSameAs(target2);
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target2);

    request = new HttpMockRequestImpl();
    RequestContextHolder.set(new MockRequestContext(null, request, null));
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
    HttpMockRequest request = new HttpMockRequestImpl();

    AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();

    MockRequestContext context = new MockRequestContext(wac, request, new MockHttpResponseImpl());
    RequestContextHolder.set(context);

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setScope(RequestContext.SCOPE_SESSION);
    bd.getPropertyValues().add("name", "abc");

    wac.register(Config.class);
    wac.refresh();
    wac.registerBeanDefinition(targetBeanName, bd);

    WebSession session = RequestContextUtils.getSession(context);

    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(session.getAttribute(targetBeanName)).isSameAs(target);

    RequestContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

  @EnableWebSession
  static class Config {

  }

}
