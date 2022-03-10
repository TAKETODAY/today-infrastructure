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

import java.io.IOException;
import java.util.Map;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.support.ApplicationPropertySourcesProcessor;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Singleton;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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
  public void lifecycle() throws IOException {
    try (StandardApplicationContext context = new StandardApplicationContext()) {
      ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(context);
      processor.setPropertiesLocation("info.properties");
      processor.postProcessEnvironment();

      context.register(LifecycleBean.class);

      BeanDefinitionRegistry registry = context.unwrapFactory(BeanDefinitionRegistry.class);
      Map<String, BeanDefinition> beanDefinitionsMap = registry.getBeanDefinitions();
      System.out.println(beanDefinitionsMap);
    }
  }

}
