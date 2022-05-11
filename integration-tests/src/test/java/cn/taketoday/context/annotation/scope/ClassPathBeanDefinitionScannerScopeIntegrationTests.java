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

package cn.taketoday.context.annotation.scope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.framework.web.session.EnableWebSession;
import cn.taketoday.lang.Component;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.context.annotation.RequestScope;
import cn.taketoday.web.context.annotation.SessionScope;
import cn.taketoday.web.context.support.GenericWebServletApplicationContext;
import cn.taketoday.web.servlet.ServletRequestContext;

import static cn.taketoday.context.annotation.ScopedProxyMode.DEFAULT;
import static cn.taketoday.context.annotation.ScopedProxyMode.INTERFACES;
import static cn.taketoday.context.annotation.ScopedProxyMode.NO;
import static cn.taketoday.context.annotation.ScopedProxyMode.TARGET_CLASS;
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

  private RequestContext oldRequestAttributes = new ServletRequestContext(null, new MockHttpServletRequest(), new MockHttpServletResponse());
  private RequestContext newRequestAttributes = new ServletRequestContext(null, new MockHttpServletRequest(), new MockHttpServletResponse());

  private RequestContext oldRequestAttributesWithSession;
  private RequestContext newRequestAttributesWithSession;

  @BeforeEach
  void setup() {
    MockHttpServletRequest oldRequestWithSession = new MockHttpServletRequest();
    oldRequestWithSession.setSession(new MockHttpSession());
    this.oldRequestAttributesWithSession = new ServletRequestContext(
            null, oldRequestWithSession, new MockHttpServletResponse());

    MockHttpServletRequest newRequestWithSession = new MockHttpServletRequest();
    newRequestWithSession.setSession(new MockHttpSession());
    this.newRequestAttributesWithSession = new ServletRequestContext(
            null, newRequestWithSession, new MockHttpServletResponse());

  }

  @AfterEach
  void reset() {
    RequestContextHolder.remove();
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
    GenericWebServletApplicationContext context = new GenericWebServletApplicationContext();
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

  @EnableWebSession
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
