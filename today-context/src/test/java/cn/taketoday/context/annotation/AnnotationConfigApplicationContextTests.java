/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation6.ComponentForScanning;
import cn.taketoday.context.annotation6.ConfigForScanning;
import cn.taketoday.context.annotation6.Jsr330NamedForScanning;
import cn.taketoday.context.testfixture.context.annotation.CglibConfiguration;
import cn.taketoday.context.testfixture.context.annotation.LambdaBeanConfiguration;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.util.StringUtils.uncapitalize;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/19 17:52
 */
public class AnnotationConfigApplicationContextTests {

  @Test
  void scanAndRefresh() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.scan("cn.taketoday.context.annotation6");
    context.refresh();

    context.getBean(uncapitalize(ConfigForScanning.class.getSimpleName()));
    context.getBean("testBean"); // contributed by ConfigForScanning
    context.getBean(uncapitalize(ComponentForScanning.class.getSimpleName()));
    context.getBean(uncapitalize(Jsr330NamedForScanning.class.getSimpleName()));
    Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
    assertThat(beans).hasSize(1);
  }

  @Test
  void registerAndRefresh() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Config.class, NameConfig.class);
    context.refresh();

    context.getBean("testBean");
    context.getBean("name");
    Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
    assertThat(beans).hasSize(2);
  }

  @Test
  void getBeansWithAnnotation() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Config.class, NameConfig.class, UntypedFactoryBean.class);
    context.refresh();

    context.getBean("testBean");
    context.getBean("name");
    Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
    assertThat(beans).hasSize(2);
  }

  @Test
  void getBeanByType() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    AnnoTestBean testBean = context.getBean(AnnoTestBean.class);
    assertThat(testBean).isNotNull();
    assertThat(testBean.name).isEqualTo("foo");
  }

  @Test
  void getBeanByTypeRaisesNoSuchBeanDefinitionException() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

    // attempt to retrieve a bean that does not exist
    Class<?> targetType = Pattern.class;
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
                    context.getBean(targetType))
            .withMessageContaining(format("No qualifying bean of type '%s'", targetType.getName()));
  }

  @Test
  void getBeanByTypeAmbiguityRaisesException() {
    ApplicationContext context = new AnnotationConfigApplicationContext(TwoTestBeanConfig.class);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
                    context.getBean(AnnoTestBean.class))
            .withMessageContaining("No qualifying bean of type '" + AnnoTestBean.class.getName() + "'")
            .withMessageContaining("tb1")
            .withMessageContaining("tb2");
  }

  /**
   * Tests that Configuration classes are registered according to convention
   *
   * @see cn.taketoday.beans.factory.support.DefaultBeanNameGenerator#generateBeanName
   */
  @Test
  void defaultConfigClassBeanNameIsGeneratedProperly() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

    // attempt to retrieve the instance by its generated bean name
    Config configObject = (Config) context.getBean("annotationConfigApplicationContextTests.Config");
    assertThat(configObject).isNotNull();
  }

  /**
   * Tests that specifying @Configuration(value="foo") results in registering
   * the configuration class with bean name 'foo'.
   */
  @Test
  void explicitConfigClassBeanNameIsRespected() {
    ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithCustomName.class);

    // attempt to retrieve the instance by its specified name
    ConfigWithCustomName configObject = (ConfigWithCustomName) context.getBean("customConfigBeanName");
    assertThat(configObject).isNotNull();
  }

  @Test
  void autowiringIsEnabledByDefault() {
    ApplicationContext context = new AnnotationConfigApplicationContext(AutowiredConfig.class);
    assertThat(context.getBean(AnnoTestBean.class).name).isEqualTo("foo");
  }

  @Test
  void nullReturningBeanPostProcessor() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(AutowiredConfig.class);
    context.getBeanFactory().addBeanPostProcessor(new InitializationBeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return (bean instanceof AnnoTestBean ? null : bean);
      }
    });
    context.getBeanFactory().addBeanPostProcessor(new InitializationBeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        bean.getClass().getName();
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) {
        bean.getClass().getName();
        return bean;
      }
    });
    context.refresh();
  }

  @Test
  void individualBeans() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanA.class, BeanB.class, BeanC.class);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeans() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean("a", BeanA.class);
    context.registerBean("b", BeanB.class);
    context.registerBean("c", BeanC.class);
    context.refresh();

    assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b"));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualBeanWithSupplier() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("annotationConfigApplicationContextTests.BeanA")).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

    assertThat(context.getBeanFactory().getDependentBeans("annotationConfigApplicationContextTests.BeanB"))
            .containsExactly("annotationConfigApplicationContextTests.BeanA");
    assertThat(context.getBeanFactory().getDependentBeans("annotationConfigApplicationContextTests.BeanC"))
            .containsExactly("annotationConfigApplicationContextTests.BeanA");
  }

  @Test
  void individualBeanWithSupplierAndCustomizer() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("annotationConfigApplicationContextTests.BeanA")).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplier() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean("a", BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("a")).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplierAndCustomizer() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean("a", BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("a")).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualBeanWithNullReturningSupplier() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean("a", BeanA.class, () -> null);
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanA.class)), "a")).isTrue();
    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanB.class)), "b")).isTrue();
    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanC.class)), "c")).isTrue();

    assertThat(context.getBeansOfType(BeanA.class)).isEmpty();
    assertThat(context.getBeansOfType(BeanB.class).values().iterator().next()).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBeansOfType(BeanC.class).values().iterator().next()).isSameAs(context.getBean(BeanC.class));

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            context.getBeanFactory().resolveNamedBean(BeanA.class));
    assertThat(context.getBeanFactory().resolveNamedBean(BeanB.class).getBeanInstance()).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBeanFactory().resolveNamedBean(BeanC.class).getBeanInstance()).isSameAs(context.getBean(BeanC.class));
  }

  @Test
  void individualBeanWithSpecifiedConstructorArguments() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    BeanB b = new BeanB();
    BeanC c = new BeanC();
    context.registerBean(BeanA.class, b, c);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(b);
    assertThat(context.getBean(BeanA.class).c).isSameAs(c);
    assertThat(b.applicationContext).isNull();
  }

  @Test
  void individualNamedBeanWithSpecifiedConstructorArguments() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    BeanB b = new BeanB();
    BeanC c = new BeanC();
    context.registerBean("a", BeanA.class, b, c);
    context.refresh();

    assertThat(context.getBean("a", BeanA.class).b).isSameAs(b);
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(c);
    assertThat(b.applicationContext).isNull();
  }

  @Test
  void individualBeanWithMixedConstructorArguments() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    BeanC c = new BeanC();
    context.registerBean(BeanA.class, c);
    context.registerBean(BeanB.class);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(c);
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithMixedConstructorArguments() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    BeanC c = new BeanC();
    context.registerBean("a", BeanA.class, c);
    context.registerBean("b", BeanB.class);
    context.refresh();

    assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(c);
    assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualBeanWithFactoryBeanSupplier() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean("fb", NonInstantiatedFactoryBean.class, NonInstantiatedFactoryBean::new, bd -> bd.setLazyInit(true));
    context.refresh();

    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getType("&fb")).isEqualTo(NonInstantiatedFactoryBean.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(NonInstantiatedFactoryBean.class)).hasSize(1);
  }

  @Test
  void individualBeanWithFactoryBeanSupplierAndTargetType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition();
    bd.setInstanceSupplier(NonInstantiatedFactoryBean::new);
    bd.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class));
    bd.setLazyInit(true);
    context.registerBeanDefinition("fb", bd);
    context.refresh();

    assertThat(context.getType("&fb")).isEqualTo(FactoryBean.class);
    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(NonInstantiatedFactoryBean.class)).isEmpty();
  }

  @Test
  void individualBeanWithFactoryBeanTypeAsTargetType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd1 = new RootBeanDefinition();
    bd1.setBeanClass(GenericHolderFactoryBean.class);
    bd1.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, ResolvableType.forClassWithGenerics(GenericHolder.class, String.class)));
    bd1.setLazyInit(true);
    context.registerBeanDefinition("fb1", bd1);
    RootBeanDefinition bd2 = new RootBeanDefinition();
    bd2.setBeanClass(UntypedFactoryBean.class);
    bd2.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, ResolvableType.forClassWithGenerics(GenericHolder.class, Integer.class)));
    bd2.setLazyInit(true);
    context.registerBeanDefinition("fb2", bd2);
    context.registerBeanDefinition("ip", new RootBeanDefinition(FactoryBeanInjectionPoints.class));
    context.refresh();

    assertThat(context.getType("&fb1")).isEqualTo(GenericHolderFactoryBean.class);
    assertThat(context.getType("fb1")).isEqualTo(GenericHolder.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(2);
    assertThat(context.getBeanNamesForType(GenericHolderFactoryBean.class)).hasSize(1);
    assertThat(context.getBean("ip", FactoryBeanInjectionPoints.class).factoryBean).isSameAs(context.getBean("&fb1"));
    assertThat(context.getBean("ip", FactoryBeanInjectionPoints.class).factoryResult).isSameAs(context.getBean("fb1"));
  }

  @Test
  void individualBeanWithUnresolvedFactoryBeanTypeAsTargetType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd1 = new RootBeanDefinition();
    bd1.setBeanClass(GenericHolderFactoryBean.class);
    bd1.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, ResolvableType.forClassWithGenerics(GenericHolder.class, Object.class)));
    bd1.setLazyInit(true);
    context.registerBeanDefinition("fb1", bd1);
    RootBeanDefinition bd2 = new RootBeanDefinition();
    bd2.setBeanClass(UntypedFactoryBean.class);
    bd2.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, ResolvableType.forClassWithGenerics(GenericHolder.class, Integer.class)));
    bd2.setLazyInit(true);
    context.registerBeanDefinition("fb2", bd2);
    context.registerBeanDefinition("ip", new RootBeanDefinition(FactoryResultInjectionPoint.class));
    context.refresh();

    assertThat(context.getType("&fb1")).isEqualTo(GenericHolderFactoryBean.class);
    assertThat(context.getType("fb1")).isEqualTo(GenericHolder.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(2);
    assertThat(context.getBean("ip", FactoryResultInjectionPoint.class).factoryResult).isSameAs(context.getBean("fb1"));
  }

  @Test
  void individualBeanWithFactoryBeanObjectTypeAsTargetType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd1 = new RootBeanDefinition();
    bd1.setBeanClass(GenericHolderFactoryBean.class);
    bd1.setTargetType(ResolvableType.forClassWithGenerics(GenericHolder.class, String.class));
    context.registerBeanDefinition("fb1", bd1);
    RootBeanDefinition bd2 = new RootBeanDefinition();
    bd2.setBeanClass(UntypedFactoryBean.class);
    bd2.setTargetType(ResolvableType.forClassWithGenerics(GenericHolder.class, Integer.class));
    context.registerBeanDefinition("fb2", bd2);
    context.registerBeanDefinition("ip", new RootBeanDefinition(FactoryResultInjectionPoint.class));
    context.refresh();

    assertThat(context.getType("&fb1")).isEqualTo(GenericHolderFactoryBean.class);
    assertThat(context.getType("fb1")).isEqualTo(GenericHolder.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(2);
    assertThat(context.getBeanNamesForType(GenericHolderFactoryBean.class)).hasSize(1);
    assertThat(context.getBean("ip", FactoryResultInjectionPoint.class).factoryResult).isSameAs(context.getBean("fb1"));
  }

  @Test
  void individualBeanWithFactoryBeanObjectTypeAsTargetTypeAndLazy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition();
    bd.setBeanClass(TypedFactoryBean.class);
    bd.setTargetType(String.class);
    bd.setLazyInit(true);
    context.registerBeanDefinition("fb", bd);
    context.refresh();

    assertThat(context.getType("&fb")).isNull();
    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getBean("&fb")).isInstanceOf(FactoryBean.class);
    assertThat(context.getType("&fb")).isEqualTo(TypedFactoryBean.class);
    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(TypedFactoryBean.class)).hasSize(1);
  }

  @Test
  void refreshForAotProcessingWithConfiguration() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Config.class);
    context.refreshForAotProcessing(new RuntimeHints());
    assertThat(context.getBeanFactory().getBeanDefinitionNames()).contains(
            "annotationConfigApplicationContextTests.Config", "testBean");
  }

  @Test
  void refreshForAotCanInstantiateBeanWithAutowiredApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanD.class);
    context.refreshForAotProcessing(new RuntimeHints());
    BeanD bean = context.getBean(BeanD.class);
    assertThat(bean.applicationContext).isSameAs(context);
  }

  @Test
  void refreshForAotCanInstantiateBeanWithFieldAutowiredApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanB.class);
    context.refreshForAotProcessing(new RuntimeHints());
    BeanB bean = context.getBean(BeanB.class);
    assertThat(bean.applicationContext).isSameAs(context);
  }

  @Test
  void refreshForAotRegisterHintsForCglibProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(CglibConfiguration.class);
    RuntimeHints runtimeHints = new RuntimeHints();
    context.refreshForAotProcessing(runtimeHints);
    TypeReference cglibType = TypeReference.of(CglibConfiguration.class.getName() + "$$Infra$$0");
    assertThat(RuntimeHintsPredicates.reflection().onType(cglibType)
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS))
            .accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(CglibConfiguration.class)
            .withMemberCategories(MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS))
            .accepts(runtimeHints);
  }

  @Test
  void refreshForAotRegisterHintsForTargetOfCglibProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(CglibConfiguration.class);
    RuntimeHints runtimeHints = new RuntimeHints();
    context.refreshForAotProcessing(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of(CglibConfiguration.class))
            .withMemberCategories(MemberCategory.INVOKE_PUBLIC_METHODS))
            .accepts(runtimeHints);
  }

  @Test
  void refreshForAotRegisterDoesNotConsiderLambdaBeanAsCglibProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(LambdaBeanConfiguration.class);
    RuntimeHints runtimeHints = new RuntimeHints();
    context.refreshForAotProcessing(runtimeHints);
    assertThat(runtimeHints.reflection().typeHints()).isEmpty();
  }

  static class GenericHolder<T> { }

  static class GenericHolderFactoryBean implements FactoryBean<GenericHolder<?>> {

    @Override
    public GenericHolder<?> getObject() {
      return new GenericHolder<>();
    }

    @Override
    public Class<?> getObjectType() {
      return GenericHolder.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  static class FactoryResultInjectionPoint {

    @Autowired
    GenericHolder<String> factoryResult;
  }

  static class FactoryBeanInjectionPoints extends FactoryResultInjectionPoint {

    @Autowired
    FactoryBean<GenericHolder<String>> factoryBean;
  }

  @Configuration
  static class Config {

    @Bean
    AnnoTestBean testBean() {
      AnnoTestBean testBean = new AnnoTestBean();
      testBean.name = "foo";
      return testBean;
    }
  }

  @Configuration("customConfigBeanName")
  static class ConfigWithCustomName {

    @Bean
    AnnoTestBean testBean() {
      return new AnnoTestBean();
    }
  }

  @Configuration
  static class TwoTestBeanConfig {

    @Bean
    AnnoTestBean tb1() {
      return new AnnoTestBean();
    }

    @Bean
    AnnoTestBean tb2() {
      return new AnnoTestBean();
    }
  }

  @Configuration
  static class NameConfig {

    @Bean
    String name() { return "foo"; }
  }

  @Configuration
  @Import(NameConfig.class)
  static class AutowiredConfig {

    @Autowired
    String autowiredName;

    @Bean
    AnnoTestBean testBean() {
      AnnoTestBean testBean = new AnnoTestBean();
      testBean.name = autowiredName;
      return testBean;
    }
  }

  static class BeanA {

    BeanB b;
    BeanC c;

    @Autowired
    BeanA(BeanB b, BeanC c) {
      this.b = b;
      this.c = c;
    }
  }

  static class BeanB {

    @Autowired
    ApplicationContext applicationContext;

    public BeanB() {
    }
  }

  static class BeanC { }

  static class BeanD {

    private final ApplicationContext applicationContext;

    public BeanD(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

  }

  static class NonInstantiatedFactoryBean implements FactoryBean<String> {

    NonInstantiatedFactoryBean() {
      throw new IllegalStateException();
    }

    @Override
    public String getObject() {
      return "";
    }

    @Override
    public Class<?> getObjectType() {
      return String.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  static class TypedFactoryBean implements FactoryBean<String> {

    @Override
    public String getObject() {
      return "";
    }

    @Override
    public Class<?> getObjectType() {
      return String.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  static class UntypedFactoryBean implements FactoryBean<Object> {

    @Override
    public Object getObject() {
      return null;
    }

    @Override
    public Class<?> getObjectType() {
      return null;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
  }
}

class AnnoTestBean {

  String name;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AnnoTestBean other = (AnnoTestBean) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    }
    else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

}
