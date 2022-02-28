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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.context.loader.BootstrapContext;

/**
 * Corners the bug originally reported by SPR-8824, where the presence of two
 * {@link ConfigurationClassPostProcessor} beans in combination with a @Configuration
 * class having at least one @Bean method causes a "Singleton 'foo' isn't currently in
 * creation" exception.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class DuplicateConfigurationClassPostProcessorTests {

  @Test
  public void repro() {
    GenericApplicationContext ctx = new GenericApplicationContext();
    StandardBeanFactory beanFactory = ctx.getBeanFactory();
    BootstrapContext loadingContext = new BootstrapContext(beanFactory, ctx);
    beanFactory.registerSingleton(loadingContext);
    ctx.registerBeanDefinition("a", new BeanDefinition(ConfigurationClassPostProcessor.class, BeanDefinition.AUTOWIRE_CONSTRUCTOR));
    ctx.registerBeanDefinition("b", new BeanDefinition(ConfigurationClassPostProcessor.class, BeanDefinition.AUTOWIRE_CONSTRUCTOR));
    ctx.registerBeanDefinition("myConfig", new BeanDefinition(Config.class));
    ctx.refresh();
  }

  @Configuration
  static class Config {
    @Bean
    public String string() {
      return "bean";
    }
  }
}
