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
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.web.DispatcherHandler;
import infra.web.HttpContextHolder;
import infra.web.HttpContextUtils;

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

    AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setScope(HttpContext.SCOPE_REQUEST);
    bd.getPropertyValues().add("name", "abc");
    wac.registerBeanDefinition(targetBeanName, bd);
    HttpContextUtils.registerScopes(wac.getBeanFactory());
    wac.refresh();

    MockRequest request = new MockRequest();
    HttpContextHolder.set(new MockHttpContext(null, request, null));
    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target);

    TestBean target2 = (TestBean) wac.getBean(targetBeanName);
    assertThat(target2.getName()).isEqualTo("abc");
    assertThat(target).isSameAs(target2);
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target2);

    request = new MockRequest();
    HttpContextHolder.set(new MockHttpContext(null, request, null));
    TestBean target3 = (TestBean) wac.getBean(targetBeanName);
    assertThat(target3.getName()).isEqualTo("abc");
    assertThat(request.getAttribute(targetBeanName)).isSameAs(target3);
    assertThat(target).isNotSameAs(target3);

    HttpContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

  @Test
  public void testPutBeanInSession() {
    String targetBeanName = "target";
    MockRequest request = new MockRequest();

    AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();

    MockHttpContext context = new MockHttpContext(wac, request, new MockResponse(), new DispatcherHandler(wac));
    HttpContextHolder.set(context);

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setScope(HttpContext.SCOPE_SESSION);
    bd.getPropertyValues().add("name", "abc");

    wac.register(Config.class);
    wac.refresh();
    wac.registerBeanDefinition(targetBeanName, bd);

    HttpContextUtils.registerScopes(wac.getBeanFactory());

    Session session = context.getSession();

    TestBean target = (TestBean) wac.getBean(targetBeanName);
    assertThat(target.getName()).isEqualTo("abc");
    assertThat(session.getAttribute(targetBeanName)).isSameAs(target);

    HttpContextHolder.set(null);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            wac.getBean(targetBeanName));
  }

  @EnableSession
  static class Config {

  }

}
