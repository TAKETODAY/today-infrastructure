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

package infra.web.context.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.support.ScopeNotActiveException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.CountingTestBean;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.expression.StandardBeanExpressionResolver;
import infra.core.io.ClassPathResource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.mock.MockRequestContext;

import static infra.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @see SessionScopeTests
 */
public class RequestScopeTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  public void setup() {
    this.beanFactory.registerScope("request", new RequestScope());
    this.beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver());
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.loadBeanDefinitions(new ClassPathResource("requestScopeTests.xml", getClass()));
    this.beanFactory.preInstantiateSingletons();
  }

  @AfterEach
  public void reset() {
    RequestContextHolder.set(null);
  }

  @Test
  public void getFromScope() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    String name = "requestScopedObject";
    assertThat(request.getAttribute(name)).isNull();
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    assertThat(bean.getName()).isEqualTo("");
    assertThat(request.getAttribute(name)).isSameAs(bean);
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
  }

  @Test
  public void destructionAtRequestCompletion() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, new MockHttpResponseImpl());
    RequestContextHolder.set(requestAttributes);

    String name = "requestScopedDisposableObject";
    assertThat(request.getAttribute(name)).isNull();
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(request.getAttribute(name)).isSameAs(bean);
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

    requestAttributes.requestCompleted();
    assertThat(bean.wasDestroyed()).isTrue();
  }

  @Test
  public void getFromFactoryBeanInScope() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    String name = "requestScopedFactoryBean";
    assertThat(request.getAttribute(name)).isNull();
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    boolean condition = request.getAttribute(name) instanceof FactoryBean;
    assertThat(condition).isTrue();
    assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
  }

  @Test
  public void circleLeadsToException() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    String name = "requestScopedObjectCircle1";
    assertThat(request.getAttribute(name)).isNull();
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    this.beanFactory.getBean(name))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
  }

  @Test
  public void innerBeanInheritsContainingBeanScopeByDefault() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, new MockHttpResponseImpl()); RequestContextHolder.set(requestAttributes);

    String outerBeanName = "requestScopedOuterBean";
    assertThat(request.getAttribute(outerBeanName)).isNull();
    TestBean outer1 = (TestBean) this.beanFactory.getBean(outerBeanName);
    assertThat(request.getAttribute(outerBeanName)).isNotNull();
    TestBean inner1 = (TestBean) outer1.getSpouse();
    assertThat(this.beanFactory.getBean(outerBeanName)).isSameAs(outer1);
    requestAttributes.requestCompleted();
    assertThat(outer1.wasDestroyed()).isTrue();
    assertThat(inner1.wasDestroyed()).isTrue();
    request = new HttpMockRequestImpl();
    requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);
    TestBean outer2 = (TestBean) this.beanFactory.getBean(outerBeanName);
    assertThat(outer2).isNotSameAs(outer1);
    assertThat(outer2.getSpouse()).isNotSameAs(inner1);
  }

  @Test
  public void requestScopedInnerBeanDestroyedWhileContainedBySingleton() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, new MockHttpResponseImpl()); RequestContextHolder.set(requestAttributes);

    String outerBeanName = "singletonOuterBean";
    TestBean outer1 = (TestBean) this.beanFactory.getBean(outerBeanName);
    assertThat(request.getAttribute(outerBeanName)).isNull();
    TestBean inner1 = (TestBean) outer1.getSpouse();
    TestBean outer2 = (TestBean) this.beanFactory.getBean(outerBeanName);
    assertThat(outer2).isSameAs(outer1);
    assertThat(outer2.getSpouse()).isSameAs(inner1);
    requestAttributes.requestCompleted();
    assertThat(inner1.wasDestroyed()).isTrue();
    assertThat(outer1.wasDestroyed()).isFalse();
  }

  @Test
  public void scopeNotAvailable() {
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(
            () -> this.beanFactory.getBean(CountingTestBean.class));

    ObjectProvider<CountingTestBean> beanProvider = this.beanFactory.getBeanProvider(CountingTestBean.class);
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(beanProvider::get);
    assertThat(beanProvider.getIfAvailable()).isNull();
    assertThat(beanProvider.getIfUnique()).isNull();

    ObjectProvider<CountingTestBean> provider =
            this.beanFactory.createBean(ProviderBean.class, AUTOWIRE_CONSTRUCTOR, false).provider;
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(provider::get);
    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void instanceFieldProvidesSingletonInstance() {
    RequestScope scope1 = RequestScope.instance;
    RequestScope scope2 = RequestScope.instance;

    assertThat(scope1).isNotNull();
    assertThat(scope1).isSameAs(scope2);
  }

  @Test
  void getReturnsBeanFromRequestScope() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    RequestScope requestScope = new RequestScope();
    String beanName = "requestScopedObject";
    Supplier<Object> objectFactory = mock(Supplier.class);
    TestBean testBean = new TestBean();
    given(objectFactory.get()).willReturn(testBean);

    Object result = requestScope.get(beanName, objectFactory);

    assertThat(result).isSameAs(testBean);
    assertThat(requestAttributes.getAttribute(beanName)).isSameAs(testBean);
    verify(objectFactory).get();
  }

  @Test
  void getReturnsExistingBeanFromRequestScope() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    RequestScope requestScope = new RequestScope();
    String beanName = "requestScopedObject";
    TestBean existingBean = new TestBean();
    requestAttributes.setAttribute(beanName, existingBean);

    Supplier<Object> objectFactory = mock(Supplier.class);
    Object result = requestScope.get(beanName, objectFactory);

    assertThat(result).isSameAs(existingBean);
    verify(objectFactory, never()).get();
  }

  @Test
  void removeReturnsAndRemovesBeanFromRequestScope() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    RequestScope requestScope = new RequestScope();
    String beanName = "requestScopedObject";
    TestBean testBean = new TestBean();
    requestAttributes.setAttribute(beanName, testBean);

    Object result = requestScope.remove(beanName);

    assertThat(result).isSameAs(testBean);
    assertThat(requestAttributes.getAttribute(beanName)).isNull();
  }

  @Test
  void resolveContextualObjectWithRequestKey() {
    MockRequestContext requestContext = new MockRequestContext(null, new HttpMockRequestImpl(), null);
    RequestContextHolder.set(requestContext);

    RequestScope requestScope = new RequestScope();
    Object result = requestScope.resolveContextualObject(RequestContext.SCOPE_REQUEST);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveContextualObjectWithInvalidKey() {
    RequestContextHolder.set(new MockRequestContext(null, new HttpMockRequestImpl(), null));

    RequestScope requestScope = new RequestScope();
    Object result = requestScope.resolveContextualObject("invalidKey");

    assertThat(result).isNull();
  }

  @Test
  void resolveContextualObjectWithSessionKeyAndNoRequestContext() {
    RequestContextHolder.set(null);

    RequestScope requestScope = new RequestScope();
    Object result = requestScope.resolveContextualObject(RequestContext.SCOPE_SESSION);

    assertThat(result).isNull();
  }

  @Test
  void registerDestructionCallbackRegistersCallback() {
    MockRequestContext requestContext = new MockRequestContext(null, new HttpMockRequestImpl(), new MockHttpResponseImpl());
    RequestContextHolder.set(requestContext);

    RequestScope requestScope = new RequestScope();
    Runnable callback = mock(Runnable.class);

    requestScope.registerDestructionCallback("testBean", callback);

    // Verification through request completion
    requestContext.requestCompleted();
    // If no exception is thrown, the callback was registered properly
  }

  public static class ProviderBean {

    public ObjectProvider<CountingTestBean> provider;

    public ProviderBean(ObjectProvider<CountingTestBean> provider) {
      this.provider = provider;
    }
  }

}
