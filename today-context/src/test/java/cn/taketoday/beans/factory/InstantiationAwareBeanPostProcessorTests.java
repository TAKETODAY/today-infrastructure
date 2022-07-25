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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.MethodMetadata;
import jakarta.inject.Singleton;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/21 22:47
 */
class InstantiationAwareBeanPostProcessorTests {

  static class InstantiationAwareBeanPostProcessor0 implements InstantiationAwareBeanPostProcessor {
    final BeanFactory factory;

    InstantiationAwareBeanPostProcessor0(BeanFactory factory) {
      this.factory = factory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
      BeanDefinition def = factory.getBeanDefinition(beanName);
      // your Instantiation Strategy
      if (def instanceof AnnotatedBeanDefinition annotated) {
        MethodMetadata metadata = annotated.getFactoryMethodMetadata();
        if (((AbstractBeanDefinition) def).getBeanClass() == InstantiationAwareBeanPostProcessorBean.class && metadata == null) {
          return new InstantiationAwareBeanPostProcessorBean(); // your strategy
        }
      }
      return null; // use default factory strategy
    }
  }

  static class InstantiationAwareBeanPostProcessorBean {
    private final BeanFactory factory;

    InstantiationAwareBeanPostProcessorBean() {
      factory = null;
    }

    InstantiationAwareBeanPostProcessorBean(BeanFactory factory) {
      this.factory = factory;
    }
  }

  @Configuration
  static class InstantiationAwareBeanPostProcessorConfig {

    @Singleton
    InstantiationAwareBeanPostProcessorBean instantiationBean(BeanFactory factory) {
      return new InstantiationAwareBeanPostProcessorBean(factory);
    }

  }

  @Test
  void postProcessBeforeInstantiation() {
    try (StandardApplicationContext context = new StandardApplicationContext()) {
      ConfigurableBeanFactory beanFactory = context.unwrapFactory(ConfigurableBeanFactory.class);

      context.register(InstantiationAwareBeanPostProcessorBean.class);
      context.register(InstantiationAwareBeanPostProcessorConfig.class);
      context.refresh();

      InstantiationAwareBeanPostProcessor0 postProcessor = new InstantiationAwareBeanPostProcessor0(context);
      beanFactory.addBeanPostProcessor(postProcessor);

      InstantiationAwareBeanPostProcessorBean bean = context.getBean(
              "instantiationAwareBeanPostProcessorTests.InstantiationAwareBeanPostProcessorBean", InstantiationAwareBeanPostProcessorBean.class);

      Object instantiationBean = context.getBean("instantiationBean");

      assertThat(instantiationBean).isInstanceOf(InstantiationAwareBeanPostProcessorBean.class);
      assertThat(instantiationBean).isNotEqualTo(bean);
      assertThat(bean.factory).isNull();

      InstantiationAwareBeanPostProcessorBean byFactory = (InstantiationAwareBeanPostProcessorBean) instantiationBean;
      assertThat(byFactory.factory).isNotNull();
    }

  }
}
