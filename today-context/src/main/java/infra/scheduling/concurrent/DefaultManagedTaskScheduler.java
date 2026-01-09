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

package infra.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.naming.NamingException;

import infra.beans.factory.InitializingBean;
import infra.jndi.JndiAccessor;
import infra.jndi.JndiLocatorDelegate;
import infra.jndi.JndiLocatorSupport;
import infra.jndi.JndiTemplate;

/**
 * JNDI-based variant of {@link ConcurrentTaskScheduler}, performing a default lookup for
 * JSR-236's "java:comp/DefaultManagedScheduledExecutorService" in a Jakarta EE environment.
 * Expected to be exposed as a bean, in particular as the default lookup happens in the
 * standard {@link InitializingBean#afterPropertiesSet()} callback.
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link java.util.concurrent.ScheduledExecutorService} that can be found in JNDI.
 * The actual adapting to {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
 * happens in the base class {@link ConcurrentTaskScheduler} itself.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see jakarta.enterprise.concurrent.ManagedScheduledExecutorService
 * @since 4.0
 */
public class DefaultManagedTaskScheduler extends ConcurrentTaskScheduler implements InitializingBean {

  private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

  private String jndiName = "java:comp/DefaultManagedScheduledExecutorService";

  public DefaultManagedTaskScheduler() {
    // Executor initialization happens in afterPropertiesSet
    super(null);
  }

  /**
   * Set the JNDI template to use for JNDI lookups.
   *
   * @see JndiAccessor#setJndiTemplate
   */
  public void setJndiTemplate(JndiTemplate jndiTemplate) {
    this.jndiLocator.setJndiTemplate(jndiTemplate);
  }

  /**
   * Set the JNDI environment to use for JNDI lookups.
   *
   * @see JndiAccessor#setJndiEnvironment
   */
  public void setJndiEnvironment(Properties jndiEnvironment) {
    this.jndiLocator.setJndiEnvironment(jndiEnvironment);
  }

  /**
   * Set whether the lookup occurs in a Jakarta EE container, i.e. if the prefix
   * "java:comp/env/" needs to be added if the JNDI name doesn't already
   * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
   *
   * @see JndiLocatorSupport#setResourceRef
   */
  public void setResourceRef(boolean resourceRef) {
    this.jndiLocator.setResourceRef(resourceRef);
  }

  /**
   * Specify a JNDI name of the {@link java.util.concurrent.Executor} to delegate to,
   * replacing the default JNDI name "java:comp/DefaultManagedScheduledExecutorService".
   * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
   * to the current environment naming context if "resourceRef" is set to "true".
   *
   * @see #setConcurrentExecutor
   * @see #setResourceRef
   */
  public void setJndiName(String jndiName) {
    this.jndiName = jndiName;
  }

  @Override
  public void afterPropertiesSet() throws NamingException {
    ScheduledExecutorService executor = this.jndiLocator.lookup(this.jndiName, ScheduledExecutorService.class);
    setConcurrentExecutor(executor);
    setScheduledExecutor(executor);
  }

}
