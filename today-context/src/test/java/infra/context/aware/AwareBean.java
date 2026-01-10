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
