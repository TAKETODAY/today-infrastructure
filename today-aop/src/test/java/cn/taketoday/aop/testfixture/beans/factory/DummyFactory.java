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

package cn.taketoday.aop.testfixture.beans.factory;

import cn.taketoday.aop.testfixture.beans.TestBean;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Simple factory to allow testing of FactoryBean support in AbstractBeanFactory.
 * Depending on whether its singleton property is set, it will return a singleton
 * or a prototype instance.
 *
 * <p>Implements InitializingBean interface, so we can check that
 * factories get this lifecycle callback if they want.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 10.03.2003
 */
public class DummyFactory
        implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

  public static final String SINGLETON_NAME = "Factory singleton";

  private static boolean prototypeCreated;

  /**
   * Clear static state.
   */
  public static void reset() {
    prototypeCreated = false;
  }

  /**
   * Default is for factories to return a singleton instance.
   */
  private boolean singleton = true;

  private String beanName;

  private AutowireCapableBeanFactory beanFactory;

  private boolean postProcessed;

  private boolean initialized;

  private final TestBean testBean;

  private TestBean otherTestBean;

  public DummyFactory() {
    this.testBean = new TestBean();
    this.testBean.setName(SINGLETON_NAME);
    this.testBean.setAge(25);
  }

  /**
   * Return if the bean managed by this factory is a singleton.
   *
   * @see FactoryBean#isSingleton()
   */
  @Override
  public boolean isSingleton() {
    return this.singleton;
  }

  /**
   * Set if the bean managed by this factory is a singleton.
   */
  public void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
    this.beanFactory.applyBeanPostProcessorsBeforeInitialization(this.testBean, this.beanName);
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setPostProcessed(boolean postProcessed) {
    this.postProcessed = postProcessed;
  }

  public boolean isPostProcessed() {
    return postProcessed;
  }

  public void setOtherTestBean(TestBean otherTestBean) {
    this.otherTestBean = otherTestBean;
    this.testBean.setSpouse(otherTestBean);
  }

  public TestBean getOtherTestBean() {
    return otherTestBean;
  }

  @Override
  public void afterPropertiesSet() {
    if (initialized) {
      throw new RuntimeException("Cannot call afterPropertiesSet twice on the one bean");
    }
    this.initialized = true;
  }

  /**
   * Was this initialized by invocation of the
   * afterPropertiesSet() method from the InitializingBean interface?
   */
  public boolean wasInitialized() {
    return initialized;
  }

  public static boolean wasPrototypeCreated() {
    return prototypeCreated;
  }

  /**
   * Return the managed object, supporting both singleton
   * and prototype mode.
   *
   * @see FactoryBean#getObject()
   */
  @Override
  public TestBean getObject() throws BeansException {
    if (isSingleton()) {
      return this.testBean;
    }
    else {
      TestBean prototype = new TestBean("prototype created at " + System.currentTimeMillis(), 11);
      if (this.beanFactory != null) {
        this.beanFactory.applyBeanPostProcessorsBeforeInitialization(prototype, this.beanName);
      }
      prototypeCreated = true;
      return prototype;
    }
  }

  @Override
  public Class<?> getObjectType() {
    return TestBean.class;
  }

  @Override
  public void destroy() {
    if (this.testBean != null) {
      this.testBean.setName(null);
    }
  }

}
