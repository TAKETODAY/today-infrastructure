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

package infra.cache.annotation.jsr330;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.ClassPathBeanDefinitionScanner;
import infra.context.annotation.ScopeMetadata;
import infra.context.annotation.ScopedProxyMode;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockHttpSession;
import infra.session.config.EnableWebSession;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ClassPathBeanDefinitionScannerJsr330ScopeIntegrationTests {

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
    RequestContextHolder.set(null);
  }

  @Test
  void testPrototype() {
    ApplicationContext context = createContext(ScopedProxyMode.NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("prototype");
    assertThat(bean).isNotNull();
    assertThat(context.isPrototype("prototype")).isTrue();
    assertThat(context.isSingleton("prototype")).isFalse();
  }

  @Test
  void testSingletonScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");
    assertThat(context.isSingleton("singleton")).isTrue();
    assertThat(context.isPrototype("singleton")).isFalse();

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
  void testSingletonScopeIgnoresProxyInterfaces() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
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
  void testSingletonScopeIgnoresProxyTargetClass() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
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
  void testRequestScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
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
  void testRequestScopeWithProxiedInterfaces() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
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
  void testRequestScopeWithProxiedTargetClass() {
    RequestContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
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
  void testSessionScopeWithNoProxy() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
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
  void testSessionScopeWithProxiedInterfaces() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
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
  void testSessionScopeWithProxiedTargetClass() {
    RequestContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
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

  private ApplicationContext createContext(final ScopedProxyMode scopedProxyMode) {
    GenericWebApplicationContext context = new GenericWebApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.setScopeMetadataResolver(definition -> {
      ScopeMetadata metadata = new ScopeMetadata();
      if (definition instanceof AnnotatedBeanDefinition annDef) {
        for (String type : annDef.getMetadata().getAnnotationTypes()) {
          if (type.equals(Singleton.class.getName())) {
            metadata.setScopeName(BeanDefinition.SCOPE_SINGLETON);
            break;
          }
          else if (annDef.getMetadata().getMetaAnnotationTypes(type).contains(jakarta.inject.Scope.class.getName())) {
            metadata.setScopeName(type.substring(type.length() - 13, type.length() - 6).toLowerCase());
            metadata.setScopedProxyMode(scopedProxyMode);
            break;
          }
          else if (type.startsWith("jakarta.inject")) {
            metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
          }
        }
      }
      return metadata;
    });

    // Scan twice in order to find errors in the bean definition compatibility check.
    scanner.scan(getClass().getPackage().getName());
    scanner.scan(getClass().getPackage().getName());

    AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
    reader.register(AppConfig.class);

    context.registerAlias("classPathBeanDefinitionScannerJsr330ScopeIntegrationTests.SessionScopedTestBean", "session");
    context.refresh();
    return context;
  }

  public interface IScopedTestBean {

    String getName();

    void setName(String name);
  }

  @EnableWebSession
  public static class AppConfig {

  }

  public static abstract class ScopedTestBean implements IScopedTestBean {

    private String name = DEFAULT_NAME;

    @Override
    public String getName() { return this.name; }

    @Override
    public void setName(String name) { this.name = name; }
  }

  @Named("prototype")
  public static class PrototypeScopedTestBean extends ScopedTestBean {
  }

  @Named("singleton")
  @Singleton
  public static class SingletonScopedTestBean extends ScopedTestBean {
  }

  public interface AnotherScopeTestInterface {
  }

  @Named("request")
  @RequestScoped
  public static class RequestScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
  }

  @Named
  @SessionScoped
  public static class SessionScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
  }

  @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @jakarta.inject.Scope
  public @interface RequestScoped {
  }

  @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @jakarta.inject.Scope
  public @interface SessionScoped {
  }

}
