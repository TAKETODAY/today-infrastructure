/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.aop.support.AopUtils;
import infra.aop.support.StaticMethodMatcherPointcut;
import infra.aop.target.HotSwappableTargetSource;
import infra.beans.FatalBeanException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionStatus;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test cases for AOP transaction management.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class BeanFactoryTransactionTests {

  private final StandardBeanFactory factory = new StandardBeanFactory();

  @BeforeEach
  void loadBeanDefinitions() {
    new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(
            new ClassPathResource("transactionalBeanFactory.xml", getClass()));
  }

  @Test
  void getsAreNotTransactionalWithProxyFactory1() {
    ITestBean testBean = factory.getBean("proxyFactory1", ITestBean.class);
    assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
    assertThat(testBean).isNotInstanceOf(TransactionalProxy.class);
    assertGetsAreNotTransactional(testBean);
  }

  @Test
  void getsAreNotTransactionalWithProxyFactory2DynamicProxy() {
    this.factory.preInstantiateSingletons();
    ITestBean testBean = factory.getBean("proxyFactory2DynamicProxy", ITestBean.class);
    assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
    assertThat(testBean).isInstanceOf(TransactionalProxy.class);
    assertGetsAreNotTransactional(testBean);
  }

  @Test
  void getsAreNotTransactionalWithProxyFactory2Cglib() {
    ITestBean testBean = factory.getBean("proxyFactory2Cglib", ITestBean.class);
    assertThat(AopUtils.isCglibProxy(testBean)).as("testBean is CGLIB advised").isTrue();
    assertThat(testBean).isInstanceOf(TransactionalProxy.class);
    assertGetsAreNotTransactional(testBean);
  }

  @Test
  void proxyFactory2Lazy() {
    ITestBean testBean = factory.getBean("proxyFactory2Lazy", ITestBean.class);
    assertThat(factory.containsSingleton("target")).isFalse();
    assertThat(testBean.getAge()).isEqualTo(666);
    assertThat(factory.containsSingleton("target")).isTrue();
  }

  @Test
  void cglibTransactionProxyImplementsNoInterfaces() {
    ImplementsNoInterfaces ini = factory.getBean("cglibNoInterfaces", ImplementsNoInterfaces.class);
    assertThat(AopUtils.isCglibProxy(ini)).as("testBean is CGLIB advised").isTrue();
    assertThat(ini).isInstanceOf(TransactionalProxy.class);
    String newName = "Gordon";

    // Install facade
    CallCountingTransactionManager ptm = new CallCountingTransactionManager();
    PlatformTransactionManagerFacade.delegate = ptm;

    ini.setName(newName);
    assertThat(ini.getName()).isEqualTo(newName);
    assertThat(ptm.commits).isEqualTo(2);
  }

  @Test
  void getsAreNotTransactionalWithProxyFactory3() {
    ITestBean testBean = factory.getBean("proxyFactory3", ITestBean.class);
    assertThat(testBean).as("testBean is a full proxy")
            .isInstanceOf(DerivedTestBean.class)
            .isInstanceOf(TransactionalProxy.class);

    InvocationCounterPointcut txnPointcut = factory.getBean("txnInvocationCounterPointcut", InvocationCounterPointcut.class);
    InvocationCounterInterceptor preInterceptor = factory.getBean("preInvocationCounterInterceptor", InvocationCounterInterceptor.class);
    InvocationCounterInterceptor postInterceptor = factory.getBean("postInvocationCounterInterceptor", InvocationCounterInterceptor.class);
    assertThat(txnPointcut.counter).as("txnPointcut").isGreaterThan(0);
    assertThat(preInterceptor.counter).as("preInterceptor").isZero();
    assertThat(postInterceptor.counter).as("postInterceptor").isZero();

    // Reset counters
    txnPointcut.counter = 0;
    preInterceptor.counter = 0;
    postInterceptor.counter = 0;

    // Invokes: getAge() * 2 and setAge() * 1 --> 2 + 1 = 3 method invocations.
    assertGetsAreNotTransactional(testBean);

    // The matches(Method, Class) method of the static transaction pointcut should not
    // have been invoked for the actual invocation of the getAge() and setAge() methods.
    assertThat(txnPointcut.counter).as("txnPointcut").isZero();

    assertThat(preInterceptor.counter).as("preInterceptor").isEqualTo(3);
    assertThat(postInterceptor.counter).as("postInterceptor").isEqualTo(3);
  }

  private void assertGetsAreNotTransactional(ITestBean testBean) {
    // Install facade
    PlatformTransactionManager ptm = mock();
    PlatformTransactionManagerFacade.delegate = ptm;

    assertThat(testBean.getAge()).as("Age").isEqualTo(666);

    // Expect no interactions with the transaction manager.
    verifyNoInteractions(ptm);

    // Install facade expecting a call
    AtomicBoolean invoked = new AtomicBoolean();
    TransactionStatus ts = mock();
    ptm = new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(@Nullable TransactionDefinition def) throws TransactionException {
        assertThat(invoked.compareAndSet(false, true))
                .as("getTransaction() should not get invoked more than once").isTrue();
        assertThat(def.getName()).as("transaction name").contains(DerivedTestBean.class.getName(), "setAge");
        return ts;
      }

      @Override
      public void commit(TransactionStatus status) throws TransactionException {
        assertThat(status).isSameAs(ts);
      }

      @Override
      public void rollback(TransactionStatus status) throws TransactionException {
        throw new IllegalStateException("rollback should not get invoked");
      }
    };
    PlatformTransactionManagerFacade.delegate = ptm;

    assertThat(invoked).as("getTransaction() invoked before setAge()").isFalse();
    testBean.setAge(42);
    assertThat(invoked).as("getTransaction() invoked after setAge()").isTrue();
    assertThat(testBean.getAge()).as("Age").isEqualTo(42);
  }

  @Test
  void getBeansOfTypeWithAbstract() {
    Map<String, ITestBean> beansOfType = factory.getBeansOfType(ITestBean.class, true, true);
    assertThat(beansOfType).isNotNull();
  }

  /**
   * Check that we fail gracefully if the user doesn't set any transaction attributes.
   */
  @Test
  void noTransactionAttributeSource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource("noTransactionAttributeSource.xml", getClass()));
    assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() -> bf.getBean("noTransactionAttributeSource"));
  }

  /**
   * Test that we can set the target to a dynamic TargetSource.
   */
  @Test
  void dynamicTargetSource() {
    // Install facade
    CallCountingTransactionManager txMan = new CallCountingTransactionManager();
    PlatformTransactionManagerFacade.delegate = txMan;

    TestBean tb = factory.getBean("hotSwapped", TestBean.class);
    assertThat(tb.getAge()).isEqualTo(666);
    int newAge = 557;
    tb.setAge(newAge);
    assertThat(tb.getAge()).isEqualTo(newAge);

    TestBean target2 = new TestBean();
    target2.setAge(65);
    HotSwappableTargetSource ts = factory.getBean("swapper", HotSwappableTargetSource.class);
    ts.swap(target2);
    assertThat(tb.getAge()).isEqualTo(target2.getAge());
    tb.setAge(newAge);
    assertThat(target2.getAge()).isEqualTo(newAge);

    assertThat(txMan.inflight).isEqualTo(0);
    assertThat(txMan.commits).isEqualTo(2);
    assertThat(txMan.rollbacks).isEqualTo(0);
  }

  public static class InvocationCounterPointcut extends StaticMethodMatcherPointcut {

    int counter = 0;

    @Override
    public boolean matches(Method method, @Nullable Class<?> clazz) {
      counter++;
      return true;
    }
  }

  public static class InvocationCounterInterceptor implements MethodInterceptor {

    int counter = 0;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
      counter++;
      return methodInvocation.proceed();
    }
  }

}
