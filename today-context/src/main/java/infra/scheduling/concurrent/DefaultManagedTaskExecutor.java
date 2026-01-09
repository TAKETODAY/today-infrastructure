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

import org.jspecify.annotations.Nullable;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.naming.NamingException;

import infra.beans.factory.InitializingBean;
import infra.jndi.JndiAccessor;
import infra.jndi.JndiLocatorDelegate;
import infra.jndi.JndiLocatorSupport;
import infra.jndi.JndiTemplate;

/**
 * JNDI-based variant of {@link ConcurrentTaskExecutor}, performing a default lookup for
 * JSR-236's "java:comp/DefaultManagedExecutorService" in a Jakarta EE/8 environment.
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link Executor} that can be found in JNDI.
 * The actual adapting to {@link jakarta.enterprise.concurrent.ManagedExecutorService}
 * happens in the base class {@link ConcurrentTaskExecutor} itself.
 *
 * @author Juergen Hoeller
 * @see jakarta.enterprise.concurrent.ManagedExecutorService
 * @since 4.0
 */
public class DefaultManagedTaskExecutor extends ConcurrentTaskExecutor implements InitializingBean {

  private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

  @Nullable
  private String jndiName = "java:comp/DefaultManagedExecutorService";

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
   * Specify a JNDI name of the {@link Executor} to delegate to,
   * replacing the default JNDI name "java:comp/DefaultManagedExecutorService".
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
    if (this.jndiName != null) {
      setConcurrentExecutor(this.jndiLocator.lookup(this.jndiName, Executor.class));
    }
  }

}
