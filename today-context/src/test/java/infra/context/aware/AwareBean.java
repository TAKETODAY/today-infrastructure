/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.context.aware;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanNameAware;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.EnvironmentAware;
import infra.core.env.Environment;
import infra.stereotype.Singleton;

/**
 * @author Today <br>
 *
 * 2018-08-08 16:32
 */
@Singleton
public class AwareBean implements ApplicationContextAware, BeanFactoryAware, BeanNameAware, EnvironmentAware {

  private String beanName;

  private BeanFactory beanFactory;

  private ApplicationContext applicationContext;

  private Environment environment;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public String getBeanName() {
    return beanName;
  }

  public Environment getEnvironment() {
    return environment;
  }
}
