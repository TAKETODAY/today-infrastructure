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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.annotation.Value;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.config.ListFactoryBean;
import infra.beans.factory.config.PropertyPlaceholderConfigurer;
import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.BeanDefinitionOverrideException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.NestedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.ApplicationListener;
import infra.context.BootstrapContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationClassPostProcessor;
import infra.context.annotation.Scope;
import infra.context.event.ContextClosedEvent;
import infra.context.event.ContextRefreshedEvent;
import infra.context.support.GenericApplicationContext;
import jakarta.annotation.Resource;
import jakarta.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Miscellaneous system tests covering {@link Bean} naming, aliases, scoping and
 * error handling within {@link Configuration} class definitions.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class ConfigurationClassProcessingTests {

  @Test
  void customBeanNameIsRespectedWhenConfiguredViaNameAttribute() {
    customBeanNameIsRespected(ConfigWithBeanWithCustomName.class,
            () -> ConfigWithBeanWithCustomName.testBean, "customName");
  }

  @Test
  void customBeanNameIsRespectedWhenConfiguredViaValueAttribute() {
    customBeanNameIsRespected(ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.class,
            () -> ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.testBean, "enigma");
  }

  private void customBeanNameIsRespected(Class<?> testClass, Supplier<TestBean> testBeanSupplier, String beanName) {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
    ac.registerBeanDefinition("config", new RootBeanDefinition(testClass));
    ac.refresh();

    assertThat(ac.getBean(beanName)).isSameAs(testBeanSupplier.get());

    // method name should not be registered
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            ac.getBean("methodName"));
  }

  @Test
  void aliasesAreRespectedWhenConfiguredViaNameAttribute() {
    aliasesAreRespected(ConfigWithBeanWithAliases.class,
            () -> ConfigWithBeanWithAliases.testBean, "name1");
  }

  @Test
  void aliasesAreRespectedWhenConfiguredViaValueAttribute() {
    aliasesAreRespected(ConfigWithBeanWithAliasesConfiguredViaValueAttribute.class,
            () -> ConfigWithBeanWithAliasesConfiguredViaValueAttribute.testBean, "enigma");
  }

  private void aliasesAreRespected(Class<?> testClass, Supplier<TestBean> testBeanSupplier, String beanName) {
    TestBean testBean = testBeanSupplier.get();
    BeanFactory factory = initBeanFactory(false, testClass);

    assertThat(factory.getBean(beanName)).isSameAs(testBean);
    Arrays.stream(factory.getAliases(beanName)).map(factory::getBean).forEach(alias -> assertThat(alias).isSameAs(testBean));

    // method name should not be registered
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            factory.getBean("methodName"));
  }

  @Test
    // SPR-11830
  void configWithBeanWithProviderImplementation() {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
    ac.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithBeanWithProviderImplementation.class));
    ac.refresh();
    assertThat(ConfigWithBeanWithProviderImplementation.testBean).isSameAs(ac.getBean("customName"));
  }

  @Test
  void configWithSetWithProviderImplementation() {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
    ac.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithSetWithProviderImplementation.class));
    ac.refresh();
    assertThat(ConfigWithSetWithProviderImplementation.set).isSameAs(ac.getBean("customName"));
  }

  @Test
  void finalBeanMethod() {
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
            initBeanFactory(false, ConfigWithFinalBean.class));
  }

  @Test
  void finalBeanMethodWithoutProxy() {
    initBeanFactory(false, ConfigWithFinalBeanWithoutProxy.class);
  }

  @Test
  void voidBeanMethod() {
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
            initBeanFactory(false, ConfigWithVoidBean.class));
  }

  @Test
  void simplestPossibleConfig() {
    BeanFactory factory = initBeanFactory(false, SimplestPossibleConfig.class);
    String stringBean = factory.getBean("stringBean", String.class);
    assertThat(stringBean).isEqualTo("foo");
  }

  @Test
  void configWithObjectReturnType() {
    BeanFactory factory = initBeanFactory(false, ConfigWithNonSpecificReturnTypes.class);
    assertThat(factory.getType("stringBean")).isEqualTo(Object.class);
    assertThat(factory.isTypeMatch("stringBean", String.class)).isFalse();
    String stringBean = factory.getBean("stringBean", String.class);
    assertThat(stringBean).isEqualTo("foo");
  }

  @Test
  void configWithFactoryBeanReturnType() {
    StandardBeanFactory factory = initBeanFactory(false, ConfigWithNonSpecificReturnTypes.class);
    assertThat(factory.getType("factoryBean")).isEqualTo(List.class);
    assertThat(factory.isTypeMatch("factoryBean", List.class)).isTrue();
    assertThat(factory.getType("&factoryBean")).isEqualTo(FactoryBean.class);
    assertThat(factory.isTypeMatch("&factoryBean", FactoryBean.class)).isTrue();
    assertThat(factory.isTypeMatch("&factoryBean", BeanClassLoaderAware.class)).isFalse();
    assertThat(factory.isTypeMatch("&factoryBean", ListFactoryBean.class)).isFalse();
    boolean condition = factory.getBean("factoryBean") instanceof List;
    assertThat(condition).isTrue();

    var beanNames = factory.getBeanNamesForType(FactoryBean.class);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("&factoryBean");

    beanNames = factory.getBeanNamesForType(BeanClassLoaderAware.class);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("&factoryBean");

    beanNames = factory.getBeanNamesForType(ListFactoryBean.class);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("&factoryBean");

    beanNames = factory.getBeanNamesForType(List.class);
    assertThat(beanNames[0]).isEqualTo("factoryBean");
  }

  @Test
  void configurationWithPrototypeScopedBeans() {
    BeanFactory factory = initBeanFactory(false, ConfigWithPrototypeBean.class);

    TestBean foo = factory.getBean("foo", TestBean.class);
    ITestBean bar = factory.getBean("bar", ITestBean.class);
    ITestBean baz = factory.getBean("baz", ITestBean.class);

    assertThat(bar).isSameAs(foo.getSpouse());
    assertThat(baz).isNotSameAs(bar.getSpouse());
  }

  @Test
  void configurationWithNullReference() {
    BeanFactory factory = initBeanFactory(false, ConfigWithNullReference.class);

    TestBean foo = factory.getBean("foo", TestBean.class);
    assertThat(factory.getBean("bar")).isEqualTo(null);
    assertThat(foo.getSpouse()).isNull();
  }

  @Test
  void configurationWithMethodNameMismatch() {
    assertThatExceptionOfType(BeanDefinitionOverrideException.class)
            .isThrownBy(() -> initBeanFactory(false, ConfigWithMethodNameMismatch.class));
  }

  @Test
  void configurationWithMethodNameMismatchAndOverridingAllowed() {
    BeanFactory factory = initBeanFactory(true, ConfigWithMethodNameMismatch.class);

    SpousyTestBean foo = factory.getBean("foo", SpousyTestBean.class);
    assertThat(foo.getName()).isEqualTo("foo1");
  }

  @Test
  void configurationWithAdaptivePrototypes() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithPrototypeBean.class, AdaptiveInjectionPoints.class);
    ctx.refresh();

    AdaptiveInjectionPoints adaptive = ctx.getBean(AdaptiveInjectionPoints.class);
    assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
    assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");

    adaptive = ctx.getBean(AdaptiveInjectionPoints.class);
    assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
    assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");
    ctx.close();
  }

  @Test
  void configurationWithAdaptiveResourcePrototypes() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithPrototypeBean.class, AdaptiveResourceInjectionPoints.class);
    ctx.refresh();

    AdaptiveResourceInjectionPoints adaptive = ctx.getBean(AdaptiveResourceInjectionPoints.class);
    assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
    assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");

    adaptive = ctx.getBean(AdaptiveResourceInjectionPoints.class);
    assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
    assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");
    ctx.close();
  }

  @Test
  void configurationWithPostProcessor() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithPostProcessor.class);
    RootBeanDefinition placeholderConfigurer = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
    placeholderConfigurer.getPropertyValues().add("properties", "myProp=myValue");
    ctx.registerBeanDefinition("placeholderConfigurer", placeholderConfigurer);
    ctx.refresh();

    TestBean foo = ctx.getBean("foo", TestBean.class);
    ITestBean bar = ctx.getBean("bar", ITestBean.class);
    ITestBean baz = ctx.getBean("baz", ITestBean.class);

    assertThat(foo.getName()).isEqualTo("foo-processed-myValue");
    assertThat(bar.getName()).isEqualTo("bar-processed-myValue");
    assertThat(baz.getName()).isEqualTo("baz-processed-myValue");

    SpousyTestBean listener = ctx.getBean("listenerTestBean", SpousyTestBean.class);
    assertThat(listener.refreshed).isTrue();
    ctx.close();
  }

  @Test
  void configurationWithFunctionalRegistration() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithFunctionalRegistration.class);
    ctx.refresh();

    assertThat(ctx.getBean(TestBean.class).getSpouse()).isSameAs(ctx.getBean("spouse"));
    assertThat(ctx.getBean(NestedTestBean.class).getCompany()).isEqualTo("functional");
    ctx.close();
  }

  @Test
  void configurationWithApplicationListener() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithApplicationListener.class);
    ctx.refresh();

    ConfigWithApplicationListener config = ctx.getBean(ConfigWithApplicationListener.class);
    assertThat(config.closed).isFalse();
    ctx.close();
    assertThat(config.closed).isTrue();
  }

  @Test
  void configurationWithOverloadedBeanMismatch() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(OverloadedBeanMismatch.class));
    ctx.refresh();

    TestBean tb = ctx.getBean(TestBean.class);
    assertThat(tb.getLawyer()).isEqualTo(ctx.getBean(NestedTestBean.class));
    ctx.close();
  }

  @Test
  void configurationWithOverloadedBeanMismatchWithAsm() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(OverloadedBeanMismatch.class.getName()));
    ctx.refresh();

    TestBean tb = ctx.getBean(TestBean.class);
    assertThat(tb.getLawyer()).isEqualTo(ctx.getBean(NestedTestBean.class));
    ctx.close();
  }

  @Test
  void autowiringWithDynamicPrototypeBeanClass() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            ConfigWithDynamicPrototype.class, PrototypeDependency.class);

    PrototypeInterface p1 = ctx.getBean(PrototypeInterface.class, 1);
    assertThat(p1).isInstanceOf(PrototypeOne.class);
    assertThat(((PrototypeOne) p1).prototypeDependency).isNotNull();

    PrototypeInterface p2 = ctx.getBean(PrototypeInterface.class, 2);
    assertThat(p2).isInstanceOf(PrototypeTwo.class);

    PrototypeInterface p3 = ctx.getBean(PrototypeInterface.class, 1);
    assertThat(p3).isInstanceOf(PrototypeOne.class);
    assertThat(((PrototypeOne) p3).prototypeDependency).isNotNull();
    ctx.close();
  }

  /**
   * Creates a new {@link BeanFactory}, populates it with a {@link BeanDefinition}
   * for each of the given {@link Configuration} {@code configClasses}, and then
   * post-processes the factory using JavaConfig's {@link ConfigurationClassPostProcessor}.
   * When complete, the factory is ready to service requests for any {@link Bean} methods
   * declared by {@code configClasses}.
   */
  private StandardBeanFactory initBeanFactory(boolean allowOverriding, Class<?>... configClasses) {
    GenericApplicationContext ac = new GenericApplicationContext();
    StandardBeanFactory factory = new StandardBeanFactory();
    for (Class<?> configClass : configClasses) {
      String configBeanName = configClass.getName();
      factory.registerBeanDefinition(configBeanName, new RootBeanDefinition(configClass));
    }
    factory.setAllowBeanDefinitionOverriding(allowOverriding);
    ConfigurationClassPostProcessor ccpp = new ConfigurationClassPostProcessor(
            new BootstrapContext(factory, ac));
    ccpp.postProcessBeanDefinitionRegistry(factory);
    ccpp.postProcessBeanFactory(factory);
    factory.freezeConfiguration();
    return factory;
  }

  @Configuration
  static class ConfigWithBeanWithCustomName {

    static TestBean testBean = new TestBean(ConfigWithBeanWithCustomName.class.getSimpleName());

    @Bean(name = "customName")
    public TestBean methodName() {
      return testBean;
    }
  }

  @Configuration
  static class ConfigWithBeanWithCustomNameConfiguredViaValueAttribute {

    static TestBean testBean = new TestBean(ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.class.getSimpleName());

    @Bean("enigma")
    public TestBean methodName() {
      return testBean;
    }
  }

  @Configuration
  static class ConfigWithBeanWithAliases {

    static TestBean testBean = new TestBean(ConfigWithBeanWithAliases.class.getSimpleName());

    @Bean(name = { "name1", "alias1", "alias2", "alias3" })
    public TestBean methodName() {
      return testBean;
    }
  }

  @Configuration
  static class ConfigWithBeanWithAliasesConfiguredViaValueAttribute {

    static TestBean testBean = new TestBean(ConfigWithBeanWithAliasesConfiguredViaValueAttribute.class.getSimpleName());

    @Bean({ "enigma", "alias1", "alias2", "alias3" })
    public TestBean methodName() {
      return testBean;
    }
  }

  @Configuration
  static class ConfigWithBeanWithProviderImplementation implements Provider<TestBean> {

    static TestBean testBean = new TestBean(ConfigWithBeanWithProviderImplementation.class.getSimpleName());

    @Override
    @Bean(name = "customName")
    public TestBean get() {
      return testBean;
    }
  }

  @Configuration
  static class ConfigWithSetWithProviderImplementation implements Provider<Set<String>> {

    static Set<String> set = Collections.singleton("value");

    @Override
    @Bean(name = "customName")
    public Set<String> get() {
      return set;
    }
  }

  @Configuration
  static class ConfigWithFinalBean {

    @Bean
    public final TestBean testBean() {
      return new TestBean();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class ConfigWithFinalBeanWithoutProxy {

    @Bean
    public final TestBean testBean() {
      return new TestBean();
    }
  }

  @Configuration
  static class ConfigWithVoidBean {

    @Bean
    public void testBean() {
    }
  }

  @Configuration
  static class SimplestPossibleConfig {

    @Bean
    public String stringBean() {
      return "foo";
    }
  }

  @Configuration
  static class ConfigWithNonSpecificReturnTypes {

    @Bean
    public Object stringBean() {
      return "foo";
    }

    @Bean
    public FactoryBean<?> factoryBean() {
      ListFactoryBean fb = new ListFactoryBean();
      fb.setSourceList(Arrays.asList("element1", "element2"));
      return fb;
    }
  }

  @Configuration
  static class ConfigWithPrototypeBean {

    @Bean
    public TestBean foo() {
      TestBean foo = new SpousyTestBean("foo");
      foo.setSpouse(bar());
      return foo;
    }

    @Bean
    public TestBean bar() {
      TestBean bar = new SpousyTestBean("bar");
      bar.setSpouse(baz());
      return bar;
    }

    @Bean
    @Scope("prototype")
    public TestBean baz() {
      return new TestBean("baz");
    }

    @Bean
    @Scope("prototype")
    public TestBean adaptive1(InjectionPoint ip) {
      return new TestBean(ip.getMember().getName());
    }

    @Bean
    @Scope("prototype")
    public TestBean adaptive2(DependencyDescriptor dd) {
      return new TestBean(dd.getMember().getName());
    }
  }

  @Configuration
  static class ConfigWithNullReference extends ConfigWithPrototypeBean {

    @Override
    public TestBean bar() {
      return null;
    }
  }

  @Configuration(enforceUniqueMethods = false)
  static class ConfigWithMethodNameMismatch {

    @Bean(name = "foo")
    public TestBean foo1() {
      return new SpousyTestBean("foo1");
    }

    @Bean(name = "foo")
    public TestBean foo2() {
      return new SpousyTestBean("foo2");
    }
  }

  @Scope("prototype")
  static class AdaptiveInjectionPoints {

    @Autowired
    @Qualifier("adaptive1")
    public TestBean adaptiveInjectionPoint1;

    public TestBean adaptiveInjectionPoint2;

    @Autowired
    @Qualifier("adaptive2")
    public void setAdaptiveInjectionPoint2(TestBean adaptiveInjectionPoint2) {
      this.adaptiveInjectionPoint2 = adaptiveInjectionPoint2;
    }
  }

  @Scope("prototype")
  static class AdaptiveResourceInjectionPoints {

    @Resource(name = "adaptive1")
    public TestBean adaptiveInjectionPoint1;

    public TestBean adaptiveInjectionPoint2;

    @Resource(name = "adaptive2")
    public void setAdaptiveInjectionPoint2(TestBean adaptiveInjectionPoint2) {
      this.adaptiveInjectionPoint2 = adaptiveInjectionPoint2;
    }
  }

  static class ConfigWithPostProcessor extends ConfigWithPrototypeBean {

    @Value("${myProp}")
    private String myProp;

    @Bean
    public POBPP beanPostProcessor() {
      return new POBPP() {

        String nameSuffix = "-processed-" + myProp;

        @SuppressWarnings("unused")
        public void setNameSuffix(String nameSuffix) {
          this.nameSuffix = nameSuffix;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
          if (bean instanceof ITestBean) {
            ((ITestBean) bean).setName(((ITestBean) bean).getName() + nameSuffix);
          }
          return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          return bean;
        }
      };
    }

    //    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
      return beanFactory -> {
        BeanDefinition bd = beanFactory.getBeanDefinition("beanPostProcessor");
        bd.getPropertyValues().add("nameSuffix", "-processed-" + myProp);
      };
    }

    @Bean
    public ITestBean listenerTestBean() {
      return new SpousyTestBean("listener");
    }
  }

  public interface POBPP extends InitializationBeanPostProcessor { }

  private static class SpousyTestBean extends TestBean implements ApplicationListener<ContextRefreshedEvent> {

    public boolean refreshed = false;

    public SpousyTestBean(String name) {
      super(name);
    }

    @Override
    public void setSpouse(ITestBean spouse) {
      super.setSpouse(spouse);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
      this.refreshed = true;
    }
  }

  @Configuration
  static class ConfigWithFunctionalRegistration {

    @Autowired
    void register(GenericApplicationContext ctx) {
      ctx.registerBean("spouse", TestBean.class,
              () -> new TestBean("functional"));
      Supplier<TestBean> testBeanSupplier =
              () -> new TestBean(ctx.getBean("spouse", TestBean.class));
      ctx.registerBean(TestBean.class, testBeanSupplier,
              bd -> bd.setPrimary(true));
    }

    @Bean
    public NestedTestBean nestedTestBean(TestBean spouse) {
      return new NestedTestBean(spouse.getSpouse().getName());
    }
  }

  @Configuration
  static class ConfigWithApplicationListener {

    boolean closed = false;

    @Bean
    public ApplicationListener<ContextClosedEvent> listener() {
      return (event -> this.closed = true);
    }
  }

  @Configuration(enforceUniqueMethods = false)
  public static class OverloadedBeanMismatch {

    @Bean(name = "other")
    public NestedTestBean foo() {
      return new NestedTestBean();
    }

    @Bean(name = "foo")
    public TestBean foo(@Qualifier("other") NestedTestBean other) {
      TestBean tb = new TestBean();
      tb.setLawyer(other);
      return tb;
    }
  }

  static class PrototypeDependency {
  }

  interface PrototypeInterface {
  }

  static class PrototypeOne extends AbstractPrototype {

    @Autowired
    PrototypeDependency prototypeDependency;

  }

  static class PrototypeTwo extends AbstractPrototype {
    // no autowired dependency here, in contrast to above
  }

  static class AbstractPrototype implements PrototypeInterface {
  }

  @Configuration
  static class ConfigWithDynamicPrototype {

    @Bean
    @Scope(value = "prototype")
    public PrototypeInterface getDemoBean(int i) {
      return switch (i) {
        case 1 -> new PrototypeOne();
        default -> new PrototypeTwo();
      };
    }
  }

}
