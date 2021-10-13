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
package cn.taketoday.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Singleton;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY <br>
 * 2019-07-25 22:44
 */
@Singleton
public class LifecycleBean
        implements DisposableBean, BeanNameAware,
                   InitializingBean, BeanFactoryAware,
                   EnvironmentAware, ApplicationContextAware {

  private static final Logger log = LoggerFactory.getLogger(LifecycleBean.class);

  @Override
  public void setBeanName(String name) {
    log.info("setBeanName: {}", name);
  }

  @Override
  public void setEnvironment(Environment environment) {
    log.info("setEnvironment: {}", environment);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    log.info("setBeanFactory: {}", beanFactory);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    log.info("setApplicationContext: {}", applicationContext);
  }

  @PostConstruct
  public void initData(ApplicationContext context, LifecycleBean myself) {
    log.info("@PostConstruct");
    log.info("@PostConstruct {}", context);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    log.info("afterPropertiesSet");
  }

  @PreDestroy
  public void preDestroy() {
    log.info("preDestroy");
  }

  @Override
  public void destroy() throws Exception {
    log.info("destroy");
  }

  @Test
  public void lifecycle() {
    try (StandardApplicationContext context = new StandardApplicationContext("info.properties")) {
      context.register(LifecycleBean.class);

      BeanDefinitionRegistry registry = context.unwrapFactory(BeanDefinitionRegistry.class);
      Map<String, BeanDefinition> beanDefinitionsMap = registry.getBeanDefinitions();
      System.out.println(beanDefinitionsMap);
    }
  }

}
