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

package infra.cache.annotation.scope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aop.support.AopUtils;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.ClassPathBeanDefinitionScanner;
import infra.context.annotation.ScopedProxyMode;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockHttpSession;
import infra.session.config.EnableSession;
import infra.stereotype.Component;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.context.annotation.RequestScope;
import infra.web.context.annotation.SessionScope;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;

import static infra.context.annotation.ScopedProxyMode.DEFAULT;
import static infra.context.annotation.ScopedProxyMode.INTERFACES;
import static infra.context.annotation.ScopedProxyMode.NO;
import static infra.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassPathBeanDefinitionScannerScopeIntegrationTests {

  private static final String DEFAULT_NAME = "default";
  private static final String MODIFIED_NAME = "modified";

  private RequestContext oldRequestAttributes = new MockRequestContext(null, new HttpMockRequestImpl(), new MockHttpResponseImpl());
  private RequestContext newRequestAttributes = new MockRequestContext(null, new HttpMockRequestImpl(), new MockHttpResponseImpl());

  private RequestContext oldRequestAttributesWithSession;
  private RequestContext newRequestAttributesWithSession;

  @BeforeEach
  void setup() {
    HttpMockRequestImpl oldRequestWithSession = new HttpMockRequestImpl();
    oldRequestWithSession.setSession(new MockHttpSession());
    this.oldRequestAttributesWithSession = new MockRequestContext(
            null, oldRequestWithSession, new MockHttpResponseImpl());

    HttpMockRequestImpl newRequestWithSession = new HttpMockRequestImpl();
    newRequestWithSession.setSession(new MockHttpSession());
    this.newRequestAttributesWithSession = new MockRequestContext(
            null, newRequestWithSession, new MockHttpResponseImpl());

  }

  @AfterEach
  void reset() {
    RequestContextHolder.cleanup();
  }

  @Test
  void singletonScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void singletonScopeIgnoresProxyInterfaces() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(INTERFACES);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void singletonScopeIgnoresProxyTargetClass() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(TARGET_CLASS);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void requestScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("request");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // but a newly retrieved bean should have the default name
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("request");
    assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
  }

  @Test
  void requestScopeWithProxiedInterfaces() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(INTERFACES);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

    // should be dynamic proxy, implementing both interfaces
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    boolean condition = bean instanceof AnotherScopeTestInterface;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    RequestContextHolder.set(oldRequestAttributes);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void requestScopeWithProxiedTargetClass() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(TARGET_CLASS);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

    // should be a class-based proxy
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();
    boolean condition = bean instanceof RequestScopedTestBean;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributes);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    RequestContextHolder.set(oldRequestAttributes);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void sessionScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("session");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributesWithSession);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // but a newly retrieved bean should have the default name
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
  }

  @Test
  void sessionScopeWithProxiedInterfaces() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(INTERFACES);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

    // should be dynamic proxy, implementing both interfaces
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    boolean condition = bean instanceof AnotherScopeTestInterface;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributesWithSession);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
    bean2.setName(DEFAULT_NAME);
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    RequestContextHolder.set(oldRequestAttributesWithSession);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void sessionScopeWithProxiedTargetClass() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(TARGET_CLASS);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

    // should be a class-based proxy
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();
    boolean condition1 = bean instanceof ScopedTestBean;
    assertThat(condition1).isTrue();
    boolean condition = bean instanceof SessionScopedTestBean;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    RequestContextHolder.set(newRequestAttributesWithSession);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
    bean2.setName(DEFAULT_NAME);
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    RequestContextHolder.set(oldRequestAttributesWithSession);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  private ApplicationContext createContext(ScopedProxyMode scopedProxyMode) {
    GenericWebApplicationContext context = new GenericWebApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.setBeanNameGenerator((definition, registry) -> definition.getScope());
    scanner.setScopedProxyMode(scopedProxyMode);

    // Scan twice in order to find errors in the bean definition compatibility check.
    scanner.scan(getClass().getPackage().getName());
    scanner.scan(getClass().getPackage().getName());
    AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
    reader.register(AppConfig1.class);

    context.refresh();
    return context;
  }

  @EnableSession
  public static class AppConfig1 {

  }

  interface IScopedTestBean {

    String getName();

    void setName(String name);
  }

  static abstract class ScopedTestBean implements IScopedTestBean {

    private String name = DEFAULT_NAME;

    @Override
    public String getName() { return this.name; }

    @Override
    public void setName(String name) { this.name = name; }
  }

  @Component
  static class SingletonScopedTestBean extends ScopedTestBean {
  }

  interface AnotherScopeTestInterface {
  }

  @Component
  @RequestScope(proxyMode = DEFAULT)
  static class RequestScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
  }

  @Component
  @SessionScope(proxyMode = DEFAULT)
  static class SessionScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
  }

}
