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

package cn.taketoday.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.Executor;

import javax.naming.NamingException;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jndi.JndiLocatorDelegate;
import cn.taketoday.jndi.JndiTemplate;
import cn.taketoday.lang.Nullable;

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
   * @see cn.taketoday.jndi.JndiAccessor#setJndiTemplate
   */
  public void setJndiTemplate(JndiTemplate jndiTemplate) {
    this.jndiLocator.setJndiTemplate(jndiTemplate);
  }

  /**
   * Set the JNDI environment to use for JNDI lookups.
   *
   * @see cn.taketoday.jndi.JndiAccessor#setJndiEnvironment
   */
  public void setJndiEnvironment(Properties jndiEnvironment) {
    this.jndiLocator.setJndiEnvironment(jndiEnvironment);
  }

  /**
   * Set whether the lookup occurs in a Jakarta EE container, i.e. if the prefix
   * "java:comp/env/" needs to be added if the JNDI name doesn't already
   * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
   *
   * @see cn.taketoday.jndi.JndiLocatorSupport#setResourceRef
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
