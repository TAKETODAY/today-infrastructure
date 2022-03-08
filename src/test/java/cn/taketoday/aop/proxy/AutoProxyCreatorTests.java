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

package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.IndexedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.context.support.StaticMessageSource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/5 12:52
 */
class AutoProxyCreatorTests {

  @Test
  public void testBeanNameAutoProxyCreator() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testInterceptor", TestInterceptor.class);

    RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
    proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
    proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,innerBean,singletonFactoryToBeProxied");
    sac.getBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    RootBeanDefinition innerBean = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition innerBean1 = new RootBeanDefinition(innerBean);
    innerBean1.setBeanName("innerBean");
    bd.getPropertyValues().add("spouse", innerBean1);
    sac.getBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

    sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
    sac.registerSingleton("autowiredIndexedTestBean", IndexedTestBean.class);

    sac.refresh();

    MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
    ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
    assertThat(Proxy.isProxyClass(messageSource.getClass())).isFalse();
    assertThat(Proxy.isProxyClass(singletonToBeProxied.getClass())).isTrue();
    assertThat(Proxy.isProxyClass(singletonToBeProxied.getSpouse().getClass())).isTrue();

    // test whether autowiring succeeded with auto proxy creation
    assertThat(singletonToBeProxied.getNestedIndexedBean()).isEqualTo(sac.getBean("autowiredIndexedTestBean"));

    TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
    // already 2: getSpouse + getNestedIndexedBean calls above
    assertThat(ti.nrOfInvocations).isEqualTo(2);
    singletonToBeProxied.getName();
    singletonToBeProxied.getSpouse().getName();
    assertThat(ti.nrOfInvocations).isEqualTo(5);

    ITestBean tb = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
    assertThat(ti.nrOfInvocations).isEqualTo(5);
    tb.getAge();
    assertThat(ti.nrOfInvocations).isEqualTo(6);

    ITestBean tb2 = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(tb2).isSameAs(tb);
    assertThat(ti.nrOfInvocations).isEqualTo(6);
    tb2.getAge();
    assertThat(ti.nrOfInvocations).isEqualTo(7);
  }

  @Test
  public void testBeanNameAutoProxyCreatorWithFactoryBeanProxy() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testInterceptor", TestInterceptor.class);

    RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
    proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
    proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,&singletonFactoryToBeProxied");
    sac.getBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    sac.getBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

    sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

    sac.refresh();

    ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
    assertThat(Proxy.isProxyClass(singletonToBeProxied.getClass())).isTrue();

    TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
    int initialNr = ti.nrOfInvocations;
    singletonToBeProxied.getName();
    assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 1));

    FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
    assertThat(Proxy.isProxyClass(factory.getClass())).isTrue();
    TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(AopUtils.isAopProxy(tb)).isFalse();
    assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 3));
    tb.getAge();
    assertThat(ti.nrOfInvocations).isEqualTo((initialNr + 3));
  }

  @Test
  public void testCustomAutoProxyCreator() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);
    sac.registerSingleton("noInterfaces", NoInterfaces.class);
    sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
    sac.registerSingleton("singletonNoInterceptor", TestBean.class);
    sac.registerSingleton("singletonToBeProxied", TestBean.class);
    sac.registerPrototype("prototypeToBeProxied", TestBean.class);
    sac.refresh();

    MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
    NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
    ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
            (ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
    ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
    ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
    ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
    assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
    assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
    assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
    assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isTrue();
    assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isTrue();
    assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isTrue();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonNoInterceptor.getName();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonToBeProxied.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
    prototypeToBeProxied.getSpouse();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
  }

  @Test
  public void testAutoProxyCreatorWithFallbackToTargetClass() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testAutoProxyCreator", FallbackTestAutoProxyCreator.class);
    sac.registerSingleton("noInterfaces", NoInterfaces.class);
    sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
    sac.registerSingleton("singletonNoInterceptor", TestBean.class);
    sac.registerSingleton("singletonToBeProxied", TestBean.class);
    sac.registerPrototype("prototypeToBeProxied", TestBean.class);
    sac.refresh();

    MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
    NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
    ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
            (ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
    ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
    ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
    ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
    assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
    assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
    assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
    assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isFalse();
    assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isFalse();
    assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isFalse();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonNoInterceptor.getName();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonToBeProxied.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
    prototypeToBeProxied.getSpouse();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
  }

  @Test
  public void testAutoProxyCreatorWithFallbackToDynamicProxy() {
    StaticApplicationContext sac = new StaticApplicationContext();

    PropertyValues pvs = new PropertyValues();
    pvs.add("proxyFactoryBean", "false");
    sac.registerSingleton("testAutoProxyCreator", IntroductionTestAutoProxyCreator.class, pvs);

    sac.registerSingleton("noInterfaces", NoInterfaces.class);
    sac.registerSingleton("containerCallbackInterfacesOnly", ContainerCallbackInterfacesOnly.class);
    sac.registerSingleton("singletonNoInterceptor", CustomProxyFactoryBean.class);
    sac.registerSingleton("singletonToBeProxied", CustomProxyFactoryBean.class);
    sac.registerPrototype("prototypeToBeProxied", SpringProxyFactoryBean.class);

    sac.refresh();

    MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
    NoInterfaces noInterfaces = (NoInterfaces) sac.getBean("noInterfaces");
    ContainerCallbackInterfacesOnly containerCallbackInterfacesOnly =
            (ContainerCallbackInterfacesOnly) sac.getBean("containerCallbackInterfacesOnly");
    ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
    ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
    ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
    assertThat(AopUtils.isCglibProxy(messageSource)).isFalse();
    assertThat(AopUtils.isCglibProxy(noInterfaces)).isTrue();
    assertThat(AopUtils.isCglibProxy(containerCallbackInterfacesOnly)).isTrue();
    assertThat(AopUtils.isCglibProxy(singletonNoInterceptor)).isFalse();
    assertThat(AopUtils.isCglibProxy(singletonToBeProxied)).isFalse();
    assertThat(AopUtils.isCglibProxy(prototypeToBeProxied)).isFalse();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonNoInterceptor.getName();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    singletonToBeProxied.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
    prototypeToBeProxied.getSpouse();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(4);
  }

  @Test
  public void testAutoProxyCreatorWithPackageVisibleMethod() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);
    sac.registerSingleton("packageVisibleMethodToBeProxied", PackageVisibleMethod.class);
    sac.refresh();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    tapc.testInterceptor.nrOfInvocations = 0;

    PackageVisibleMethod tb = (PackageVisibleMethod) sac.getBean("packageVisibleMethodToBeProxied");
    assertThat(AopUtils.isCglibProxy(tb)).isTrue();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    tb.doSomething();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
  }

  @Test
  public void testAutoProxyCreatorWithFactoryBean() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);
    sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
    sac.refresh();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    tapc.testInterceptor.nrOfInvocations = 0;

    FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(factory)).isTrue();

    TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(tb)).isTrue();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
    tb.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(3);
  }

  @Test
  public void testAutoProxyCreatorWithFactoryBeanAndPrototype() {
    StaticApplicationContext sac = new StaticApplicationContext();
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);

    PropertyValues pvs = new PropertyValues();
    pvs.add("singleton", "false");
    sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

    sac.refresh();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    tapc.testInterceptor.nrOfInvocations = 0;

    FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(prototypeFactory)).isTrue();
    TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(tb)).isTrue();

    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
    tb.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(3);
  }

  @Test
  public void testAutoProxyCreatorWithFactoryBeanAndProxyObjectOnly() {
    StaticApplicationContext sac = new StaticApplicationContext();

    PropertyValues pvs = new PropertyValues();
    pvs.add("proxyFactoryBean", "false");
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class, pvs);

    sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

    sac.refresh();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    tapc.testInterceptor.nrOfInvocations = 0;

    FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
    assertThat(AopUtils.isAopProxy(factory)).isFalse();

    TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(tb)).isTrue();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(0);
    tb.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);

    TestBean tb2 = (TestBean) sac.getBean("singletonFactoryToBeProxied");
    assertThat(tb2).isSameAs(tb);
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(1);
    tb2.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
  }

  @Test
  public void testAutoProxyCreatorWithFactoryBeanAndProxyFactoryBeanOnly() {
    StaticApplicationContext sac = new StaticApplicationContext();

    PropertyValues pvs = new PropertyValues();
    pvs.add("proxyObject", "false");
    sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class, pvs);

    pvs = new PropertyValues();
    pvs.add("singleton", "false");
    sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

    sac.refresh();

    TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
    tapc.testInterceptor.nrOfInvocations = 0;

    FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(prototypeFactory)).isTrue();
    TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
    assertThat(AopUtils.isCglibProxy(tb)).isFalse();

    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
    tb.getAge();
    assertThat(tapc.testInterceptor.nrOfInvocations).isEqualTo(2);
  }

  @SuppressWarnings("serial")
  public static class TestAutoProxyCreator extends AbstractAutoProxyCreator {

    private boolean proxyFactoryBean = true;

    private boolean proxyObject = true;

    public TestInterceptor testInterceptor = new TestInterceptor();

    public TestAutoProxyCreator() {
      setProxyTargetClass(true);
      setOrder(0);
    }

    public void setProxyFactoryBean(boolean proxyFactoryBean) {
      this.proxyFactoryBean = proxyFactoryBean;
    }

    public void setProxyObject(boolean proxyObject) {
      this.proxyObject = proxyObject;
    }

    @Override
    @Nullable
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String name, @Nullable TargetSource customTargetSource) {
      if (StaticMessageSource.class.equals(beanClass)) {
        return DO_NOT_PROXY;
      }
      else if (name.endsWith("ToBeProxied")) {
        boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
        if ((this.proxyFactoryBean && isFactoryBean) || (this.proxyObject && !isFactoryBean)) {
          return getAdvicesAndAdvisors();
        }
        else {
          return DO_NOT_PROXY;
        }
      }
      else {
        return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
      }
    }

    protected Object[] getAdvicesAndAdvisors() {
      return new Object[] { this.testInterceptor };
    }
  }

  @SuppressWarnings("serial")
  public static class FallbackTestAutoProxyCreator extends TestAutoProxyCreator {

    public FallbackTestAutoProxyCreator() {
      setProxyTargetClass(false);
    }
  }

  @SuppressWarnings("serial")
  public static class IntroductionTestAutoProxyCreator extends TestAutoProxyCreator {

    @Override
    protected Object[] getAdvicesAndAdvisors() {
      DefaultIntroductionAdvisor advisor = new DefaultIntroductionAdvisor(this.testInterceptor);
      advisor.addInterface(Serializable.class);
      return new Object[] { this.testInterceptor, advisor };
    }
  }

  /**
   * Interceptor that counts the number of non-finalize method calls.
   */
  public static class TestInterceptor implements MethodInterceptor {

    public int nrOfInvocations = 0;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      if (!invocation.getMethod().getName().equals("finalize")) {
        this.nrOfInvocations++;
      }
      return invocation.proceed();
    }
  }

  public static class NoInterfaces {
  }

  @SuppressWarnings("serial")
  public static class ContainerCallbackInterfacesOnly  // as well as an empty marker interface
          implements BeanFactoryAware, ApplicationContextAware, InitializingBean, DisposableBean, Serializable {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
    }

    @Override
    public void afterPropertiesSet() {
    }

    @Override
    public void destroy() {
    }
  }

  public static class CustomProxyFactoryBean implements FactoryBean<ITestBean> {

    private final TestBean tb = new TestBean();

    @Override
    public ITestBean getObject() {
      return (ITestBean) Proxy.newProxyInstance(CustomProxyFactoryBean.class.getClassLoader(), new Class<?>[] { ITestBean.class },
              (proxy, method, args) -> ReflectionUtils.invokeMethod(method, tb, args));
    }

    @Override
    public Class<?> getObjectType() {
      return ITestBean.class;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
  }

  public static class SpringProxyFactoryBean implements FactoryBean<ITestBean> {

    private final TestBean tb = new TestBean();

    @Override
    public ITestBean getObject() {
      return ProxyFactory.getProxy(ITestBean.class, new SingletonTargetSource(tb));
    }

    @Override
    public Class<?> getObjectType() {
      return ITestBean.class;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
  }

}
