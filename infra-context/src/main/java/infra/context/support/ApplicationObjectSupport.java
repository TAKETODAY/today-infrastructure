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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationContextException;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Convenient superclass for application objects that want to be aware of
 * the application context, e.g. for custom lookup of collaborating beans
 * or for context-specific resource access. It saves the application
 * context reference and provides an initialization callback method.
 * Furthermore, it offers numerous convenience methods for message lookup.
 *
 * <p>There is no requirement to subclass this class: It just makes things
 * a little easier if you need access to the context, e.g. for access to
 * file resources or to the message source. Note that many application
 * objects do not need to be aware of the application context at all,
 * as they can receive collaborating beans via bean references.
 *
 * <p>Many framework classes are derived from this class, particularly
 * within the web support.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-21 15:45
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected @Nullable ApplicationContext applicationContext;

  /**
   * MessageSourceAccessor for easy message access.
   *
   * @since 4.0
   */
  protected @Nullable MessageSourceAccessor messageSourceAccessor;

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
                "Invalid application context: needs to be of type [%s]".formatted(requiredContextClass().getName()));
      }
      this.applicationContext = context;
      initApplicationContext(context);
    }
    else {
      // Ignore reinitialization if same context passed in.
      if (this.applicationContext != context) {
        throw new ApplicationContextException("Cannot reinitialize with different application context: current one is [%s], passed-in one is [%s]"
                .formatted(this.applicationContext, context));
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
  protected void initApplicationContext() {
  }

  /**
   * Return the ApplicationContext that this object is associated with.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   */
  public final @Nullable ApplicationContext getApplicationContext() throws IllegalStateException {
    ApplicationContext context = this.applicationContext;
    if (context == null && isContextRequired()) {
      throw new IllegalStateException(
              "ApplicationContextSupport instance [%s] does not run in an ApplicationContext".formatted(this));
    }
    return context;
  }

  /**
   * Return the ApplicationContext that this object is associated with.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   */
  public ApplicationContext applicationContext() {
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
    return applicationContext().unwrapFactory(requiredType);
  }

  /**
   * unwrap this ApplicationContext to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   * @since 4.0
   */
  public <T> T unwrapContext(Class<T> requiredType) {
    return applicationContext().unwrap(requiredType);
  }

  /**
   * Return a MessageSourceAccessor for the application context
   * used by this object, for easy message access.
   *
   * @throws IllegalStateException if not running in an ApplicationContext
   * @since 4.0
   */
  protected final @Nullable MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
    MessageSourceAccessor accessor = this.messageSourceAccessor;
    if (accessor == null) {
      ApplicationContext context = this.applicationContext;
      if (context == null) {
        if (isContextRequired()) {
          throw new IllegalStateException(
                  "ApplicationObjectSupport instance [%s] does not run in an ApplicationContext".formatted(this));
        }
        return null;
      }
      else {
        accessor = new MessageSourceAccessor(context);
        this.messageSourceAccessor = accessor;
      }
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
