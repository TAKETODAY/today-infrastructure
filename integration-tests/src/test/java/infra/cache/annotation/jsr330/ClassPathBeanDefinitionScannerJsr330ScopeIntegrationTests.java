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
import infra.context.support.GenericApplicationContext;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.MockSession;
import infra.session.config.EnableSession;
import infra.web.DispatcherHandler;
import infra.web.HttpContextHolder;
import infra.web.HttpContextUtils;
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

  private final HttpContext oldRequestAttributes = new MockHttpContext(null, new MockRequest(), new MockResponse());
  private final HttpContext newRequestAttributes = new MockHttpContext(null, new MockRequest(), new MockResponse());

  private HttpContext oldRequestAttributesWithSession;
  private HttpContext newRequestAttributesWithSession;

  private final GenericApplicationContext context = new GenericApplicationContext();

  @BeforeEach
  void setup() {
    HttpContextUtils.registerScopes(context.getBeanFactory());
    MockRequest oldRequestWithSession = new MockRequest();
    oldRequestWithSession.setSession(new MockSession());
    this.oldRequestAttributesWithSession = new MockHttpContext(
            context, oldRequestWithSession, new MockResponse(), new DispatcherHandler(context));

    MockRequest newRequestWithSession = new MockRequest();
    newRequestWithSession.setSession(new MockSession());
    this.newRequestAttributesWithSession = new MockHttpContext(
            context, newRequestWithSession, new MockResponse(), new DispatcherHandler(context));

  }

  @AfterEach
  void reset() {
    HttpContextHolder.set(null);
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
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");
    assertThat(context.isSingleton("singleton")).isTrue();
    assertThat(context.isPrototype("singleton")).isFalse();

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testSingletonScopeIgnoresProxyInterfaces() {
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testSingletonScopeIgnoresProxyTargetClass() {
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // singleton bean, so name should be modified even after lookup
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testRequestScopeWithNoProxy() {
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("request");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // but a newly retrieved bean should have the default name
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("request");
    assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
  }

  @Test
  void testRequestScopeWithProxiedInterfaces() {
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

    // should be dynamic proxy, implementing both interfaces
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    boolean condition = bean instanceof AnotherScopeTestInterface;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    HttpContextHolder.set(oldRequestAttributes);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testRequestScopeWithProxiedTargetClass() {
    HttpContextHolder.set(oldRequestAttributes);
    ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

    // should be a class-based proxy
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();
    boolean condition = bean instanceof RequestScopedTestBean;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributes);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    HttpContextHolder.set(oldRequestAttributes);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testSessionScopeWithNoProxy() {
    HttpContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(ScopedProxyMode.NO);
    ScopedTestBean bean = (ScopedTestBean) context.getBean("session");

    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributesWithSession);
    // not a proxy so this should not have changed
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

    // but a newly retrieved bean should have the default name
    ScopedTestBean bean2 = (ScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
  }

  @Test
  void testSessionScopeWithProxiedInterfaces() {
    HttpContextHolder.set(oldRequestAttributesWithSession);
    ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
    IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

    // should be dynamic proxy, implementing both interfaces
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    boolean condition = bean instanceof AnotherScopeTestInterface;
    assertThat(condition).isTrue();

    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    HttpContextHolder.set(newRequestAttributesWithSession);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
    bean2.setName(DEFAULT_NAME);
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    HttpContextHolder.set(oldRequestAttributesWithSession);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  @Test
  void testSessionScopeWithProxiedTargetClass() {
    HttpContextHolder.set(oldRequestAttributesWithSession);
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

    HttpContextHolder.set(newRequestAttributesWithSession);
    // this is a proxy so it should be reset to default
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
    bean.setName(MODIFIED_NAME);

    IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
    assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
    bean2.setName(DEFAULT_NAME);
    assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

    HttpContextHolder.set(oldRequestAttributesWithSession);
    assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
  }

  private ApplicationContext createContext(final ScopedProxyMode scopedProxyMode) {
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

  @EnableSession
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
