/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.filter;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanNameAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.context.EnvironmentAware;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.env.StandardEnvironment;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.DispatcherHandler;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;

/**
 * Abstract base class for {@link Filter} implementations, providing
 * convenient infrastructure for working with the application context.
 *
 * <p>Subclasses need to implement the {@link #doFilter(RequestContext, FilterChain)}
 * method, and may optionally override the {@link #initFilterBean()} lifecycle hook.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>{@link BeanNameAware} integration — the bean name is available via {@link #getFilterName()}</li>
 *   <li>{@link EnvironmentAware} integration — the {@link Environment} is available for property resolution</li>
 *   <li>{@link Ordered} support — subclasses can be ordered via {@link #setOrder(int)}</li>
 *   <li>Lifecycle management via {@link InitializingBean} and {@link DisposableBean}</li>
 *   <li>Logging via the {@link #logger} field</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Filter
 * @see FilterChain
 * @see DispatcherHandler
 * @since 5.0
 */
public abstract class GenericFilterBean extends OrderedSupport implements Filter, BeanNameAware, EnvironmentAware,
        EnvironmentCapable, Ordered, InitializingBean, DisposableBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private @Nullable String beanName;

  private @Nullable Environment environment;

  @Override
  public void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  /**
   * Return the name of this filter bean, or {@code null} if not set.
   */
  public @Nullable String getFilterName() {
    return this.beanName;
  }

  /**
   * Set the {@code Environment} that this filter runs in.
   * <p>Any environment set here overrides the {@link StandardEnvironment}
   * provided by default.
   * <p>This {@code Environment} object is used only for resolving placeholders in
   * resource paths passed into init-parameters for this filter. If no init-params are
   * used, this {@code Environment} can be essentially ignored.
   */
  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Return the {@link Environment} associated with this filter.
   * <p>If none specified, a default environment will be initialized via
   * {@link #createEnvironment()}.
   */
  @Override
  public Environment getEnvironment() {
    if (this.environment == null) {
      this.environment = createEnvironment();
    }
    return this.environment;
  }

  /**
   * Create and return a new {@link StandardEnvironment}.
   * <p>Subclasses may override this in order to configure the environment or
   * specialize the environment type returned.
   */
  protected Environment createEnvironment() {
    return new StandardEnvironment();
  }

  /**
   * Invoked after all bean properties have been set, before the filter
   * is first used. Delegates to {@link #initFilterBean()} by default.
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    initFilterBean();
  }

  /**
   * Subclasses may override this to perform custom initialization.
   * Called after all bean properties have been set.
   */
  protected void initFilterBean() throws Exception {
  }

  /**
   * Subclasses may override this to perform custom shutdown logic.
   */
  @Override
  public void destroy() {
  }

}
