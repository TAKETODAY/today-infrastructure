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
package cn.taketoday.context.aware;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.support.MessageSourceAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Convenient superclass for application objects that want to be aware of
 * the application context, e.g. for custom lookup of collaborating beans
 * or for context-specific resource access. It saves the application
 * context reference and provides an initialization callback method.
 * Furthermore, it offers numerous convenience methods for message lookup.
 *
 * @author TODAY <br>
 * @since 2019-12-21 15:45
 */
public abstract class ApplicationContextSupport implements ApplicationContextAware {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Nullable
  protected ApplicationContext applicationContext;

  /** MessageSourceAccessor for easy message access. @since 4.0 */
  @Nullable
  protected MessageSourceAccessor messageSourceAccessor;

  @Override
  public final void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
    if (context == null && !isContextRequired()) {
      // Reset internal context state.
      this.applicationContext = null;
      this.messageSourceAccessor = null;
    }
    else if (this.applicationContext == null) {
      // Initialize with passed-in context.
      if (!requiredContextClass().isInstance(context)) {
        throw new ApplicationContextException(
                "Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
      }
      this.applicationContext = context;
      this.messageSourceAccessor = new MessageSourceAccessor(context);
      initApplicationContext(context);
    }
    else {
      // Ignore reinitialization if same context passed in.
      if (this.applicationContext != context) {
        throw new ApplicationContextException(
                "Cannot reinitialize with different application context: current one is [" +
                        this.applicationContext + "], passed-in one is [" + context + "]");
      }
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

  /**
   * Return the ApplicationContext that this object is associated with.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   */
  @Nullable
  public final ApplicationContext getApplicationContext() throws IllegalStateException {
    ApplicationContext context = this.applicationContext;
    if (context == null && isContextRequired()) {
      throw new IllegalStateException(
              "ApplicationContextSupport instance [" + this + "] does not run in an ApplicationContext");
    }
    return context;
  }

  /**
   * Return the ApplicationContext that this object is associated with.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   */
  public ApplicationContext obtainApplicationContext() {
    final ApplicationContext context = this.applicationContext;
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
  public <T> T unwrapContext(Class<T> requiredType) {
    return obtainApplicationContext().unwrap(requiredType);
  }

  /**
   * Return a MessageSourceAccessor for the application context
   * used by this object, for easy message access.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   * @since 4.0
   */
  @Nullable
  protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
    MessageSourceAccessor accessor = this.messageSourceAccessor;
    if (accessor == null && isContextRequired()) {
      throw new IllegalStateException(
              "ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
    }
    return accessor;
  }

  /**
   * Determine whether this application object needs to run in an ApplicationContext.
   * <p>Default is "false". Can be overridden to enforce running in a context
   * (i.e. to throw IllegalStateException on accessors if outside a context).
   *
   * @see #getApplicationContext
   * @see #getMessageSourceAccessor
   * @since 4.0
   */
  protected boolean isContextRequired() {
    return false;
  }

  /**
   * Determine the context class that any context passed to
   * {@code setApplicationContext} must be an instance of.
   * Can be overridden in subclasses.
   *
   * @see #setApplicationContext
   * @since 4.0
   */
  protected Class<?> requiredContextClass() {
    return ApplicationContext.class;
  }
}
