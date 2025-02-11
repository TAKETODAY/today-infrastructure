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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aop.support.AopUtils;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.ScopeNotActiveException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.CountingTestBean;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.DummyFactory;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
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
 * @author Juergen Hoeller
 */
public class RequestScopedProxyTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  public void setup() {
    this.beanFactory.registerScope("request", new RequestScope());
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.loadBeanDefinitions(new ClassPathResource("requestScopedProxyTests.xml", getClass()));
    this.beanFactory.preInstantiateSingletons();
  }

  @Test
  public void testGetFromScope() {
    String name = "requestScopedObject";
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getName()).isEqualTo("scoped");
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      TestBean target = (TestBean) request.getAttribute("scopedTarget." + name);
      assertThat(target.getClass()).isEqualTo(TestBean.class);
      assertThat(target.getName()).isEqualTo("scoped");
      assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
      assertThat(target.toString()).isEqualTo(bean.toString());
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void testGetFromScopeThroughDynamicProxy() {
    String name = "requestScopedProxy";
    ITestBean bean = (ITestBean) this.beanFactory.getBean(name);
    // assertTrue(AopUtils.isJdkDynamicProxy(bean));

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getName()).isEqualTo("scoped");
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      TestBean target = (TestBean) request.getAttribute("scopedTarget." + name);
      assertThat(target.getClass()).isEqualTo(TestBean.class);
      assertThat(target.getName()).isEqualTo("scoped");
      assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
      assertThat(target.toString()).isEqualTo(bean.toString());
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void testDestructionAtRequestCompletion() {
    String name = "requestScopedDisposableObject";
    DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, new MockHttpResponseImpl());
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getName()).isEqualTo("scoped");
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(DerivedTestBean.class);
      assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
      assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

      requestAttributes.requestCompleted();
      assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).wasDestroyed()).isTrue();
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void testGetFromFactoryBeanInScope() {
    String name = "requestScopedFactoryBean";
    TestBean bean = (TestBean) this.beanFactory.getBean(name);
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(DummyFactory.class);
      assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void testGetInnerBeanFromScope() {
    TestBean bean = (TestBean) this.beanFactory.getBean("outerBean");
    assertThat(AopUtils.isAopProxy(bean)).isFalse();
    assertThat(AopUtils.isCglibProxy(bean.getSpouse())).isTrue();

    String name = "scopedInnerBean";

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getSpouse().getName()).isEqualTo("scoped");
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(TestBean.class);
      assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void testGetAnonymousInnerBeanFromScope() {
    TestBean bean = (TestBean) this.beanFactory.getBean("outerBean");
    assertThat(AopUtils.isAopProxy(bean)).isFalse();
    assertThat(AopUtils.isCglibProxy(bean.getSpouse())).isTrue();

    BeanDefinition beanDef = this.beanFactory.getBeanDefinition("outerBean");
    BeanDefinitionHolder innerBeanDef =
            (BeanDefinitionHolder) beanDef.getPropertyValues().getPropertyValue("spouse");
    String name = innerBeanDef.getBeanName();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext requestAttributes = new MockRequestContext(null, request, null);
    RequestContextHolder.set(requestAttributes);

    try {
      assertThat(request.getAttribute("scopedTarget." + name)).isNull();
      assertThat(bean.getSpouse().getName()).isEqualTo("scoped");
      assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
      assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(TestBean.class);
      assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
    }
    finally {
      RequestContextHolder.set(null);
    }
  }

  @Test
  public void scopeNotAvailable() {
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(
            () -> this.beanFactory.getBean(CountingTestBean.class).absquatulate());

    ObjectProvider<CountingTestBean> beanProvider = this.beanFactory.getBeanProvider(CountingTestBean.class);
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(() -> beanProvider.get().absquatulate());
    beanProvider.ifAvailable(TestBean::absquatulate);
    beanProvider.ifUnique(TestBean::absquatulate);

    ObjectProvider<CountingTestBean> provider =
            this.beanFactory.createBean(ProviderBean.class, AUTOWIRE_CONSTRUCTOR, false).provider;
    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(() -> provider.get().absquatulate());
    provider.ifAvailable(TestBean::absquatulate);
    provider.ifUnique(TestBean::absquatulate);
  }

  public static class ProviderBean {

    public ObjectProvider<CountingTestBean> provider;

    public ProviderBean(ObjectProvider<CountingTestBean> provider) {
      this.provider = provider;
    }
  }

}
