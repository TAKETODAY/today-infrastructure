/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.aop.proxy.ProxyFactoryBean;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.aop.support.interceptor.DebugInterceptor;
import cn.taketoday.aop.target.HotSwappableTargetSource;
import cn.taketoday.aop.target.LazyInitTargetSource;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionReference;
import cn.taketoday.beans.factory.support.DerivedTestBean;
import cn.taketoday.beans.factory.support.ITestBean;
import cn.taketoday.beans.factory.support.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.TestBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test cases for AOP transaction management.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 23.04.2003
 */
public class BeanFactoryTransactionTests {

  private StandardBeanFactory factory = new StandardBeanFactory();

  /*

	<bean id="targetDependency" class="cn.taketoday.beans.testfixture.beans.TestBean">
		<property name="name"><value>dependency</value></property>
	</bean>
*/ {
    factory.registerBeanDefinition(new BeanDefinition("targetDependency", TestBean.class)
            .addPropertyValue("name", "dependency"));
  }

  /*
	<!-- Simple target -->
	<bean id="target" class="cn.taketoday.beans.testfixture.beans.DerivedTestBean" lazy-init="true">
		<property name="name"><value>custom</value></property>
		<property name="age"><value>666</value></property>
		<property name="spouse"><ref bean="targetDependency"/></property>
	</bean>

	<bean id="debugInterceptor" class="cn.taketoday.aop.interceptor.DebugInterceptor"/>

	<bean id="mockMan" class="cn.taketoday.transaction.interceptor.PlatformTransactionManagerFacade"/>

	<bean id="txInterceptor" class="cn.taketoday.transaction.interceptor.TransactionInterceptor">
		<property name="transactionManager"><ref bean="mockMan"/></property>
		<property name="transactionAttributeSource">
			<value>
				cn.taketoday.beans.factory.support.ITestBean.s*=PROPAGATION_MANDATORY
				cn.taketoday.beans.factory.support.AgeHolder.setAg*=PROPAGATION_REQUIRED
				cn.taketoday.beans.factory.support.ITestBean.set*= PROPAGATION_SUPPORTS , readOnly
			</value>
		</property>
	</bean>
*/ {
    factory.registerBeanDefinition(new BeanDefinition("target", DerivedTestBean.class)
            .addPropertyValue("name", "custom")
            .addPropertyValue("age", "666")
            .addPropertyValue("spouse", RuntimeBeanReference.from("targetDependency"))
    );
    factory.registerBeanDefinition(new BeanDefinition("debugInterceptor", DebugInterceptor.class));
    factory.registerBeanDefinition(new BeanDefinition("mockMan", PlatformTransactionManagerFacade.class));

    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    Properties properties = new Properties();
    properties.setProperty("cn.taketoday.beans.factory.support.ITestBean.s*", "PROPAGATION_MANDATORY");
    properties.setProperty("cn.taketoday.beans.factory.support.AgeHolder.setAg*", "PROPAGATION_REQUIRED");
    properties.setProperty("cn.taketoday.beans.factory.support.ITestBean.set*", " PROPAGATION_SUPPORTS , readOnly");
    source.setProperties(properties);
    factory.registerBeanDefinition(new BeanDefinition("txInterceptor", TransactionInterceptor.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributeSource", source)
    );

  }

  /*
	<bean id="proxyFactory1" class="cn.taketoday.aop.framework.ProxyFactoryBean">
		<property name="proxyInterfaces">
			<value>cn.taketoday.beans.factory.support.ITestBean</value>
		</property>
		<property name="interceptorNames">
			<list>
				<value>txInterceptor</value>
				<value>target</value>
			</list>
		</property>
	</bean>

	<bean id="baseProxyFactory" class="cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean"
		  abstract="true">
		<property name="transactionManager"><ref bean="mockMan"/></property>
		<property name="transactionAttributes">
			<props>
				<prop key="s*">PROPAGATION_MANDATORY</prop>
				<prop key="setAg*">  PROPAGATION_REQUIRED  ,  readOnly  </prop>
				<prop key="set*">PROPAGATION_SUPPORTS</prop>
			</props>
		</property>
	</bean>

	<bean id="proxyFactory2DynamicProxy" parent="baseProxyFactory">
		<property name="target"><ref bean="target"/></property>
	</bean>
*/ {
    factory.registerBeanDefinition(new BeanDefinition("proxyFactory1", ProxyFactoryBean.class)
            .addPropertyValue("proxyInterfaces", ITestBean.class)
            .addPropertyValue("interceptorNames", "txInterceptor,target")
    );

    Properties properties = new Properties();
    properties.setProperty("s*", "PROPAGATION_MANDATORY");
    properties.setProperty("setAg*", "PROPAGATION_REQUIRED");
    properties.setProperty("set*", " PROPAGATION_SUPPORTS , readOnly");

    factory.registerBeanDefinition(new BeanDefinition("proxyFactory2DynamicProxy", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", properties)
            .addPropertyValue("target", RuntimeBeanReference.from("target"))
    );

  }

  /*
	<!--
		Same as proxyFactory2DynamicProxy but forces the use of CGLIB.
	-->
	<bean id="proxyFactory2Cglib" parent="baseProxyFactory">
		<property name="proxyTargetClass"><value>true</value></property>
		<property name="target"><ref bean="target"/></property>
	</bean>

	<bean id="proxyFactory2Lazy" parent="baseProxyFactory">
		<property name="target">
			<bean class="cn.taketoday.aop.target.LazyInitTargetSource">
				<property name="targetBeanName"><idref bean="target"/></property>
			</bean>
		</property>
	</bean>

	<bean id="proxyFactory3" parent="baseProxyFactory">
		<property name="target"><ref bean="target"/></property>
		<property name="proxyTargetClass"><value>true</value></property>
		<property name="pointcut">
			<ref bean="txnInvocationCounterPointcut"/>
		</property>
		<property name="preInterceptors">
			<list>
				<ref bean="preInvocationCounterInterceptor"/>
			</list>
		</property>
		<property name="postInterceptors">
			<list>
				<ref bean="postInvocationCounterInterceptor"/>
			</list>
		</property>
	</bean>
*/ {
    Properties properties = new Properties();
    properties.setProperty("s*", "PROPAGATION_MANDATORY");
    properties.setProperty("setAg*", "PROPAGATION_REQUIRED");
    properties.setProperty("set*", " PROPAGATION_SUPPORTS , readOnly");

    factory.registerBeanDefinition(new BeanDefinition("proxyFactory2Cglib", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", properties)
            .addPropertyValue("target", RuntimeBeanReference.from("target"))
    );

    factory.registerBeanDefinition(new BeanDefinition("proxyFactory2Lazy", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", properties)
            .addPropertyValue("target", BeanDefinitionReference.from(
                    BeanDefinitionBuilder.from(LazyInitTargetSource.class)
                            .propertyValues(new PropertyValues().add("targetBeanName", "target")))
            )
    );

    factory.registerBeanDefinition(new BeanDefinition("proxyFactory3", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", properties)
            .addPropertyValue("target", RuntimeBeanReference.from("target"))
            .addPropertyValue("proxyTargetClass", true)
            .addPropertyValue("pointcut", RuntimeBeanReference.from("txnInvocationCounterPointcut"))
            .addPropertyValue("preInterceptors", RuntimeBeanReference.from("preInvocationCounterInterceptor"))
            .addPropertyValue("postInterceptors", RuntimeBeanReference.from("preInvocationCounterInterceptor"))
    );

  }
  /*
	<bean name="cglibNoInterfaces" class="cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean">
		<property name="transactionManager"><ref bean="mockMan"/></property>
		<property name="target">
			<bean class="cn.taketoday.transaction.interceptor.ImplementsNoInterfaces">
				<property name="dependency"><ref bean="targetDependency"/></property>
			</bean>
		</property>
		<property name="transactionAttributes">
			<props>
				<prop key="*">PROPAGATION_REQUIRED</prop>
			</props>
		</property>
	</bean>

	<!--
		The HotSwappableTargetSource is a Type 3 component.
	-->
	<bean id="swapper" class="cn.taketoday.aop.target.HotSwappableTargetSource">
		<constructor-arg><ref bean="target"/></constructor-arg>
	</bean>

	<bean id="hotSwapped" class="cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean">
		<property name="transactionManager"><ref bean="mockMan"/></property>
		<!-- Should automatically pick up the target source, rather than simple target -->
		<property name="target"><ref bean="swapper"/></property>
		<property name="transactionAttributes">
			<props>
				<prop key="s*">PROPAGATION_MANDATORY</prop>
				<prop key="setAg*">PROPAGATION_REQUIRED</prop>
				<prop key="set*">PROPAGATION_SUPPORTS</prop>
			</props>
		</property>
		<property name="proxyTargetClass"><value>true</value></property>
		<property name="optimize"><value>false</value></property>
	</bean>

	<bean id="txnInvocationCounterPointcut"
			class="cn.taketoday.transaction.interceptor.BeanFactoryTransactionTests$InvocationCounterPointcut"/>

	<bean id="preInvocationCounterInterceptor" class="cn.taketoday.transaction.interceptor.BeanFactoryTransactionTests$InvocationCounterInterceptor"/>

	<bean id="postInvocationCounterInterceptor" class="cn.taketoday.transaction.interceptor.BeanFactoryTransactionTests$InvocationCounterInterceptor"/>

  */

  {
    Properties properties = new Properties();
    properties.setProperty("*", "PROPAGATION_REQUIRED");
    factory.registerBeanDefinition(new BeanDefinition("cglibNoInterfaces", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", properties)
            .addPropertyValue("target", BeanDefinitionReference.from(
                    BeanDefinitionBuilder.from(ImplementsNoInterfaces.class)
                            .propertyValues(new PropertyValues()
                                    .add("dependency", RuntimeBeanReference.from("targetDependency"))))
            )
    );

    // The HotSwappableTargetSource is a Type 3 component.

    BeanDefinition definition = new BeanDefinition("swapper", HotSwappableTargetSource.class);
    definition.getConstructorArgumentValues().addGenericArgumentValue(RuntimeBeanReference.from("target"));
//    definition.setInstanceSupplier(() -> new HotSwappableTargetSource(factory.getBean("target")));
    factory.registerBeanDefinition(definition);

    Properties swapperProperties = new Properties();
    swapperProperties.setProperty("s*", "PROPAGATION_MANDATORY");
    swapperProperties.setProperty("setAg*", "PROPAGATION_REQUIRED");
    swapperProperties.setProperty("set*", "PROPAGATION_SUPPORTS");

    factory.registerBeanDefinition(new BeanDefinition("hotSwapped", TransactionProxyFactoryBean.class)
            .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
            .addPropertyValue("transactionAttributes", swapperProperties)
            .addPropertyValue("target", RuntimeBeanReference.from("swapper"))
            .addPropertyValue("proxyTargetClass", true)
            .addPropertyValue("optimize", false)
    );

    factory.registerBeanDefinition(new BeanDefinition("txnInvocationCounterPointcut", InvocationCounterPointcut.class));
    factory.registerBeanDefinition(new BeanDefinition("preInvocationCounterInterceptor", InvocationCounterInterceptor.class));
    factory.registerBeanDefinition(new BeanDefinition("postInvocationCounterInterceptor", InvocationCounterInterceptor.class));

  }

  @Test
  public void testGetsAreNotTransactionalWithProxyFactory1() {
    ITestBean testBean = (ITestBean) factory.getBean("proxyFactory1");
    assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
    boolean condition = testBean instanceof TransactionalProxy;
    assertThat(condition).isFalse();
    doTestGetsAreNotTransactional(testBean);
  }

  @Test
  public void testGetsAreNotTransactionalWithProxyFactory2DynamicProxy() {
    this.factory.preInstantiateSingletons();
    ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2DynamicProxy");
    assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
    boolean condition = testBean instanceof TransactionalProxy;
    assertThat(condition).isTrue();
    doTestGetsAreNotTransactional(testBean);
  }

  @Test
  public void testGetsAreNotTransactionalWithProxyFactory2Cglib() {
    ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Cglib");

    assertThat(AopUtils.isCglibProxy(testBean))
            .as("testBean is CGLIB advised")
            .isTrue();

    boolean condition = testBean instanceof TransactionalProxy;
    assertThat(condition).isTrue();
    doTestGetsAreNotTransactional(testBean);
  }

  @Test
  public void testProxyFactory2Lazy() {
    ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Lazy");
    assertThat(factory.containsSingleton("target")).isFalse();
    assertThat(testBean.getAge()).isEqualTo(666);
    assertThat(factory.containsSingleton("target")).isTrue();
  }

  @Test
  public void testCglibTransactionProxyImplementsNoInterfaces() {
    ImplementsNoInterfaces ini = (ImplementsNoInterfaces) factory.getBean("cglibNoInterfaces");
    assertThat(AopUtils.isCglibProxy(ini)).as("testBean is CGLIB advised").isTrue();
    boolean condition = ini instanceof TransactionalProxy;
    assertThat(condition).isTrue();
    String newName = "Gordon";

    // Install facade
    CallCountingTransactionManager ptm = new CallCountingTransactionManager();
    PlatformTransactionManagerFacade.delegate = ptm;

    ini.setName(newName);
    assertThat(ini.getName()).isEqualTo(newName);
    assertThat(ptm.commits).isEqualTo(2);
  }

  @Test
  public void testGetsAreNotTransactionalWithProxyFactory3() {
    ITestBean testBean = (ITestBean) factory.getBean("proxyFactory3");
    boolean condition = testBean instanceof DerivedTestBean;
    assertThat(condition).as("testBean is a full proxy").isTrue();
    boolean condition1 = testBean instanceof TransactionalProxy;
    assertThat(condition1).isTrue();
    InvocationCounterPointcut txnCounter = (InvocationCounterPointcut) factory.getBean("txnInvocationCounterPointcut");
    InvocationCounterInterceptor preCounter = (InvocationCounterInterceptor) factory.getBean("preInvocationCounterInterceptor");
    InvocationCounterInterceptor postCounter = (InvocationCounterInterceptor) factory.getBean("postInvocationCounterInterceptor");
    txnCounter.counter = 0;
    preCounter.counter = 0;
    postCounter.counter = 0;
    doTestGetsAreNotTransactional(testBean);
    // Can't assert it's equal to 4 as the pointcut may be optimized and only invoked once
    assertThat(0 < txnCounter.counter && txnCounter.counter <= 4).isTrue();
    assertThat(preCounter.counter).isEqualTo(4);
    assertThat(postCounter.counter).isEqualTo(4);
  }

  private void doTestGetsAreNotTransactional(final ITestBean testBean) {
    // Install facade
    PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
    PlatformTransactionManagerFacade.delegate = ptm;

    assertThat(testBean.getAge() == 666).as("Age should not be " + testBean.getAge()).isTrue();

    // Expect no methods
    verifyNoInteractions(ptm);

    // Install facade expecting a call
    final TransactionStatus ts = mock(TransactionStatus.class);
    ptm = new PlatformTransactionManager() {
      private boolean invoked;

      @Override
      public TransactionStatus getTransaction(@Nullable TransactionDefinition def) throws TransactionException {
        if (invoked) {
          throw new IllegalStateException("getTransaction should not get invoked more than once");
        }
        invoked = true;
        if (!(def.getName().contains(DerivedTestBean.class.getName()) && def.getName().contains("setAge"))) {
          throw new IllegalStateException(
                  "transaction name should contain class and method name: " + def.getName());
        }
        return ts;
      }

      @Override
      public void commit(TransactionStatus status) throws TransactionException {
        assertThat(status == ts).isTrue();
      }

      @Override
      public void rollback(TransactionStatus status) throws TransactionException {
        throw new IllegalStateException("rollback should not get invoked");
      }
    };
    PlatformTransactionManagerFacade.delegate = ptm;

    // TODO same as old age to avoid ordering effect for now
    int age = 666;
    testBean.setAge(age);
    assertThat(testBean.getAge() == age).isTrue();
  }

  @Test
  public void testGetBeansOfTypeWithAbstract() {
    Map<String, ITestBean> beansOfType = factory.getBeansOfType(ITestBean.class, true, true);
    assertThat(beansOfType).isNotNull();
  }

  /**
   * Check that we fail gracefully if the user doesn't set any transaction attributes.
   */
  @Test
  public void testNoTransactionAttributeSource() {
    /*
	<!-- Simple target -->
	<bean id="target" class="cn.taketoday.beans.testfixture.beans.DerivedTestBean">
		<property name="name"><value>custom</value></property>
		<property name="age"><value>666</value></property>
	</bean>

	<bean id="mockMan" class="cn.taketoday.transaction.interceptor.PlatformTransactionManagerFacade"/>

	<!--
		Invalid: we need a transaction attribute source
	-->
	<bean id="noTransactionAttributeSource" class="cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean">
		<property name="transactionManager"><ref bean="mockMan"/></property>
		<property name="target"><ref bean="target"/></property>
	</bean>
	*/

    assertThatExceptionOfType(BeansException.class).isThrownBy(() -> {
      StandardBeanFactory bf = new StandardBeanFactory();
      bf.registerBeanDefinition(new BeanDefinition("target", DerivedTestBean.class));
      bf.registerBeanDefinition(new BeanDefinition("mockMan", PlatformTransactionManagerFacade.class));
      bf.registerBeanDefinition(new BeanDefinition("noTransactionAttributeSource", TransactionProxyFactoryBean.class)
              .addPropertyValue("transactionManager", RuntimeBeanReference.from("mockMan"))
              .addPropertyValue("target", RuntimeBeanReference.from("target"))
      );

      bf.getBean("noTransactionAttributeSource");
    });
  }

  /**
   * Test that we can set the target to a dynamic TargetSource.
   */
  @Test
  public void testDynamicTargetSource() {
    // Install facade
    CallCountingTransactionManager txMan = new CallCountingTransactionManager();
    PlatformTransactionManagerFacade.delegate = txMan;

    TestBean tb = (TestBean) factory.getBean("hotSwapped");
    assertThat(tb.getAge()).isEqualTo(666);
    int newAge = 557;
    tb.setAge(newAge);
    assertThat(tb.getAge()).isEqualTo(newAge);

    TestBean target2 = new TestBean();
    target2.setAge(65);
    HotSwappableTargetSource ts = (HotSwappableTargetSource) factory.getBean("swapper");
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
