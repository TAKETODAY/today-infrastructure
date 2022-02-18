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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.StandardDependenciesBeanPostProcessor;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.lang.Component;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY
 * 2020/9/17 16:16
 */
class AutowireCapableBeanFactoryTests {

  static class CreateTestBean {
    int property;
  }

  @Test
  void testCreateBean() {
    try (StandardApplicationContext context = new StandardApplicationContext()) {
      AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();

      CreateTestBean bean = beanFactory.createBean(CreateTestBean.class);
      CreateTestBean bean2 = beanFactory.createBean(CreateTestBean.class);
      assertThat(bean.property).isZero();
      assertThat(bean2).isNotEqualTo(bean);

      CreateTestBean cachedBeanDef = beanFactory.createBean(CreateTestBean.class, true);
      CreateTestBean cachedBeanDef2 = beanFactory.createBean(CreateTestBean.class, true);

      assertThat(cachedBeanDef.property).isZero();
      assertThat(cachedBeanDef2).isNotEqualTo(cachedBeanDef);

      BeanDefinition beanDefinition = context.getBeanDefinition(CreateTestBean.class);

      assertThat(beanDefinition.getBeanClass()).isEqualTo(CreateTestBean.class);
      assertThat(beanDefinition.getScope()).isEqualTo(Scope.PROTOTYPE);
      assertThat(beanDefinition.getPropertyValues()).isNull();
    }
  }

  // 防止外部的扫描到
  static class AutowireTestBeanCondition implements Condition {
    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

  }

  @Conditional(AutowireTestBeanCondition.class)
  @Component(initMethods = "init", destroyMethod = "destroy")
  static class AutowireTestBean implements BeanNameAware, InitializingBean {
    @Value("#{1+1}")
    int property;

    @Autowired(required = false)
    CreateTestBean bean;

    boolean initMethod;
    boolean destroyMethods;
    boolean postConstruct;
    boolean afterPropertiesSet;
    String name;

    boolean afterPostProcessor;
    boolean beforePostProcessor;
    boolean postProcessBeforeDestruction;

    void init() {
      initMethod = true;
    }

    void destroy() {
      destroyMethods = true;
    }

    @PostConstruct
    void postConstruct() {
      postConstruct = true;
    }

    @Override
    public void setBeanName(final String name) {
      this.name = name;
    }

    @Override
    public void afterPropertiesSet() {
      afterPropertiesSet = true;
    }
  }

  @Test
  void testAutowireBean() {
    try (StandardApplicationContext context = new StandardApplicationContext()) {

      StandardBeanFactory beanFactory = context.getBeanFactory();

      CreateTestBean cachedBeanDef = beanFactory.createBean(CreateTestBean.class, true);
      beanFactory.addBeanPostProcessor(new PostProcessor());
      context.refresh();

      AutowireTestBean autowireTestBean = new AutowireTestBean();
      beanFactory.autowireBean(autowireTestBean);

      assertThat(autowireTestBean.name).isNull();
      assertThat(autowireTestBean.property).isEqualTo(2);
      assertThat(autowireTestBean.initMethod).isFalse();
      assertThat(autowireTestBean.postConstruct).isFalse();
      assertThat(autowireTestBean.afterPropertiesSet).isFalse();
      assertThat(autowireTestBean.afterPostProcessor).isFalse();
      assertThat(autowireTestBean.beforePostProcessor).isFalse();
      assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
    }
  }

  static class PostProcessor implements InitializationBeanPostProcessor, DestructionBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      if (bean instanceof AutowireTestBean) {
        ((AutowireTestBean) bean).beforePostProcessor = true;
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      if (bean instanceof AutowireTestBean) {
        ((AutowireTestBean) bean).afterPostProcessor = true;
      }
      return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) {
      if (bean instanceof AutowireTestBean) {
        ((AutowireTestBean) bean).postProcessBeforeDestruction = true;
      }
    }

  }

  @Test
  void testInitializeBean() {
    String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.refresh();
      StandardBeanFactory beanFactory = context.getBeanFactory();

      CreateTestBean cachedBeanDef = beanFactory.createBean(CreateTestBean.class, true);

      beanFactory.addBeanPostProcessor(new PostProcessor());

      AutowireTestBean autowireTestBean = new AutowireTestBean();
      beanFactory.initializeBean(autowireTestBean, beanName); // no bean definition

      assertThat(autowireTestBean.name).isEqualTo(beanName);

      assertThat(autowireTestBean.initMethod).isFalse();
      assertThat(autowireTestBean.postConstruct).isTrue();
      assertThat(autowireTestBean.afterPropertiesSet).isTrue();
      assertThat(autowireTestBean.afterPostProcessor).isTrue();
      assertThat(autowireTestBean.beforePostProcessor).isTrue();
      assertThat(autowireTestBean.property).isEqualTo(0);
      assertThat(autowireTestBean.bean).isNull();

      // autowireBean
      beanFactory.autowireBean(autowireTestBean);
      assertThat(autowireTestBean.property).isEqualTo(2);
      assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
    }
  }

  @Test
  void testInitializeBeanWithBeanDefinition() {
    String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.refresh();
      StandardBeanFactory beanFactory = context.getBeanFactory();

      CreateTestBean cachedBeanDef = beanFactory.createBean(CreateTestBean.class, true);

      beanFactory.addBeanPostProcessor(new PostProcessor());

      BeanDefinition defaults = BeanDefinitionBuilder.defaults(beanName, AutowireTestBean.class);

      AutowireTestBean autowireTestBean = new AutowireTestBean();
      defaults.setInitMethods("init");
      beanFactory.initializeBean(autowireTestBean, defaults); // no bean definition

      assertThat(autowireTestBean.name).isEqualTo(beanName);

      assertThat(autowireTestBean.initMethod).isTrue();
      assertThat(autowireTestBean.postConstruct).isTrue();
      assertThat(autowireTestBean.afterPropertiesSet).isTrue();
      assertThat(autowireTestBean.afterPostProcessor).isTrue();
      assertThat(autowireTestBean.beforePostProcessor).isTrue();
      assertThat(autowireTestBean.property).isEqualTo(0);
      assertThat(autowireTestBean.bean).isNull();

      // autowireBean
      beanFactory.autowireBean(autowireTestBean);
      assertThat(autowireTestBean.property).isEqualTo(2);
      assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
    }
  }

  @Test
  void testApplyBeanPostProcessorsBeforeInitialization() {
    String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      StandardBeanFactory beanFactory = context.getBeanFactory();

      beanFactory.addBeanPostProcessor(new PostProcessor());

      AutowireTestBean autowireTestBean = new AutowireTestBean();

      Object before = beanFactory.applyBeanPostProcessorsBeforeInitialization(autowireTestBean, beanName);
      assertThat(autowireTestBean).isEqualTo(before);

      assertThat(autowireTestBean.bean).isNull();
      assertThat(autowireTestBean.name).isNull();
      assertThat(autowireTestBean.property).isZero();
      assertThat(autowireTestBean.initMethod).isFalse();
      assertThat(autowireTestBean.postConstruct).isFalse();
      assertThat(autowireTestBean.afterPropertiesSet).isFalse();

      assertThat(autowireTestBean.afterPostProcessor).isFalse();
      assertThat(autowireTestBean.beforePostProcessor).isTrue();
    }
  }

  @Test
  void testApplyBeanPostProcessorsAfterInitialization() {
    String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      StandardBeanFactory beanFactory = context.getBeanFactory();

      beanFactory.addBeanPostProcessor(new PostProcessor());

      AutowireTestBean autowireTestBean = new AutowireTestBean();

      Object after = beanFactory.applyBeanPostProcessorsAfterInitialization(autowireTestBean, beanName);
      assertThat(autowireTestBean).isEqualTo(after);

      assertThat(autowireTestBean.bean).isNull();
      assertThat(autowireTestBean.name).isNull();
      assertThat(autowireTestBean.property).isZero();
      assertThat(autowireTestBean.initMethod).isFalse();
      assertThat(autowireTestBean.postConstruct).isFalse();
      assertThat(autowireTestBean.afterPropertiesSet).isFalse();

      assertThat(autowireTestBean.afterPostProcessor).isTrue();
      assertThat(autowireTestBean.beforePostProcessor).isFalse();

    }
  }

  @Test
  void testDestroyBean() {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      StandardBeanFactory beanFactory = context.getBeanFactory();

      beanFactory.addBeanPostProcessor(new PostProcessor());

      AutowireTestBean autowireTestBean = new AutowireTestBean();

      assertThat(autowireTestBean.bean).isNull();
      assertThat(autowireTestBean.name).isNull();
      assertThat(autowireTestBean.property).isZero();
      assertThat(autowireTestBean.initMethod).isFalse();
      assertThat(autowireTestBean.postConstruct).isFalse();
      assertThat(autowireTestBean.afterPropertiesSet).isFalse();

      assertThat(autowireTestBean.afterPostProcessor).isFalse();
      assertThat(autowireTestBean.beforePostProcessor).isFalse();

      beanFactory.destroyBean(autowireTestBean);
      assertThat(autowireTestBean.postProcessBeforeDestruction).isTrue();

    }
  }

  @Test
  void configureBean() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition bd = new BeanDefinition(TestBean.class);
    bd.addPropertyValue("age", "99");
    beanFactory.registerBeanDefinition("test", bd);
    TestBean tb = new TestBean();
    assertThat(tb.getAge()).isEqualTo(0);
    beanFactory.configureBean(tb, "test");
    assertThat(tb.getAge()).isEqualTo(99);
    assertThat(tb.getBeanFactory()).isSameAs(beanFactory);
    assertThat(tb.getSpouse()).isNull();
  }

  @Test
  void configureBeanWithAutowiring() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    StandardDependenciesBeanPostProcessor postProcessor = new StandardDependenciesBeanPostProcessor(beanFactory);
    beanFactory.addBeanPostProcessor(postProcessor);

    BeanDefinition bd = new BeanDefinition(TestBean.class);
    beanFactory.registerBeanDefinition("spouse", bd);
    bd.addPropertyValue("age", "99");

    BeanDefinition tbd = new BeanDefinition(ConfigureBeanWithAutowiring.class);
    beanFactory.registerBeanDefinition("test", tbd);
    ConfigureBeanWithAutowiring tb = new ConfigureBeanWithAutowiring();
    beanFactory.configureBean(tb, "test");
    assertThat(tb.getBeanFactory()).isSameAs(beanFactory);
    TestBean spouse = (TestBean) beanFactory.getBean("spouse");
    assertThat(tb.getSpouse()).isEqualTo(spouse);
  }

  @Getter
  @Setter
  static class ConfigureBeanWithAutowiring implements BeanFactoryAware {
    @Autowired
    TestBean spouse;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

  }

}
