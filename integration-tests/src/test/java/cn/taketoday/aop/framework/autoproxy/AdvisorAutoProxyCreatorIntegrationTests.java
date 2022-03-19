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

package cn.taketoday.aop.framework.autoproxy;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.StaticMethodMatcherPointcutAdvisor;
import cn.taketoday.aop.testfixture.testfixture.advice.CountingBeforeAdvice;
import cn.taketoday.aop.testfixture.testfixture.advice.MethodCounter;
import cn.taketoday.aop.testfixture.testfixture.interceptor.NopInterceptor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.testfixture.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.NoTransactionException;
import cn.taketoday.transaction.interceptor.TransactionInterceptor;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for auto proxy creation by advisor recognition working in
 * conjunction with transaction management resources.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @see cn.taketoday.aop.framework.autoproxy.AdvisorAutoProxyCreatorTests
 */
class AdvisorAutoProxyCreatorIntegrationTests {

  private static final Class<?> CLASS = AdvisorAutoProxyCreatorIntegrationTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();

  private static final String DEFAULT_CONTEXT = CLASSNAME + "-context.xml";

  private static final String ADVISOR_APC_BEAN_NAME = "aapc";
  private static final String TXMANAGER_BEAN_NAME = "txManager";

  /**
   * Return a bean factory with attributes and EnterpriseServices configured.
   */
  protected BeanFactory getBeanFactory() throws IOException {
    return new ClassPathXmlApplicationContext(DEFAULT_CONTEXT, CLASS);
  }

  @Test
  void testDefaultExclusionPrefix() throws Exception {
    DefaultAdvisorAutoProxyCreator aapc = (DefaultAdvisorAutoProxyCreator) getBeanFactory().getBean(ADVISOR_APC_BEAN_NAME);
    assertThat(aapc.getAdvisorBeanNamePrefix()).isEqualTo((ADVISOR_APC_BEAN_NAME + DefaultAdvisorAutoProxyCreator.SEPARATOR));
    assertThat(aapc.isUsePrefix()).isFalse();
  }

  /**
   * If no pointcuts match (no attrs) there should be proxying.
   */
  @Test
  void testNoProxy() throws Exception {
    BeanFactory bf = getBeanFactory();
    Object o = bf.getBean("noSetters");
    assertThat(AopUtils.isAopProxy(o)).isFalse();
  }

  @Test
  void testTxIsProxied() throws Exception {
    BeanFactory bf = getBeanFactory();
    ITestBean test = (ITestBean) bf.getBean("test");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
  }

  @Test
  void testRegexpApplied() throws Exception {
    BeanFactory bf = getBeanFactory();
    ITestBean test = (ITestBean) bf.getBean("test");
    MethodCounter counter = (MethodCounter) bf.getBean("countingAdvice");
    assertThat(counter.getCalls()).isEqualTo(0);
    test.getName();
    assertThat(counter.getCalls()).isEqualTo(1);
  }

  @Test
  void testTransactionAttributeOnMethod() throws Exception {
    BeanFactory bf = getBeanFactory();
    ITestBean test = (ITestBean) bf.getBean("test");

    CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
    OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
    assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);

    assertThat(txMan.commits).isEqualTo(0);
    assertThat(test.getAge()).as("Initial value was correct").isEqualTo(4);
    int newAge = 5;
    test.setAge(newAge);
    assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(1);

    assertThat(test.getAge()).as("New value set correctly").isEqualTo(newAge);
    assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
  }

  /**
   * Should not roll back on servlet exception.
   */
  @Test
  void testRollbackRulesOnMethodCauseRollback() throws Exception {
    BeanFactory bf = getBeanFactory();
    Rollback rb = (Rollback) bf.getBean("rollback");

    CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
    OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
    assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);

    assertThat(txMan.commits).isEqualTo(0);
    rb.echoException(null);
    // Fires only on setters
    assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);
    assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);

    assertThat(txMan.rollbacks).isEqualTo(0);
    Exception ex = new Exception();
    try {
      rb.echoException(ex);
    }
    catch (Exception actual) {
      assertThat(actual).isEqualTo(ex);
    }
    assertThat(txMan.rollbacks).as("Transaction counts match").isEqualTo(1);
  }

  @Test
  void testRollbackRulesOnMethodPreventRollback() throws Exception {
    BeanFactory bf = getBeanFactory();
    Rollback rb = (Rollback) bf.getBean("rollback");

    CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

    assertThat(txMan.commits).isEqualTo(0);
    // Should NOT roll back on ServletException
    try {
      rb.echoException(new ServletException());
    }
    catch (ServletException ex) {

    }
    assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
  }

  @Test
  void testProgrammaticRollback() throws Exception {
    BeanFactory bf = getBeanFactory();

    Object bean = bf.getBean(TXMANAGER_BEAN_NAME);
    boolean condition = bean instanceof CallCountingTransactionManager;
    assertThat(condition).isTrue();
    CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

    Rollback rb = (Rollback) bf.getBean("rollback");
    assertThat(txMan.commits).isEqualTo(0);
    rb.rollbackOnly(false);
    assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
    assertThat(txMan.rollbacks).isEqualTo(0);
    // Will cause rollback only
    rb.rollbackOnly(true);
    assertThat(txMan.rollbacks).isEqualTo(1);
  }

}

@SuppressWarnings("serial")
class NeverMatchAdvisor extends StaticMethodMatcherPointcutAdvisor {

  public NeverMatchAdvisor() {
    super(new NopInterceptor());
  }

  /**
   * This method is solely to allow us to create a mixture of dependencies in
   * the bean definitions. The dependencies don't have any meaning, and don't
   * <b>do</b> anything.
   */
  public void setDependencies(List<?> l) {

  }

  /**
   * @see cn.taketoday.aop.MethodMatcher#matches(Method, Class)
   */
  @Override
  public boolean matches(Method m, @Nullable Class<?> targetClass) {
    return false;
  }

}

class NoSetters {

  public void A() {

  }

  public int getB() {
    return -1;
  }

}

@SuppressWarnings("serial")
class OrderedTxCheckAdvisor extends StaticMethodMatcherPointcutAdvisor implements InitializingBean {

  /**
   * Should we insist on the presence of a transaction attribute or refuse to accept one?
   */
  private boolean requireTransactionContext = false;

  public void setRequireTransactionContext(boolean requireTransactionContext) {
    this.requireTransactionContext = requireTransactionContext;
  }

  public boolean isRequireTransactionContext() {
    return requireTransactionContext;
  }

  public CountingBeforeAdvice getCountingBeforeAdvice() {
    return (CountingBeforeAdvice) getAdvice();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    setAdvice(new TxCountingBeforeAdvice());
  }

  @Override
  public boolean matches(Method method, @Nullable Class<?> targetClass) {
    return method.getName().startsWith("setAge");
  }

  private class TxCountingBeforeAdvice extends CountingBeforeAdvice {

    @Override
    public void before(MethodInvocation invocation) throws Throwable {
      // do transaction checks
      if (requireTransactionContext) {
        TransactionInterceptor.currentTransactionStatus();
      }
      else {
        try {
          TransactionInterceptor.currentTransactionStatus();
          throw new RuntimeException("Shouldn't have a transaction");
        }
        catch (NoTransactionException ex) {
          // this is Ok
        }
      }
      super.before(invocation);
    }
  }

}

class Rollback {

  /**
   * Inherits transaction attribute.
   * Illustrates programmatic rollback.
   */
  public void rollbackOnly(boolean rollbackOnly) {
    if (rollbackOnly) {
      setRollbackOnly();
    }
  }

  /**
   * Extracted in a protected method to facilitate testing
   */
  protected void setRollbackOnly() {
    TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
  }

  /**
   * @cn.taketoday.transaction.interceptor.RuleBasedTransaction (timeout = - 1)
   * @cn.taketoday.transaction.interceptor.RollbackRule (" java.lang.Exception ")
   * @cn.taketoday.transaction.interceptor.NoRollbackRule (" ServletException ")
   */
  public void echoException(Exception ex) throws Exception {
    if (ex != null) {
      throw ex;
    }
  }

}
