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

package infra.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.beans.FatalBeanException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.ServiceLocatorFactoryBean;
import infra.core.NestedCheckedException;
import infra.core.NestedRuntimeException;

import static infra.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/6 17:51
 */
class ServiceLocatorFactoryBeanTests {

  private StandardBeanFactory bf;

  @BeforeEach
  public void setUp() {
    bf = new StandardBeanFactory();
  }

  @Test
  public void testNoArgGetter() {
    bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
                    .getBeanDefinition());

    TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
    TestService testService = factory.getTestService();
    assertThat(testService).isNotNull();
  }

  @Test
  public void testErrorOnTooManyOrTooFew() throws Exception {
    bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("testServiceInstance2", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
                    .getBeanDefinition());
    bf.registerBeanDefinition("factory2",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
                    .getBeanDefinition());
    bf.registerBeanDefinition("factory3",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestService2Locator.class)
                    .getBeanDefinition());
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("more than one matching type").isThrownBy(() ->
            ((TestServiceLocator) bf.getBean("factory")).getTestService());
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("more than one matching type").isThrownBy(() ->
            ((TestServiceLocator2) bf.getBean("factory2")).getTestService(null));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("no matching types").isThrownBy(() ->
            ((TestService2Locator) bf.getBean("factory3")).getTestService());
  }

  @Test
  public void testErrorOnTooManyOrTooFewWithCustomServiceLocatorException() {
    bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("testServiceInstance2", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
                    .addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException1.class)
                    .getBeanDefinition());
    bf.registerBeanDefinition("factory2",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
                    .addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException2.class)
                    .getBeanDefinition());
    bf.registerBeanDefinition("factory3",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestService2Locator.class)
                    .addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException3.class)
                    .getBeanDefinition());
    assertThatExceptionOfType(CustomServiceLocatorException1.class).as("more than one matching type").isThrownBy(() ->
                    ((TestServiceLocator) bf.getBean("factory")).getTestService())
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);
    assertThatExceptionOfType(CustomServiceLocatorException2.class).as("more than one matching type").isThrownBy(() ->
                    ((TestServiceLocator2) bf.getBean("factory2")).getTestService(null))
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);
    assertThatExceptionOfType(CustomServiceLocatorException3.class).as("no matching type").isThrownBy(() ->
            ((TestService2Locator) bf.getBean("factory3")).getTestService());
  }

  @Test
  public void testStringArgGetter() throws Exception {
    bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
                    .getBeanDefinition());

    // test string-arg getter with null id
    TestServiceLocator2 factory = (TestServiceLocator2) bf.getBean("factory");

    @SuppressWarnings("unused")
    TestService testBean = factory.getTestService(null);
    // now test with explicit id
    testBean = factory.getTestService("testService");
    // now verify failure on bad id
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            factory.getTestService("bogusTestService"));
  }

  @Disabled
  @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
  public void testCombinedLocatorInterface() {
    bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerAlias("testService", "1");

    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class)
                    .getBeanDefinition());

//		StaticApplicationContext ctx = new StaticApplicationContext();
//		ctx.registerPrototype("testService", TestService.class, new PropertyValues());
//		ctx.registerAlias("testService", "1");
//		PropertyValues mpv = new PropertyValues();
//		mpv.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);
//		ctx.registerSingleton("factory", ServiceLocatorFactoryBean.class, mpv);
//		ctx.refresh();

    TestServiceLocator3 factory = (TestServiceLocator3) bf.getBean("factory");
    TestService testBean1 = factory.getTestService();
    TestService testBean2 = factory.getTestService("testService");
    TestService testBean3 = factory.getTestService(1);
    TestService testBean4 = factory.someFactoryMethod();
    assertThat(testBean2).isNotSameAs(testBean1);
    assertThat(testBean3).isNotSameAs(testBean1);
    assertThat(testBean4).isNotSameAs(testBean1);
    assertThat(testBean3).isNotSameAs(testBean2);
    assertThat(testBean4).isNotSameAs(testBean2);
    assertThat(testBean4).isNotSameAs(testBean3);

    assertThat(factory.toString().contains("TestServiceLocator3")).isTrue();
  }

  @Disabled
  @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
  public void testServiceMappings() {
    bf.registerBeanDefinition("testService1", genericBeanDefinition(TestService.class).getBeanDefinition());
    bf.registerBeanDefinition("testService2", genericBeanDefinition(ExtendedTestService.class).getBeanDefinition());
    bf.registerBeanDefinition("factory",
            genericBeanDefinition(ServiceLocatorFactoryBean.class)
                    .addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class)
                    .addPropertyValue("serviceMappings", "=testService1\n1=testService1\n2=testService2")
                    .getBeanDefinition());

//		StaticApplicationContext ctx = new StaticApplicationContext();
//		ctx.registerPrototype("testService1", TestService.class, new PropertyValues());
//		ctx.registerPrototype("testService2", ExtendedTestService.class, new PropertyValues());
//		PropertyValues mpv = new PropertyValues();
//		mpv.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);
//		mpv.addPropertyValue("serviceMappings", "=testService1\n1=testService1\n2=testService2");
//		ctx.registerSingleton("factory", ServiceLocatorFactoryBean.class, mpv);
//		ctx.refresh();

    TestServiceLocator3 factory = (TestServiceLocator3) bf.getBean("factory");
    TestService testBean1 = factory.getTestService();
    TestService testBean2 = factory.getTestService("testService1");
    TestService testBean3 = factory.getTestService(1);
    TestService testBean4 = factory.getTestService(2);
    assertThat(testBean2).isNotSameAs(testBean1);
    assertThat(testBean3).isNotSameAs(testBean1);
    assertThat(testBean4).isNotSameAs(testBean1);
    assertThat(testBean3).isNotSameAs(testBean2);
    assertThat(testBean4).isNotSameAs(testBean2);
    assertThat(testBean4).isNotSameAs(testBean3);
    boolean condition3 = testBean1 instanceof ExtendedTestService;
    assertThat(condition3).isFalse();
    boolean condition2 = testBean2 instanceof ExtendedTestService;
    assertThat(condition2).isFalse();
    boolean condition1 = testBean3 instanceof ExtendedTestService;
    assertThat(condition1).isFalse();
    boolean condition = testBean4 instanceof ExtendedTestService;
    assertThat(condition).isTrue();
  }

  @Test
  public void testNoServiceLocatorInterfaceSupplied() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(
            new ServiceLocatorFactoryBean()::afterPropertiesSet);
  }

  @Test
  public void testWhenServiceLocatorInterfaceIsNotAnInterfaceType() throws Exception {
    ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
    factory.setServiceLocatorInterface(getClass());
    assertThatIllegalArgumentException().isThrownBy(
            factory::afterPropertiesSet);
    // should throw, bad (non-interface-type) serviceLocator interface supplied
  }

  @Test
  public void testWhenServiceLocatorExceptionClassToExceptionTypeWithOnlyNoArgCtor() throws Exception {
    ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(() ->
            factory.setServiceLocatorExceptionClass(ExceptionClassWithOnlyZeroArgCtor.class));
    // should throw, bad (invalid-Exception-type) serviceLocatorException class supplied
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testWhenServiceLocatorExceptionClassIsNotAnExceptionSubclass() throws Exception {
    ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(() ->
            factory.setServiceLocatorExceptionClass((Class) getClass()));
    // should throw, bad (non-Exception-type) serviceLocatorException class supplied
  }

  @Test
  public void testWhenServiceLocatorMethodCalledWithTooManyParameters() throws Exception {
    ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
    factory.setServiceLocatorInterface(ServiceLocatorInterfaceWithExtraNonCompliantMethod.class);
    factory.afterPropertiesSet();
    ServiceLocatorInterfaceWithExtraNonCompliantMethod locator = (ServiceLocatorInterfaceWithExtraNonCompliantMethod) factory.getObject();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            locator.getTestService("not", "allowed")); //bad method (too many args, doesn't obey class contract)
  }

  @Test
  public void testRequiresListableBeanFactoryAndChokesOnAnythingElse() throws Exception {
    BeanFactory beanFactory = mock(BeanFactory.class);
    try {
      ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
      factory.setBeanFactory(beanFactory);
    }
    catch (FatalBeanException ex) {
      // expected
    }
  }

  public static class TestService {

  }

  public static class ExtendedTestService extends TestService {

  }

  public static class TestService2 {

  }

  public static interface TestServiceLocator {

    TestService getTestService();
  }

  public static interface TestServiceLocator2 {

    TestService getTestService(String id) throws CustomServiceLocatorException2;
  }

  public static interface TestServiceLocator3 {

    TestService getTestService();

    TestService getTestService(String id);

    TestService getTestService(int id);

    TestService someFactoryMethod();
  }

  public static interface TestService2Locator {

    TestService2 getTestService() throws CustomServiceLocatorException3;
  }

  public static interface ServiceLocatorInterfaceWithExtraNonCompliantMethod {

    TestService2 getTestService();

    TestService2 getTestService(String serviceName, String defaultNotAllowedParameter);
  }

  @SuppressWarnings("serial")
  public static class CustomServiceLocatorException1 extends NestedRuntimeException {

    public CustomServiceLocatorException1(String message, Throwable cause) {
      super(message, cause);
    }
  }

  @SuppressWarnings("serial")
  public static class CustomServiceLocatorException2 extends NestedCheckedException {

    public CustomServiceLocatorException2(Throwable cause) {
      super("", cause);
    }
  }

  @SuppressWarnings("serial")
  public static class CustomServiceLocatorException3 extends NestedCheckedException {

    public CustomServiceLocatorException3(String message) {
      super(message);
    }
  }

  @SuppressWarnings("serial")
  public static class ExceptionClassWithOnlyZeroArgCtor extends Exception {

  }

}
