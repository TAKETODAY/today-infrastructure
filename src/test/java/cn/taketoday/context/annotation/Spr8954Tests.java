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

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, BeanDefinition)} now ensures
 * that not only the predicted bean type is considered, but also the original bean
 * definition's beanClass.
 *
 * @author Chris Beams
 * @author Oliver Gierke
 */
@SuppressWarnings("resource")
public class Spr8954Tests {

  @Test
  public void repro() {
    StandardApplicationContext bf = new StandardApplicationContext();
    bf.registerBeanDefinition("fooConfig", new BeanDefinition(FooConfig.class));
    bf.getBeanFactory().addBeanPostProcessor(new PredictingBPP());
    bf.refresh();

    assertThat(bf.getBean("foo")).isInstanceOf(Foo.class);
    assertThat(bf.getBean("&foo")).isInstanceOf(FooFactoryBean.class);

    assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

    @SuppressWarnings("rawtypes")
    Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
    assertThat(1).isEqualTo(fbBeans.size());
    assertThat("&foo").isEqualTo(fbBeans.keySet().iterator().next());

    Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
    assertThat(1).isEqualTo(aiBeans.size());
    assertThat("&foo").isEqualTo(aiBeans.keySet().iterator().next());
  }

  @Test
  public void findsBeansByTypeIfNotInstantiated() {
    StandardApplicationContext bf = new StandardApplicationContext();
    bf.registerBeanDefinition("fooConfig", new BeanDefinition(FooConfig.class));
    bf.getBeanFactory().addBeanPostProcessor(new PredictingBPP());
    bf.refresh();

    assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

    @SuppressWarnings("rawtypes")
    Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
    assertThat(1).isEqualTo(fbBeans.size());
    assertThat("&foo").isEqualTo(fbBeans.keySet().iterator().next());

    Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
    assertThat(1).isEqualTo(aiBeans.size());
    assertThat("&foo").isEqualTo(aiBeans.keySet().iterator().next());
  }

  static class FooConfig {

    @Bean
    FooFactoryBean foo() {
      return new FooFactoryBean();
    }
  }

  static class FooFactoryBean implements FactoryBean<Foo>, AnInterface {

    @Override
    public Foo getObject() {
      return new Foo();
    }

    @Override
    public Class<?> getObjectType() {
      return Foo.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  interface AnInterface {
  }

  static class Foo {
  }

  interface PredictedType {
  }

  static class PredictingBPP implements SmartInstantiationAwareBeanPostProcessor {

    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
      return FactoryBean.class.isAssignableFrom(beanClass) ? PredictedType.class : null;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
      return pvs;
    }
  }

}
