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

package infra.web;

import java.util.concurrent.atomic.AtomicBoolean;

import infra.beans.factory.BeanNameAware;
import infra.context.ApplicationContext;
import infra.context.SmartLifecycle;
import infra.core.Conventions;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Base class for web lifecycle management, implementing {@link SmartLifecycle}
 * to handle initialization and destruction phases within the application context.
 * <p>Provides template methods such as {@link #onRefresh(ApplicationContext)}
 * for subclasses to customize startup behavior. Also supports configurable
 * logging of request details for debugging purposes.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/26 23:21
 */
public abstract class WebLifecycleManager implements BeanNameAware, SmartLifecycle {

  protected static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

  protected String beanName = Conventions.getVariableName(this);

  private final ApplicationContext applicationContext;

  /** Whether to log potentially sensitive info (request params at DEBUG + headers at TRACE). */
  private boolean enableLoggingRequestDetails = false;

  protected final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Create a new {@code WebLifecycleManager} with the given application context.
   *
   * @param applicationContext the context to use
   */
  protected WebLifecycleManager(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * Template method which can be overridden to add infra-specific refresh work.
   * Called after successful context refresh.
   * <p>This implementation is empty.
   *
   * @param context the current ApplicationContext
   */
  protected void onRefresh(ApplicationContext context) {
    // For subclasses: do nothing by default.
  }

  /**
   * Destroy Application
   */
  public void destroy() {
  }

  /**
   * Return this ApplicationContext.
   */
  public final ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  /**
   * Whether to log request params at DEBUG level, and headers at TRACE level.
   * Both may contain sensitive information.
   * <p>By default, set to {@code false} so that request details are not shown.
   *
   * @param enable whether to enable or not
   */
  public void setEnableLoggingRequestDetails(boolean enable) {
    this.enableLoggingRequestDetails = enable;
  }

  /**
   * Whether logging of potentially sensitive, request details at DEBUG and
   * TRACE level is allowed.
   */
  public boolean isEnableLoggingRequestDetails() {
    return this.enableLoggingRequestDetails;
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void start() {
    if (running.compareAndSet(false, true)) {
      long startTime = System.currentTimeMillis();
      try {
        onRefresh(applicationContext);
      }
      catch (Exception ex) {
        log.error("Context initialization failed", ex);
        throw ex;
      }

      if (log.isDebugEnabled()) {
        String value = isEnableLoggingRequestDetails()
                ? "shown which may lead to unsafe logging of potentially sensitive data"
                : "masked to prevent unsafe logging of potentially sensitive data";
        log.debug("enableLoggingRequestDetails='{}': request parameters and headers will be {}", isEnableLoggingRequestDetails(), value);
      }

      log.info("Completed initialization in {} ms", System.currentTimeMillis() - startTime);
    }
  }

  @Override
  public void stop() {
    destroy();
    running.set(false);
  }

}
