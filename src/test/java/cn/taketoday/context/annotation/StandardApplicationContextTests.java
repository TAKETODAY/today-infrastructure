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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.beans.factory.support.DefaultBeanNamePopulator;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation6.ComponentForScanning;
import cn.taketoday.context.annotation6.ConfigForScanning;
import cn.taketoday.context.annotation6.Jsr330NamedForScanning;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.ObjectUtils;

import static cn.taketoday.util.StringUtils.uncapitalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
@SuppressWarnings("resource")
class StandardApplicationContextTests {

  @Test
  void scanAndRefresh() {
    StandardApplicationContext context = new StandardApplicationContext();
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
    StandardApplicationContext context = new StandardApplicationContext();
    context.register(Config.class, NameConfig.class);
    context.refresh();

    context.getBean("testBean");
    context.getBean("name");
    Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
    assertThat(beans).hasSize(2);
  }

  @Test
  void getBeansWithAnnotation() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.register(Config.class, NameConfig.class, UntypedFactoryBean.class);
    context.refresh();

    context.getBean("testBean");
    context.getBean("name");
    Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
    assertThat(beans).hasSize(2);
  }

  @Test
  void getBeanByType() {
    ApplicationContext context = new StandardApplicationContext(Config.class);
    TestBean testBean = context.getBean(TestBean.class);
    assertThat(testBean).isNotNull();
    assertThat(testBean.name).isEqualTo("foo");
  }

  @Test
  void getBeanByTypeAmbiguityRaisesException() {
    ApplicationContext context = new StandardApplicationContext(TwoTestBeanConfig.class);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
                    context.getBean(TestBean.class))
            .withMessageContaining("No qualifying bean of type '" + TestBean.class.getName() + "'")
            .withMessageContaining("tb1")
            .withMessageContaining("tb2");
  }

  /**
   * Tests that Configuration classes are registered according to convention
   *
   * @see DefaultBeanNamePopulator#populateName
   */
  @Test
  void defaultConfigClassBeanNameIsGeneratedProperly() {
    ApplicationContext context = new StandardApplicationContext(Config.class);

    // attempt to retrieve the instance by its generated bean name
    Config configObject = (Config) context.getBean("config");
    assertThat(configObject).isNotNull();
  }

  /**
   * Tests that specifying @Configuration(value="foo") results in registering
   * the configuration class with bean name 'foo'.
   */
  @Test
  void explicitConfigClassBeanNameIsRespected() {
    ApplicationContext context = new StandardApplicationContext(ConfigWithCustomName.class);

    // attempt to retrieve the instance by its specified name
    ConfigWithCustomName configObject = (ConfigWithCustomName) context.getBean("customConfigBeanName");
    assertThat(configObject).isNotNull();
  }

  @Test
  void autowiringIsEnabledByDefault() {
    ApplicationContext context = new StandardApplicationContext(AutowiredConfig.class);
    assertThat(context.getBean(TestBean.class).name).isEqualTo("foo");
  }

  @Test
  void nullReturningBeanPostProcessor() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.register(AutowiredConfig.class);
    context.getBeanFactory().addBeanPostProcessor(new InitializationBeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return (bean instanceof TestBean ? null : bean);
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
    StandardApplicationContext context = new StandardApplicationContext();
    context.register(BeanA.class, BeanB.class, BeanC.class);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeans() {
    StandardApplicationContext context = new StandardApplicationContext();
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
    StandardApplicationContext context = new StandardApplicationContext();
    context.registerBean(BeanA.class, () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("beanA")).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

    // TODO getDependentBeans
//    assertThat(context.getBeanFactory().getDependentBeans("beanB"))
//            .containsExactly("beanA");
//    assertThat(context.getBeanFactory().getDependentBeans("beanC"))
//            .containsExactly("beanA");
  }

  @Test
  void individualBeanWithSupplierAndCustomizer() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("beanA")).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplier() {
    StandardApplicationContext context = new StandardApplicationContext();
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
    StandardApplicationContext context = new StandardApplicationContext();
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
    StandardApplicationContext context = new StandardApplicationContext();
    context.registerBean("a", BeanA.class, () -> null);
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanA.class).toArray(), "a")).isTrue();
    assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanB.class).toArray(), "b")).isTrue();
    assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanC.class).toArray(), "c")).isTrue();

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
    StandardApplicationContext context = new StandardApplicationContext();
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
    StandardApplicationContext context = new StandardApplicationContext();
    BeanB b = new BeanB();
    BeanC c = new BeanC();
    context.registerBean("a", BeanA.class, b, c);
    context.refresh();

    assertThat(context.getBean("a", BeanA.class).b).isSameAs(b);
    assertThat(context.getBean("a", BeanA.class).c).isSameAs(c);
    assertThat(b.applicationContext).isNull();
  }

  @Test
  void individualBeanWithFactoryBeanSupplier() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.registerBean("fb", NonInstantiatedFactoryBean.class, NonInstantiatedFactoryBean::new, bd -> bd.setLazyInit(true));
    context.refresh();

    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getType("&fb")).isEqualTo(NonInstantiatedFactoryBean.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(NonInstantiatedFactoryBean.class)).hasSize(1);
  }

  @Test
  void individualBeanWithFactoryBeanSupplierAndTargetType() {
    StandardApplicationContext context = new StandardApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition();
    bd.setInstanceSupplier(NonInstantiatedFactoryBean::new);
    bd.setTargetType(ResolvableType.fromClassWithGenerics(FactoryBean.class, String.class));
    bd.setLazyInit(true);
    context.registerBeanDefinition("fb", bd);
    context.refresh();

    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getType("&fb")).isEqualTo(FactoryBean.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(NonInstantiatedFactoryBean.class)).isEmpty();
  }

  @Test
  void individualBeanWithFactoryBeanObjectTypeAsTargetType() {
    StandardApplicationContext context = new StandardApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition();
    bd.setBeanClass(TypedFactoryBean.class);
    bd.setTargetType(String.class);
    context.registerBeanDefinition("fb", bd);
    context.refresh();

    assertThat(context.getType("&fb")).isEqualTo(TypedFactoryBean.class);
    assertThat(context.getType("fb")).isEqualTo(String.class);
    assertThat(context.getBeanNamesForType(FactoryBean.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(TypedFactoryBean.class)).hasSize(1);
  }

  @Test
  void individualBeanWithFactoryBeanObjectTypeAsTargetTypeAndLazy() {
    StandardApplicationContext context = new StandardApplicationContext();
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

  @Configuration
  static class Config {

    @Bean
    TestBean testBean() {
      TestBean testBean = new TestBean();
      testBean.name = "foo";
      return testBean;
    }
  }

  @Configuration("customConfigBeanName")
  static class ConfigWithCustomName {

    @Bean
    TestBean testBean() {
      return new TestBean();
    }
  }

  @Configuration
  static class TwoTestBeanConfig {

    @Bean
    TestBean tb1() {
      return new TestBean();
    }

    @Bean
    TestBean tb2() {
      return new TestBean();
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
    TestBean testBean() {
      TestBean testBean = new TestBean();
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

  static class TestBean {

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
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TestBean other = (TestBean) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      return true;
    }

  }
}
