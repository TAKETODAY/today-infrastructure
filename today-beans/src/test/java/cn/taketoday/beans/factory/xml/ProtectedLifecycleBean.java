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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;

public class ProtectedLifecycleBean implements BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

  protected boolean initMethodDeclared = false;

  protected String beanName;

  protected BeanFactory owningFactory;

  protected boolean postProcessedBeforeInit;

  protected boolean inited;

  protected boolean initedViaDeclaredInitMethod;

  protected boolean postProcessedAfterInit;

  protected boolean destroyed;

  public void setInitMethodDeclared(boolean initMethodDeclared) {
    this.initMethodDeclared = initMethodDeclared;
  }

  public boolean isInitMethodDeclared() {
    return initMethodDeclared;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  public String getBeanName() {
    return beanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.owningFactory = beanFactory;
  }

  public void postProcessBeforeInit() {
    if (this.inited || this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called postProcessBeforeInit after afterPropertiesSet");
    }
    if (this.postProcessedBeforeInit) {
      throw new RuntimeException("Factory called postProcessBeforeInit twice");
    }
    this.postProcessedBeforeInit = true;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.owningFactory == null) {
      throw new RuntimeException("Factory didn't call setBeanFactory before afterPropertiesSet on lifecycle bean");
    }
    if (!this.postProcessedBeforeInit) {
      throw new RuntimeException("Factory didn't call postProcessBeforeInit before afterPropertiesSet on lifecycle bean");
    }
    if (this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory initialized via declared init method before initializing via afterPropertiesSet");
    }
    if (this.inited) {
      throw new RuntimeException("Factory called afterPropertiesSet twice");
    }
    this.inited = true;
  }

  public void declaredInitMethod() {
    if (!this.inited) {
      throw new RuntimeException("Factory didn't call afterPropertiesSet before declared init method");
    }

    if (this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called declared init method twice");
    }
    this.initedViaDeclaredInitMethod = true;
  }

  public void postProcessAfterInit() {
    if (!this.inited) {
      throw new RuntimeException("Factory called postProcessAfterInit before afterPropertiesSet");
    }
    if (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called postProcessAfterInit before calling declared init method");
    }
    if (this.postProcessedAfterInit) {
      throw new RuntimeException("Factory called postProcessAfterInit twice");
    }
    this.postProcessedAfterInit = true;
  }

  /**
   * Dummy business method that will fail unless the factory
   * managed the bean's lifecycle correctly
   */
  public void businessMethod() {
    if (!this.inited || (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) ||
            !this.postProcessedAfterInit) {
      throw new RuntimeException("Factory didn't initialize lifecycle object correctly");
    }
  }

  @Override
  public void destroy() {
    if (this.destroyed) {
      throw new IllegalStateException("Already destroyed");
    }
    this.destroyed = true;
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  public static class PostProcessor implements InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
      if (bean instanceof ProtectedLifecycleBean) {
        ((ProtectedLifecycleBean) bean).postProcessBeforeInit();
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
      if (bean instanceof ProtectedLifecycleBean) {
        ((ProtectedLifecycleBean) bean).postProcessAfterInit();
      }
      return bean;
    }
  }
}
