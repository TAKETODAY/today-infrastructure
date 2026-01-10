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

package infra.web.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.testfixture.beans.TestBean;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import infra.web.mock.support.StaticWebApplicationContext;

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

    Session session = RequestContextUtils.getSession(context);

    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(session.getAttribute(targetBeanName)).isSameAs(target);

    RequestContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

  @EnableSession
  static class Config {

  }

}
