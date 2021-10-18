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
package cn.taketoday.context.aware;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY <br>
 * 2019-12-21 15:45
 */
public abstract class ApplicationContextSupport implements ApplicationContextAware {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private ApplicationContext applicationContext;

  @Override
  public final void setApplicationContext(final ApplicationContext context) {
    if (this.applicationContext == null) {
      this.applicationContext = context;
      initApplicationContext(context);
    }
    else if (this.applicationContext != context) {
      // Ignore reinitialization if same context passed in.
      throw new ApplicationContextException(
              "Cannot reinitialize with different application context: current one is [" +
                      this.applicationContext + "], passed-in one is [" + context + "]");
    }
  }

  /**
   * Subclasses can override this for custom initialization behavior. Gets called
   * by {@code setApplicationContext} after setting the context instance.
   * <p>
   * Note: Does <i>not</i> get called on re-initialization of the context but
   * rather just on first initialization of this object's context reference.
   * <p>
   * The default implementation calls the overloaded
   * {@link #initApplicationContext()} method without ApplicationContext
   * reference.
   *
   * @param context the containing ApplicationContext
   * @throws ApplicationContextException if thrown by ApplicationContext methods
   * @see #setApplicationContext
   */
  protected void initApplicationContext(ApplicationContext context) {
    initApplicationContext();
  }

  /**
   * Subclasses can override this for custom initialization behavior.
   * <p>
   * The default implementation is empty. Called by
   * {@link #initApplicationContext(ApplicationContext)}.
   *
   * @throws ApplicationContextException if thrown by ApplicationContext methods
   * @see #setApplicationContext
   */
  protected void initApplicationContext() { }

  public final ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /**
   * Return the ApplicationContext that this object is associated with.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   */
  public ApplicationContext obtainApplicationContext() {
    final ApplicationContext context = getApplicationContext();
    Assert.state(context != null, "No ApplicationContext");
    return context;
  }

  /**
   * unwrap bean-factory to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   * @see ApplicationContext#getBeanFactory()
   * @since 4.0
   */
  public <T> T unwrapFactory(Class<T> requiredType) {
    return obtainApplicationContext().unwrapFactory(requiredType);
  }

  /**
   * unwrap this ApplicationContext to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   * @since 4.0
   */
  public <T> T unwrap(Class<T> requiredType) {
    return obtainApplicationContext().unwrap(requiredType);
  }

}
