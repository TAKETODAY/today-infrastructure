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
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jndi.JndiLocatorDelegate;
import cn.taketoday.jndi.JndiTemplate;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * JNDI-based variant of {@link CustomizableThreadFactory}, performing a default lookup
 * for JSR-236's "java:comp/DefaultManagedThreadFactory" in a Jakarta EE environment,
 * falling back to the local {@link CustomizableThreadFactory} setup if not found.
 *
 * <p>This is a convenient way to use managed threads when running in a Jakarta EE
 * environment, simply using regular local threads otherwise - without conditional
 * setup (i.e. without profiles).
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link ThreadFactory} that can be found in JNDI. Therefore,
 * the default JNDI name "java:comp/DefaultManagedThreadFactory" can be customized
 * through the {@link #setJndiName "jndiName"} bean property.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultManagedAwareThreadFactory extends CustomizableThreadFactory implements InitializingBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

  @Nullable
  private String jndiName = "java:comp/DefaultManagedThreadFactory";

  @Nullable
  private ThreadFactory threadFactory;

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
   * Specify a JNDI name of the {@link ThreadFactory} to delegate to,
   * replacing the default JNDI name "java:comp/DefaultManagedThreadFactory".
   * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
   * to the current environment naming context if "resourceRef" is set to "true".
   *
   * @see #setResourceRef
   */
  public void setJndiName(String jndiName) {
    this.jndiName = jndiName;
  }

  @Override
  public void afterPropertiesSet() throws NamingException {
    if (this.jndiName != null) {
      try {
        this.threadFactory = this.jndiLocator.lookup(jndiName, ThreadFactory.class);
      }
      catch (NamingException ex) {
        if (logger.isTraceEnabled()) {
          logger.trace("Failed to retrieve [{}] from JNDI", jndiName, ex);
        }
        logger.info("Could not find default managed thread factory in JNDI - " +
                "proceeding with default local thread factory");
      }
    }
  }

  @Override
  public Thread newThread(Runnable runnable) {
    if (this.threadFactory != null) {
      return this.threadFactory.newThread(runnable);
    }
    else {
      return super.newThread(runnable);
    }
  }

}
