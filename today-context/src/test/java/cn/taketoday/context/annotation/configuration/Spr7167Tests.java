/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation.configuration;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;

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
