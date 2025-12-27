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

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Properties;

import infra.aop.framework.ProxyFactory;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.core.ResolvableType;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionManager;
import infra.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for TransactionInterceptor.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
class TransactionInterceptorTests extends AbstractTransactionAspectTests {

  @Override
  protected Object advised(Object target, PlatformTransactionManager ptm, TransactionAttributeSource[] tas) {
    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionManager(ptm);
    ti.setTransactionAttributeSources(tas);

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvice(0, ti);
    return pf.getProxy();
  }

  /**
   * Template method to create an advised object given the
   * target object and transaction setup.
   * Creates a TransactionInterceptor and applies it.
   */
  @Override
  protected Object advised(Object target, PlatformTransactionManager ptm, TransactionAttributeSource tas) {
    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionManager(ptm);
    assertThat(ti.getTransactionManager()).isEqualTo(ptm);
    ti.setTransactionAttributeSource(tas);
    assertThat(ti.getTransactionAttributeSource()).isEqualTo(tas);

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvice(0, ti);
    return pf.getProxy();
  }

  /**
   * A TransactionInterceptor should be serializable if its
   * PlatformTransactionManager is.
   */
  @Test
  void serializableWithAttributeProperties() throws Exception {
    TransactionInterceptor ti = new TransactionInterceptor();
    Properties props = new Properties();
    props.setProperty("methodName", "PROPAGATION_REQUIRED");
    ti.setTransactionAttributes(props);
    PlatformTransactionManager ptm = new SerializableTransactionManager();
    ti.setTransactionManager(ptm);
    ti = SerializationTestUtils.serializeAndDeserialize(ti);

    // Check that logger survived deserialization
    assertThat(ti.logger).isNotNull();
    assertThat(ti.getTransactionManager()).isInstanceOf(SerializableTransactionManager.class);
    assertThat(ti.getTransactionAttributeSource()).isNotNull();
  }

  @Test
  void serializableWithCompositeSource() throws Exception {
    NameMatchTransactionAttributeSource tas1 = new NameMatchTransactionAttributeSource();
    Properties props = new Properties();
    props.setProperty("methodName", "PROPAGATION_REQUIRED");
    tas1.setProperties(props);

    NameMatchTransactionAttributeSource tas2 = new NameMatchTransactionAttributeSource();
    props = new Properties();
    props.setProperty("otherMethodName", "PROPAGATION_REQUIRES_NEW");
    tas2.setProperties(props);

    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionAttributeSources(tas1, tas2);
    PlatformTransactionManager ptm = new SerializableTransactionManager();
    ti.setTransactionManager(ptm);
    ti = SerializationTestUtils.serializeAndDeserialize(ti);

    assertThat(ti.getTransactionManager() instanceof SerializableTransactionManager).isTrue();
    assertThat(ti.getTransactionAttributeSource() instanceof CompositeTransactionAttributeSource).isTrue();
    CompositeTransactionAttributeSource ctas = (CompositeTransactionAttributeSource) ti.getTransactionAttributeSource();
    assertThat(ctas.getTransactionAttributeSources()[0] instanceof NameMatchTransactionAttributeSource).isTrue();
    assertThat(ctas.getTransactionAttributeSources()[1] instanceof NameMatchTransactionAttributeSource).isTrue();
  }

  @Test
  void determineTransactionManagerWithNoBeanFactory() {
    PlatformTransactionManager transactionManager = mock();
    TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

    assertThat(ti.determineTransactionManager(new DefaultTransactionAttribute(), null)).isSameAs(transactionManager);
  }

  @Test
  void determineTransactionManagerWithNoBeanFactoryAndNoTransactionAttribute() {
    PlatformTransactionManager transactionManager = mock();
    TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

    assertThat(ti.determineTransactionManager(null, null)).isSameAs(transactionManager);
  }

  @Test
  void determineTransactionManagerWithNoTransactionAttribute() {
    BeanFactory beanFactory = mock();
    TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

    assertThat(ti.determineTransactionManager(null, null)).isNull();
  }

  @Test
  void determineTransactionManagerWithQualifierUnknown() {
    BeanFactory beanFactory = mock();
    TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("fooTransactionManager");

    given(beanFactory.getBeanNamesForType(any(Class.class))).willReturn(new String[0]);

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> ti.determineTransactionManager(attribute, null))
            .withMessageContaining("'fooTransactionManager'");
  }

  @Test
  void determineTransactionManagerWithQualifierAndDefault() {
    BeanFactory beanFactory = mock();
    PlatformTransactionManager transactionManager = mock();
    TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, beanFactory);
    PlatformTransactionManager fooTransactionManager =
            associateTransactionManager(beanFactory, "fooTransactionManager");

    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("fooTransactionManager");

    assertThat(ti.determineTransactionManager(attribute, null)).isSameAs(fooTransactionManager);
  }

  @Test
  void determineTransactionManagerWithQualifierAndDefaultName() {
    BeanFactory beanFactory = mock();
    associateTransactionManager(beanFactory, "defaultTransactionManager");
    TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
            "defaultTransactionManager", beanFactory);

    PlatformTransactionManager fooTransactionManager =
            associateTransactionManager(beanFactory, "fooTransactionManager");
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("fooTransactionManager");

    assertThat(ti.determineTransactionManager(attribute, null)).isSameAs(fooTransactionManager);
  }

  @Test
  void determineTransactionManagerWithEmptyQualifierAndDefaultName() {
    BeanFactory beanFactory = mock();
    PlatformTransactionManager defaultTransactionManager
            = associateTransactionManager(beanFactory, "defaultTransactionManager");
    TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
            "defaultTransactionManager", beanFactory);

    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("");

    assertThat(ti.determineTransactionManager(attribute, null)).isSameAs(defaultTransactionManager);
  }

  @Test
  void determineTransactionManagerWithQualifierSeveralTimes() {
    BeanFactory beanFactory = mock();
    TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

    PlatformTransactionManager txManager = associateTransactionManager(beanFactory, "fooTransactionManager");

    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("fooTransactionManager");
    TransactionManager actual = ti.determineTransactionManager(attribute, null);
    assertThat(actual).isSameAs(txManager);

    // Call again, should be cached
    TransactionManager actual2 = ti.determineTransactionManager(attribute, null);
    assertThat(actual2).isSameAs(txManager);
    verify(beanFactory, times(1)).containsBean("fooTransactionManager");
    verify(beanFactory, times(1)).getBean("fooTransactionManager", TransactionManager.class);
  }

  @Test
  void determineTransactionManagerWithBeanNameSeveralTimes() {
    BeanFactory beanFactory = mock(BeanFactory.class);
    TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
            "fooTransactionManager", beanFactory);

    PlatformTransactionManager txManager = associateTransactionManager(beanFactory, "fooTransactionManager");

    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    TransactionManager actual = ti.determineTransactionManager(attribute, null);
    assertThat(actual).isSameAs(txManager);

    // Call again, should be cached
    TransactionManager actual2 = ti.determineTransactionManager(attribute, null);
    assertThat(actual2).isSameAs(txManager);
    verify(beanFactory, times(1)).getBean("fooTransactionManager", TransactionManager.class);
  }

  @Test
  void determineTransactionManagerDefaultSeveralTimes() {
    BeanFactory beanFactory = mock();
    TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

    PlatformTransactionManager txManager = mock();
    given(beanFactory.getBean(TransactionManager.class)).willReturn(txManager);

    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    TransactionManager actual = ti.determineTransactionManager(attribute, null);
    assertThat(actual).isSameAs(txManager);

    // Call again, should be cached
    TransactionManager actual2 = ti.determineTransactionManager(attribute, null);
    assertThat(actual2).isSameAs(txManager);
    verify(beanFactory, times(1)).getBean(TransactionManager.class);
  }

  @Test
  void defaultConstructorCreatesInstance() {
    TransactionInterceptor interceptor = new TransactionInterceptor();
    assertThat(interceptor).isNotNull();
  }

  @Test
  void constructorWithTransactionManagerAndTransactionAttributeSource() {
    PlatformTransactionManager transactionManager = mock();
    TransactionAttributeSource attributeSource = mock();

    TransactionInterceptor interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(transactionManager);
    interceptor.setTransactionAttributeSource(attributeSource);

    assertThat(interceptor.getTransactionManager()).isEqualTo(transactionManager);
    assertThat(interceptor.getTransactionAttributeSource()).isEqualTo(attributeSource);
  }

  @Test
  void constructorWithTransactionManagerAndProperties() {
    PlatformTransactionManager transactionManager = mock();
    Properties properties = new Properties();
    properties.setProperty("testMethod", "PROPAGATION_REQUIRED");

    TransactionInterceptor interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(transactionManager);
    interceptor.setTransactionAttributes(properties);

    assertThat(interceptor.getTransactionManager()).isEqualTo(transactionManager);
    assertThat(interceptor.getTransactionAttributeSource()).isNotNull();
  }

  @Test
  void invokeWithValidMethodInvocation() throws Throwable {
    // Create a mock method invocation
    MethodInvocation invocation = mock();
    given(invocation.getMethod()).willReturn(TestClass.class.getMethod("testMethod"));
    given(invocation.getThis()).willReturn(new TestClass());
    given(invocation.proceed()).willReturn("result");

    TransactionInterceptor interceptor = new TransactionInterceptor();

    // This test mainly verifies that the method can be invoked without exceptions
    assertThatCode(() -> interceptor.invoke(invocation)).doesNotThrowAnyException();
  }

  @Test
  void invokeWithNullTarget() throws Throwable {
    MethodInvocation invocation = mock();
    given(invocation.getMethod()).willReturn(TestClass.class.getMethod("testMethod"));
    given(invocation.getThis()).willReturn(null);
    given(invocation.proceed()).willReturn("result");

    TransactionInterceptor interceptor = new TransactionInterceptor();

    assertThatCode(() -> interceptor.invoke(invocation)).doesNotThrowAnyException();
  }

  @Test
  void serializableWithAttributeSource() throws Exception {
    TransactionInterceptor ti = new TransactionInterceptor();
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    source.addTransactionalMethod("test", new DefaultTransactionAttribute());
    ti.setTransactionAttributeSource(source);
    PlatformTransactionManager ptm = new TransactionInterceptorTests.SerializableTransactionManager();
    ti.setTransactionManager(ptm);

    TransactionInterceptor deserialized = SerializationTestUtils.serializeAndDeserialize(ti);

    assertThat(deserialized).isNotNull();
    assertThat(deserialized.getTransactionManager()).isInstanceOf(TransactionInterceptorTests.SerializableTransactionManager.class);
    assertThat(deserialized.getTransactionAttributeSource()).isNotNull();
  }

  @Test
  void serializableWithMultipleAttributeSources() throws Exception {
    NameMatchTransactionAttributeSource tas1 = new NameMatchTransactionAttributeSource();
    Properties props = new Properties();
    props.setProperty("methodName", "PROPAGATION_REQUIRED");
    tas1.setProperties(props);

    NameMatchTransactionAttributeSource tas2 = new NameMatchTransactionAttributeSource();
    props = new Properties();
    props.setProperty("otherMethodName", "PROPAGATION_REQUIRES_NEW");
    tas2.setProperties(props);

    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionAttributeSources(tas1, tas2);
    PlatformTransactionManager ptm = new TransactionInterceptorTests.SerializableTransactionManager();
    ti.setTransactionManager(ptm);

    TransactionInterceptor deserialized = SerializationTestUtils.serializeAndDeserialize(ti);

    assertThat(deserialized.getTransactionManager() instanceof TransactionInterceptorTests.SerializableTransactionManager).isTrue();
    assertThat(deserialized.getTransactionAttributeSource() instanceof CompositeTransactionAttributeSource).isTrue();
  }

  static class TestClass {
    public void testMethod() {
    }
  }

  private TransactionInterceptor createTransactionInterceptor(BeanFactory beanFactory,
          String transactionManagerName, PlatformTransactionManager transactionManager) {

    TransactionInterceptor ti = new TransactionInterceptor();
    if (beanFactory != null) {
      ti.setBeanFactory(beanFactory);
    }
    if (transactionManagerName != null) {
      ti.setTransactionManagerBeanName(transactionManagerName);

    }
    if (transactionManager != null) {
      ti.setTransactionManager(transactionManager);
    }
    ti.setTransactionAttributeSource(new NameMatchTransactionAttributeSource());
    ti.afterPropertiesSet();
    return ti;
  }

  private TransactionInterceptor transactionInterceptorWithTransactionManager(
          PlatformTransactionManager transactionManager, BeanFactory beanFactory) {

    return createTransactionInterceptor(beanFactory, null, transactionManager);
  }

  private TransactionInterceptor transactionInterceptorWithTransactionManagerName(
          String transactionManagerName, BeanFactory beanFactory) {

    return createTransactionInterceptor(beanFactory, transactionManagerName, null);
  }

  private TransactionInterceptor simpleTransactionInterceptor(BeanFactory beanFactory) {
    return createTransactionInterceptor(beanFactory, null, null);
  }

  private PlatformTransactionManager associateTransactionManager(BeanFactory beanFactory, String name) {
    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    given(beanFactory.containsBean(name)).willReturn(true);
    given(beanFactory.isTypeMatch(name, TransactionManager.class)).willReturn(true);
    given(beanFactory.isTypeMatch(name, PlatformTransactionManager.class)).willReturn(true);
    given(beanFactory.getBean(name, TransactionManager.class)).willReturn(transactionManager);
    given(beanFactory.getBean(name, PlatformTransactionManager.class)).willReturn(transactionManager);
    given(beanFactory.getBeanNamesForType(any(Class.class))).willReturn(new String[0]);
    given(beanFactory.getBeanNamesForType(any(ResolvableType.class))).willReturn(new String[0]);
    return transactionManager;
  }

  /**
   * We won't use this: we just want to know it's serializable.
   */
  @SuppressWarnings("serial")
  public static class SerializableTransactionManager implements PlatformTransactionManager, Serializable {

    @Override
    public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
      throw new UnsupportedOperationException();
    }
  }

}
