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

package cn.taketoday.aop.target;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for pooling {@link TargetSource}
 * implementations which maintain a pool of target instances, acquiring and
 * releasing a target object from the pool for each method invocation.
 * This abstract base class is independent of concrete pooling technology;
 * see the subclass {@link CommonsPool2TargetSource} for a concrete example.
 *
 * <p>Subclasses must implement the {@link #getTarget} and
 * {@link #releaseTarget} methods based on their chosen object pool.
 * The {@link #newPrototypeInstance()} method inherited from
 * {@link AbstractPrototypeTargetSource} can be used to create objects
 * in order to put them into the pool.
 *
 * <p>Subclasses must also implement some of the monitoring methods from the
 * {@link PoolingConfig} interface. The {@link #getPoolingConfigMixin()} method
 * makes these stats available on proxied objects through an IntroductionAdvisor.
 *
 * <p>This class implements the {@link cn.taketoday.beans.factory.DisposableBean}
 * interface in order to force subclasses to implement a {@link #destroy()}
 * method, closing down their object pool.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getTarget
 * @see #releaseTarget
 * @see #destroy
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractPoolingTargetSource
        extends AbstractPrototypeTargetSource implements PoolingConfig, DisposableBean {

  /** The maximum size of the pool. */
  private int maxSize = -1;

  /**
   * Set the maximum size of the pool.
   * Default is -1, indicating no size limit.
   */
  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  /**
   * Return the maximum size of the pool.
   */
  @Override
  public int getMaxSize() {
    return this.maxSize;
  }

  @Override
  public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    super.setBeanFactory(beanFactory);
    try {
      createPool();
    }
    catch (Throwable ex) {
      throw new BeanInitializationException("Could not create instance pool for TargetSource", ex);
    }
  }

  /**
   * Create the pool.
   *
   * @throws Exception to avoid placing constraints on pooling APIs
   */
  protected abstract void createPool() throws Exception;

  /**
   * Acquire an object from the pool.
   *
   * @return an object from the pool
   * @throws Exception we may need to deal with checked exceptions from pool
   * APIs, so we're forgiving with our exception signature
   */
  @Override
  @Nullable
  public abstract Object getTarget() throws Exception;

  /**
   * Return the given object to the pool.
   *
   * @param target object that must have been acquired from the pool
   * via a call to {@code getTarget()}
   * @throws Exception to allow pooling APIs to throw exception
   * @see #getTarget
   */
  @Override
  public abstract void releaseTarget(Object target) throws Exception;

  /**
   * Return an IntroductionAdvisor that provides a mixin
   * exposing statistics about the pool maintained by this object.
   */
  public DefaultIntroductionAdvisor getPoolingConfigMixin() {
    DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
    return new DefaultIntroductionAdvisor(dii, PoolingConfig.class);
  }

}
