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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class GenericApplicationContextTests {

  @Test
  void getBeanForClass() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
    ac.refresh();

    assertThat(ac.getBean("testBean")).isEqualTo("");
    assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
    assertThat(ac.getBean(CharSequence.class)).isSameAs(ac.getBean("testBean"));

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
            ac.getBean(Object.class));
  }

  @Test
  void withSingletonSupplier() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, ac::toString));
    ac.refresh();

    assertThat(ac.getBean("testBean")).isSameAs(ac.getBean("testBean"));
    assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
    assertThat(ac.getBean(CharSequence.class)).isSameAs(ac.getBean("testBean"));
    assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
  }

  @Test
  void withScopedSupplier() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("testBean",
            new RootBeanDefinition(String.class, BeanDefinition.SCOPE_PROTOTYPE, ac::toString));
    ac.refresh();

    assertThat(ac.getBean("testBean")).isNotSameAs(ac.getBean("testBean"));
    assertThat(ac.getBean(String.class)).isEqualTo(ac.getBean("testBean"));
    assertThat(ac.getBean(CharSequence.class)).isEqualTo(ac.getBean("testBean"));
    assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
  }

  @Test
  void accessAfterClosing() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
    ac.refresh();

    assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
    assertThat(ac.getAutowireCapableBeanFactory().getBean(String.class)).isSameAs(ac.getAutowireCapableBeanFactory().getBean("testBean"));

    ac.close();

    assertThatIllegalStateException().isThrownBy(() ->
            ac.getBean(String.class));

    assertThatIllegalStateException().isThrownBy(() -> {
      ac.getAutowireCapableBeanFactory().getBean("testBean");
      ac.getAutowireCapableBeanFactory().getBean(String.class);
    });
  }

  @Test
  void individualBeans() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean(BeanA.class);
    context.registerBean(BeanB.class);
    context.registerBean(BeanC.class);
    context.refresh();

    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeans() {
    GenericApplicationContext context = new GenericApplicationContext();
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
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isTrue();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

    assertThat(context.getBeanFactory().getDependentBeans(BeanB.class.getName())).isEqualTo(new String[] { BeanA.class.getName() });
    assertThat(context.getBeanFactory().getDependentBeans(BeanC.class.getName())).isEqualTo(new String[] { BeanA.class.getName() });
  }

  @Test
  void individualBeanWithSupplierAndCustomizer() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean(BeanA.class,
            () -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
            bd -> bd.setLazyInit(true));
    context.registerBean(BeanB.class, BeanB::new);
    context.registerBean(BeanC.class, BeanC::new);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isFalse();
    assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
    assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
  }

  @Test
  void individualNamedBeanWithSupplier() {
    GenericApplicationContext context = new GenericApplicationContext();
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
    GenericApplicationContext context = new GenericApplicationContext();
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
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("a", BeanA.class, () -> null);
    context.registerBean("b", BeanB.class, BeanB::new);
    context.registerBean("c", BeanC.class, BeanC::new);
    context.refresh();

    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanA.class)), "a")).isTrue();
    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanB.class)), "b")).isTrue();
    assertThat(ObjectUtils.containsElement(StringUtils.toStringArray(context.getBeanNamesForType(BeanC.class)), "c")).isTrue();
    assertThat(context.getBeansOfType(BeanA.class).isEmpty()).isTrue();
    assertThat(context.getBeansOfType(BeanB.class).values().iterator().next()).isSameAs(context.getBean(BeanB.class));
    assertThat(context.getBeansOfType(BeanC.class).values().iterator().next()).isSameAs(context.getBean(BeanC.class));
  }

//
//  @Test
//  void refreshForAotSetsContextActive() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    assertThat(context.isActive()).isFalse();
//    context.refreshForAotProcessing();
//    assertThat(context.isActive()).isTrue();
//  }

//  @Test
//  void refreshForAotRegistersEnvironment() {
//    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.setEnvironment(environment);
//    context.refreshForAotProcessing();
//    assertThat(context.getBean(Environment.class)).isEqualTo(environment);
//  }
//
//  @Test
//  void refreshForAotLoadsBeanClassName() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.registerBeanDefinition("number", new RootBeanDefinition("java.lang.Integer"));
//    context.refreshForAotProcessing();
//    assertThat(getBeanDefinition(context, "number").getBeanClass()).isEqualTo(Integer.class);
//  }
//
//  @Test
//  void refreshForAotLoadsBeanClassNameOfConstructorArgumentInnerBeanDefinition() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
//    GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
//    innerBeanDefinition.setBeanClassName("java.lang.Integer");
//    beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, innerBeanDefinition);
//    context.registerBeanDefinition("test", beanDefinition);
//    context.refreshForAotProcessing();
//    RootBeanDefinition bd = getBeanDefinition(context, "test");
//    GenericBeanDefinition value = (GenericBeanDefinition) bd.getConstructorArgumentValues()
//            .getIndexedArgumentValue(0, GenericBeanDefinition.class).getValue();
//    assertThat(value.hasBeanClass()).isTrue();
//    assertThat(value.getBeanClass()).isEqualTo(Integer.class);
//
//  }
//
//  @Test
//  void refreshForAotLoadsBeanClassNameOfPropertyValueInnerBeanDefinition() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
//    GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
//    innerBeanDefinition.setBeanClassName("java.lang.Integer");
//    beanDefinition.getPropertyValues().add("inner", innerBeanDefinition);
//    context.registerBeanDefinition("test", beanDefinition);
//    context.refreshForAotProcessing();
//    RootBeanDefinition bd = getBeanDefinition(context, "test");
//    GenericBeanDefinition value = (GenericBeanDefinition) bd.getPropertyValues().get("inner");
//    assertThat(value.hasBeanClass()).isTrue();
//    assertThat(value.getBeanClass()).isEqualTo(Integer.class);
//  }
//
//  @Test
//  void refreshForAotInvokesBeanFactoryPostProcessors() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    BeanFactoryPostProcessor bfpp = mock(BeanFactoryPostProcessor.class);
//    context.addBeanFactoryPostProcessor(bfpp);
//    context.refreshForAotProcessing();
//    verify(bfpp).postProcessBeanFactory(context.getBeanFactory());
//  }
//
//  @Test
//  void refreshForAotInvokesMergedBeanDefinitionPostProcessors() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.registerBeanDefinition("test", new RootBeanDefinition(String.class));
//    context.registerBeanDefinition("number", new RootBeanDefinition("java.lang.Integer"));
//    MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
//    context.refreshForAotProcessing();
//    verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), String.class, "test");
//    verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "number"), Integer.class, "number");
//  }
//
//  @Test
//  void refreshForAotInvokesMergedBeanDefinitionPostProcessorsOnConstructorArgument() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanD.class);
//    GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
//    innerBeanDefinition.setBeanClassName("java.lang.Integer");
//    beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, innerBeanDefinition);
//    context.registerBeanDefinition("test", beanDefinition);
//    MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
//    context.refreshForAotProcessing();
//    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
//    verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), BeanD.class, "test");
//    verify(bpp).postProcessMergedBeanDefinition(any(RootBeanDefinition.class), eq(Integer.class), captor.capture());
//    assertThat(captor.getValue()).startsWith("(inner bean)");
//  }
//
//  @Test
//  void refreshForAotInvokesMergedBeanDefinitionPostProcessorsOnPropertyValue() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanD.class);
//    GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
//    innerBeanDefinition.setBeanClassName("java.lang.Integer");
//    beanDefinition.getPropertyValues().add("counter", innerBeanDefinition);
//    context.registerBeanDefinition("test", beanDefinition);
//    MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
//    context.refreshForAotProcessing();
//    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
//    verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), BeanD.class, "test");
//    verify(bpp).postProcessMergedBeanDefinition(any(RootBeanDefinition.class), eq(Integer.class), captor.capture());
//    assertThat(captor.getValue()).startsWith("(inner bean)");
//  }
//
//  @Test
//  void refreshForAotFailsOnAnActiveContext() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.refresh();
//    assertThatIllegalStateException().isThrownBy(context::refreshForAotProcessing)
//            .withMessageContaining("does not support multiple refresh attempts");
//  }
//
//  @Test
//  void refreshForAotDoesNotInitializeFactoryBeansEarly() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.registerBeanDefinition("genericFactoryBean",
//            new RootBeanDefinition(TestAotFactoryBean.class));
//    context.refreshForAotProcessing();
//  }
//
//  @Test
//  void refreshForAotDoesNotInstantiateBean() {
//    GenericApplicationContext context = new GenericApplicationContext();
//    context.registerBeanDefinition("test", BeanDefinitionBuilder.rootBeanDefinition(String.class, () -> {
//      throw new IllegalStateException("Should not be invoked");
//    }).getBeanDefinition());
//    context.refreshForAotProcessing();
//  }
//
//  private MergedBeanDefinitionPostProcessor registerMockMergedBeanDefinitionPostProcessor(GenericApplicationContext context) {
//    MergedBeanDefinitionPostProcessor bpp = mock(MergedBeanDefinitionPostProcessor.class);
//    context.registerBeanDefinition("bpp", BeanDefinitionBuilder.rootBeanDefinition(
//                    MergedBeanDefinitionPostProcessor.class, () -> bpp)
//            .setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
//    return bpp;
//  }
//
//  private RootBeanDefinition getBeanDefinition(GenericApplicationContext context, String name) {
//    return (RootBeanDefinition) context.getBeanFactory().getMergedBeanDefinition(name);
//  }

  static class BeanA {

    BeanB b;
    BeanC c;

    public BeanA(BeanB b, BeanC c) {
      this.b = b;
      this.c = c;
    }
  }

  static class BeanB implements ApplicationContextAware {

    ApplicationContext applicationContext;

    public BeanB() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }
  }

  static class BeanC { }

  static class BeanD {

    private Integer counter;

    BeanD(Integer counter) {
      this.counter = counter;
    }

    public BeanD() {
    }

    public void setCounter(Integer counter) {
      this.counter = counter;
    }

  }

  static class TestAotFactoryBean<T> extends AbstractFactoryBean<T> {

    TestAotFactoryBean() {
      throw new IllegalStateException("FactoryBean should not be instantied early");
    }

    @Override
    public Class<?> getObjectType() {
      return Object.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected T createBeanInstance() throws Exception {
      return (T) new Object();
    }
  }

}
