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

package cn.taketoday.aop.support;

import org.aopalliance.aop.Advice;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract BeanFactory-based PointcutAdvisor that allows for any Advice
 * to be configured as reference to an Advice bean in a BeanFactory.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself
 * (if running within a BeanFactory) increases loose coupling at initialization time,
 * in order to not initialize the advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 * @since 4.0 2021/12/10 21:20
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor
        extends AbstractPointcutAdvisor implements BeanFactoryAware {

  @Nullable
  private String adviceBeanName;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private transient volatile Advice advice;

  private transient volatile Object adviceMonitor = new Object();

  /**
   * Specify the name of the advice bean that this advisor should refer to.
   * <p>An instance of the specified bean will be obtained on first access
   * of this advisor's advice. This advisor will only ever obtain at most one
   * single instance of the advice bean, caching the instance for the lifetime
   * of the advisor.
   *
   * @see #getAdvice()
   */
  public void setAdviceBeanName(@Nullable String adviceBeanName) {
    this.adviceBeanName = adviceBeanName;
  }

  /**
   * Return the name of the advice bean that this advisor refers to, if any.
   */
  @Nullable
  public String getAdviceBeanName() {
    return this.adviceBeanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    resetAdviceMonitor();
  }

  private void resetAdviceMonitor() {
    if (this.beanFactory instanceof ConfigurableBeanFactory) {
      this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
    }
    else {
      this.adviceMonitor = new Object();
    }
  }

  /**
   * Specify a particular instance of the target advice directly,
   * avoiding lazy resolution in {@link #getAdvice()}.
   */
  public void setAdvice(Advice advice) {
    synchronized(this.adviceMonitor) {
      this.advice = advice;
    }
  }

  @Override
  public Advice getAdvice() {
    Advice advice = this.advice;
    if (advice != null) {
      return advice;
    }

    Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
    Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

    if (this.beanFactory.isSingleton(this.adviceBeanName)) {
      // Rely on singleton semantics provided by the factory.
      advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
      this.advice = advice;
      return advice;
    }
    else {
      // No singleton guarantees from the factory -> let's lock locally but
      // reuse the factory's singleton lock, just in case a lazy dependency
      // of our advice bean happens to trigger the singleton lock implicitly...
      synchronized(this.adviceMonitor) {
        advice = this.advice;
        if (advice == null) {
          advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
          this.advice = advice;
        }
        return advice;
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append(": advice ");
    if (this.adviceBeanName != null) {
      sb.append("bean '").append(this.adviceBeanName).append('\'');
    }
    else {
      sb.append(this.advice);
    }
    return sb.toString();
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization, just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    resetAdviceMonitor();
  }

}

