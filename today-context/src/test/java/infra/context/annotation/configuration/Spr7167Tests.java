/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation.configuration;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class Spr7167Tests {

  @SuppressWarnings("deprecation")
  @Test
  public void test() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

    assertThat(ctx.getBeanFactory().getBeanDefinition("someDependency").getDescription())
            .as("someDependency was not post processed")
            .isEqualTo("post processed by MyPostProcessor");

    MyConfig config = ctx.getBean(MyConfig.class);
    AssertionsForClassTypes.assertThat(config).as("Config class was not enhanced")
            .isInstanceOf(BeanFactoryAware.class);
  }

}

@Configuration
class MyConfig {

  @Bean
  public Dependency someDependency() {
    return new Dependency();
  }

  @Bean
  public BeanFactoryPostProcessor thePostProcessor() {
    return new MyPostProcessor(someDependency());
  }
}

class Dependency {
}

class MyPostProcessor implements BeanFactoryPostProcessor {

  public MyPostProcessor(Dependency someDependency) {
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    BeanDefinition bd = beanFactory.getBeanDefinition("someDependency");
    bd.setDescription("post processed by MyPostProcessor");
  }
}
