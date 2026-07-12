
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

package infra.web.filter;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;

/**
 * Proxy for a standard Filter, delegating to a Infra-managed bean that
 * implements the Filter interface. Supports a "targetBeanName" filter init-param
 * in {@code web.xml}, specifying the name of the target bean in the Infra
 * application context.
 *
 * <p>This approach is particularly useful for Filter implementations with complex
 * setup needs, allowing to apply the full Infra bean definition machinery to
 * Filter instances. Alternatively, consider standard Filter setup in combination
 * with looking up service beans from the Infra root application context.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @see #setTargetBeanName
 * @see #DelegatingFilterProxy(Filter)
 * @see #DelegatingFilterProxy(String, ApplicationContext)
 * @since 5.0
 */
public class DelegatingFilterProxy extends GenericFilterBean implements ApplicationContextAware {

  private @Nullable ApplicationContext applicationContext;

  private @Nullable String targetBeanName;

  private volatile @Nullable Filter delegate;

  private final Lock delegateLock = new ReentrantLock();

  /**
   * Create a new {@code DelegatingFilterProxy}. For traditional use in {@code web.xml}.
   *
   * @see #setTargetBeanName(String)
   */
  public DelegatingFilterProxy() {
  }

  /**
   * Create a new {@code DelegatingFilterProxy} with the given {@link Filter} delegate.
   * Bypasses entirely the need for interacting with Infra application context,
   * specifying the {@linkplain #setTargetBeanName target bean name}, etc.
   * <p>For use with instance-based registration of filters.
   *
   * @param delegate the {@code Filter} instance that this proxy will delegate to and
   * manage the lifecycle for (must not be {@code null}).
   * @see #doFilter(HttpContext, FilterChain)
   * @see #invokeDelegate(Filter, HttpContext, FilterChain)
   * @see #destroy()
   * @see #setEnvironment(Environment)
   */
  public DelegatingFilterProxy(Filter delegate) {
    Assert.notNull(delegate, "Delegate Filter is required");
    this.delegate = delegate;
  }

  /**
   * Create a new {@code DelegatingFilterProxy} that will retrieve the named target
   * bean from the given Infra {@code ApplicationContext}.
   * <p>For use with instance-based registration of filters.
   * <p>The target bean must implement the standard Filter interface.
   * <p>This proxy's {@code Environment} will be inherited from the given
   * {@code ApplicationContext}.
   *
   * @param targetBeanName name of the target filter bean in the Infra application
   * context (must not be {@code null}).
   * @param wac the application context from which the target filter will be retrieved;
   * if {@code null}
   * @see #setEnvironment(Environment)
   */
  public DelegatingFilterProxy(String targetBeanName, @Nullable ApplicationContext wac) {
    Assert.hasText(targetBeanName, "Target Filter bean name must not be null or empty");
    setTargetBeanName(targetBeanName);
    this.applicationContext = wac;
    if (wac != null) {
      setEnvironment(wac.getEnvironment());
    }
  }

  /**
   * Set the name of the target bean in the Infra application context.
   * The target bean must implement the standard Web Filter interface.
   * <p>By default, the {@code filter-name} as specified for the
   * DelegatingFilterProxy in {@code web.xml} will be used.
   */
  public void setTargetBeanName(@Nullable String targetBeanName) {
    this.targetBeanName = targetBeanName;
  }

  /**
   * Return the name of the target bean in the Infra application context.
   */
  protected @Nullable String getTargetBeanName() {
    return this.targetBeanName;
  }

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  protected void initFilterBean() {
    this.delegateLock.lock();
    try {
      if (this.delegate == null) {
        // If no target bean name specified, use filter name.
        if (this.targetBeanName == null) {
          this.targetBeanName = getFilterName();
        }
        ApplicationContext wac = applicationContext;
        if (wac != null) {
          this.delegate = initDelegate(wac);
        }
      }
    }
    finally {
      this.delegateLock.unlock();
    }
  }

  @Override
  public void doFilter(HttpContext http, FilterChain chain) throws Exception {
    // Lazily initialize the delegate if necessary.
    Filter delegateToUse = this.delegate;
    if (delegateToUse == null) {
      this.delegateLock.lock();
      try {
        delegateToUse = this.delegate;
        if (delegateToUse == null) {
          ApplicationContext wac = applicationContext;
          if (wac == null) {
            throw new IllegalStateException("No ApplicationContext set");
          }
          delegateToUse = initDelegate(wac);
        }
        this.delegate = delegateToUse;
      }
      finally {
        this.delegateLock.unlock();
      }
    }

    // Let the delegate perform the actual doFilter operation.
    invokeDelegate(delegateToUse, http, chain);
  }

  /**
   * Initialize the Filter delegate, defined as bean the given Infra
   * application context.
   * <p>The default implementation fetches the bean from the application context
   * and calls the standard {@code Filter.init} method on it, passing
   * in the FilterConfig of this Filter proxy.
   *
   * @param wac the root application context
   * @return the initialized delegate Filter
   * @see #getTargetBeanName()
   */
  protected Filter initDelegate(ApplicationContext wac) {
    String targetBeanName = getTargetBeanName();
    Assert.state(targetBeanName != null, "No target bean name set");
    return wac.getBean(targetBeanName, Filter.class);
  }

  /**
   * Actually invoke the delegate Filter with the given request and response.
   *
   * @param delegate the delegate Filter
   * @param request the current HTTP request
   * @param filterChain the current FilterChain
   */
  protected void invokeDelegate(Filter delegate, HttpContext request, FilterChain filterChain) throws Exception {
    delegate.doFilter(request, filterChain);
  }

}
