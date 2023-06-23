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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, RootBeanDefinition)} now ensures that
 * not only the predicted bean type is considered, but also the original bean definition's
 * beanClass.
 *
 * @author Chris Beams
 * @author Oliver Gierke
 */
public class Spr8954Tests {

  private StandardBeanFactory bf;

  @BeforeEach
  public void setUp() {
    bf = new StandardBeanFactory();
    bf.registerBeanDefinition("foo", new RootBeanDefinition(FooFactoryBean.class));
    bf.addBeanPostProcessor(new PredictingBPP());
  }

  @Test
  public void repro() {
    assertThat(bf.getBean("foo")).isInstanceOf(Foo.class);
    assertThat(bf.getBean("&foo")).isInstanceOf(FooFactoryBean.class);
    assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

    @SuppressWarnings("rawtypes")
    Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
    assertThat(fbBeans).hasSize(1);
    assertThat(fbBeans.keySet()).contains("&foo");

    Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
    assertThat(aiBeans).hasSize(1);
    assertThat(aiBeans.keySet()).contains("&foo");
  }

  @Test
  public void findsBeansByTypeIfNotInstantiated() {
    assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

    @SuppressWarnings("rawtypes")
    Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
    assertThat(fbBeans.size()).isEqualTo(1);
    assertThat(fbBeans.keySet().iterator().next()).isEqualTo("&foo");

    Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
    assertThat(aiBeans).hasSize(1);
    assertThat(aiBeans.keySet()).contains("&foo");
  }

  /**
   * SPR-10517
   */
  @Test
  public void findsFactoryBeanNameByTypeWithoutInstantiation() {
    Set<String> names = bf.getBeanNamesForType(AnInterface.class, false, false);
    assertThat(names).contains("&foo");

    Map<String, AnInterface> beans = bf.getBeansOfType(AnInterface.class, false, false);
    assertThat(beans).hasSize(1);
    assertThat(beans.keySet()).contains("&foo");
  }

  static class FooFactoryBean implements FactoryBean<Foo>, AnInterface {

    @Override
    public Foo getObject() throws Exception {
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

  static class PredictedTypeImpl implements PredictedType {
  }

  static class PredictingBPP implements SmartInstantiationAwareBeanPostProcessor {

    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
      return FactoryBean.class.isAssignableFrom(beanClass) ? PredictedType.class : null;
    }
  }

}
