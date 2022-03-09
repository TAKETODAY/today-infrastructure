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

package cn.taketoday.aop.framework;

import java.io.Serial;
import java.util.ArrayList;

import cn.taketoday.aop.AdvisedSupportListener;
import cn.taketoday.lang.Assert;

/**
 * Base class for proxy factories.
 * Provides convenient access to a configurable AopProxyFactory.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 22:55
 * @see #createAopProxy()
 * @since 3.0
 */
public class ProxyCreatorSupport extends AdvisedSupport {
  @Serial
  private static final long serialVersionUID = 1L;
  /** Set to true when the first AOP proxy has been created. */
  private boolean active = false;

  private AopProxyFactory aopProxyFactory;
  private final ArrayList<AdvisedSupportListener> listeners = new ArrayList<>();

  /**
   * Create a new ProxyCreatorSupport instance.
   */
  public ProxyCreatorSupport() {
    this.aopProxyFactory = new DefaultAopProxyFactory();
  }

  /**
   * Create a new ProxyCreatorSupport instance.
   *
   * @param aopProxyFactory the AopProxyFactory to use
   */
  public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
    Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
    this.aopProxyFactory = aopProxyFactory;
  }

  /**
   * Customize the AopProxyFactory, allowing different strategies
   * to be dropped in without changing the core framework.
   * <p>Default is {@link DefaultAopProxyFactory}, using dynamic JDK
   * proxies or CGLIB proxies based on the requirements.
   */
  public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
    Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
    this.aopProxyFactory = aopProxyFactory;
  }

  /**
   * Return the AopProxyFactory that this ProxyConfig uses.
   */
  public AopProxyFactory getAopProxyFactory() {
    return this.aopProxyFactory;
  }

  /**
   * Subclasses should call this to get a new AOP proxy. They should <b>not</b>
   * create an AOP proxy with {@code this} as an argument.
   */
  protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
      activate();
    }
    return aopProxyFactory.createAopProxy(this);
  }

  /**
   * Activate this proxy configuration.
   */
  private void activate() {
    this.active = true;

    for (AdvisedSupportListener listener : this.listeners) {
      listener.activated(this);
    }
  }

  /**
   * Propagate advice change event to all AdvisedSupportListeners.
   *
   * @see AdvisedSupportListener#adviceChanged
   */
  @Override
  protected void adviceChanged() {
    super.adviceChanged();
    synchronized(this) {
      if (this.active) {
        for (AdvisedSupportListener listener : this.listeners) {
          listener.adviceChanged(this);
        }
      }
    }
  }

  /**
   * Subclasses can call this to check whether any AOP proxies have been created yet.
   */
  protected final synchronized boolean isActive() {
    return this.active;
  }

  /**
   * Add the given AdvisedSupportListener to this proxy configuration.
   *
   * @param listener the listener to register
   */
  public void addListener(AdvisedSupportListener listener) {
    Assert.notNull(listener, "AdvisedSupportListener must not be null");
    this.listeners.add(listener);
  }

  /**
   * Remove the given AdvisedSupportListener from this proxy configuration.
   *
   * @param listener the listener to deregister
   */
  public void removeListener(AdvisedSupportListener listener) {
    Assert.notNull(listener, "AdvisedSupportListener must not be null");
    this.listeners.remove(listener);
  }

}
