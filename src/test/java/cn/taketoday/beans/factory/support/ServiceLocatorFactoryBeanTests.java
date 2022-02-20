package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.core.NestedCheckedException;
import cn.taketoday.core.NestedRuntimeException;

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
    bf.registerBeanDefinition("testService", new BeanDefinition(TestService.class));
    BeanDefinition definition = new BeanDefinition(ServiceLocatorFactoryBean.class);
    definition.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class);
    bf.registerBeanDefinition("factory", definition);

    TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
    TestService testService = factory.getTestService();
    assertThat(testService).isNotNull();
  }

  @Test
  public void testErrorOnTooManyOrTooFew() throws Exception {
    bf.registerBeanDefinition("testService", new BeanDefinition(TestService.class));
    bf.registerBeanDefinition("testServiceInstance2", new BeanDefinition(TestService.class));

    BeanDefinition factory = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class);
    bf.registerBeanDefinition("factory", factory);

    BeanDefinition factory2 = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory2.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class);
    bf.registerBeanDefinition("factory2", factory2);

    BeanDefinition factory3 = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory3.addPropertyValue("serviceLocatorInterface", TestService2Locator.class);
    bf.registerBeanDefinition("factory3", factory3);

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .as("more than one matching type")
            .isThrownBy(() -> ((TestServiceLocator) bf.getBean("factory")).getTestService());

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .as("more than one matching type")
            .isThrownBy(() -> ((TestServiceLocator2) bf.getBean("factory2")).getTestService(null));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .as("no matching types")
            .isThrownBy(() -> ((TestService2Locator) bf.getBean("factory3")).getTestService());
  }

  @Test
  public void testErrorOnTooManyOrTooFewWithCustomServiceLocatorException() {
    bf.registerBeanDefinition("testService", new BeanDefinition(TestService.class));
    bf.registerBeanDefinition("testServiceInstance2", new BeanDefinition(TestService.class));

    BeanDefinition factory = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class);
    factory.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException1.class);
    bf.registerBeanDefinition("factory", factory);

    BeanDefinition factory2 = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory2.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class);
    factory2.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException2.class);
    bf.registerBeanDefinition("factory2", factory2);

    BeanDefinition factory3 = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factory3.addPropertyValue("serviceLocatorInterface", TestService2Locator.class);
    factory3.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException3.class);
    bf.registerBeanDefinition("factory3", factory3);

    assertThatExceptionOfType(CustomServiceLocatorException1.class)
            .as("more than one matching type")
            .isThrownBy(() -> ((TestServiceLocator) bf.getBean("factory")).getTestService())
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);

    assertThatExceptionOfType(CustomServiceLocatorException2.class)
            .as("more than one matching type")
            .isThrownBy(() -> ((TestServiceLocator2) bf.getBean("factory2")).getTestService(null))
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);

    assertThatExceptionOfType(CustomServiceLocatorException3.class)
            .as("no matching type")
            .isThrownBy(() -> ((TestService2Locator) bf.getBean("factory3")).getTestService());
  }

  @Test
  public void testStringArgGetter() throws Exception {
    bf.registerBeanDefinition("testService", new BeanDefinition(TestService.class));

    BeanDefinition factoryDef = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factoryDef.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class);

    bf.registerBeanDefinition("factory", factoryDef);

    // test string-arg getter with null id
    TestServiceLocator2 factory = (TestServiceLocator2) bf.getBean("factory");

    @SuppressWarnings("unused")
    TestService testBean = factory.getTestService(null);
    // now test with explicit id
    testBean = factory.getTestService("testService");
    // now verify failure on bad id
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> factory.getTestService("bogusTestService"));
  }

  @Disabled
  @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
  public void testCombinedLocatorInterface() {
    bf.registerBeanDefinition("testService", new BeanDefinition(TestService.class));
    bf.registerAlias("testService", "1");

    BeanDefinition factoryDef = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factoryDef.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);

    bf.registerBeanDefinition("factory", factoryDef);

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

    BeanDefinition factoryDef = new BeanDefinition(ServiceLocatorFactoryBean.class);
    factoryDef.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);

    bf.registerBeanDefinition("testService1", new BeanDefinition(TestService.class));
    bf.registerBeanDefinition("testService2", new BeanDefinition(ExtendedTestService.class));

    BeanDefinition factoryDe = new BeanDefinition(ServiceLocatorFactoryBean.class);

    factoryDe.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);
    factoryDe.addPropertyValue("serviceMappings", "=testService1\n1=testService1\n2=testService2");

    bf.registerBeanDefinition("factory", factoryDe);

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
  public void testRequiresBeanFactoryAndChokesOnAnythingElse() throws Exception {
    BeanFactory beanFactory = mock(BeanFactory.class);
    try {
      ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
      factory.setBeanFactory(beanFactory);
    }
    catch (BeansException ex) {
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
