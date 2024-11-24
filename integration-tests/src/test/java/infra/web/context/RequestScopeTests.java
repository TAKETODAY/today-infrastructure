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

package infra.web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import infra.web.RequestContextHolder;
import infra.web.context.support.RequestScope;
import infra.web.mock.MockRequestContext;

import static infra.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

  public static class ProviderBean {

    public ObjectProvider<CountingTestBean> provider;

    public ProviderBean(ObjectProvider<CountingTestBean> provider) {
      this.provider = provider;
    }
  }

}
