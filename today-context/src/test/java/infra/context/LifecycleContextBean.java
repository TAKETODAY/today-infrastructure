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

package infra.context;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.testfixture.beans.LifecycleBean;

/**
 * Simple bean to test ApplicationContext lifecycle methods for beans
 *
 * @author Colin Sampaleanu
 */
public class LifecycleContextBean extends LifecycleBean implements ApplicationContextAware {

  protected ApplicationContext owningContext;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    if (this.owningContext != null) {
      throw new RuntimeException("Factory called setBeanFactory after setApplicationContext");
    }
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    if (this.owningContext == null) {
      throw new RuntimeException("Factory didn't call setApplicationContext before afterPropertiesSet on lifecycle bean");
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    if (this.owningFactory == null) {
      throw new RuntimeException("Factory called setApplicationContext before setBeanFactory");
    }

    this.owningContext = applicationContext;
  }

}
