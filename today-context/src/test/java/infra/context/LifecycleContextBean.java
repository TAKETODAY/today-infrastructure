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
