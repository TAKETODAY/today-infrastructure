/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.factory;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.InstantiationAwareBeanPostProcessor;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.type.MethodMetadata;
import infra.stereotype.Singleton;

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
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
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
