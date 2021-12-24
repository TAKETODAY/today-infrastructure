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

package cn.taketoday.jndi;

import javax.naming.NamingException;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link PropertySource} implementation that reads properties from an underlying Framework
 * {@link JndiLocatorDelegate}.
 *
 * <p>By default, the underlying {@code JndiLocatorDelegate} will be configured with its
 * {@link JndiLocatorDelegate#setResourceRef(boolean) "resourceRef"} property set to
 * {@code true}, meaning that names looked up will automatically be prefixed with
 * "java:comp/env/" in alignment with published
 * <a href="https://download.oracle.com/javase/jndi/tutorial/beyond/misc/policy.html">JNDI
 * naming conventions</a>. To override this setting or to change the prefix, manually
 * configure a {@code JndiLocatorDelegate} and provide it to one of the constructors here
 * that accepts it. The same applies when providing custom JNDI properties. These should
 * be specified using {@link JndiLocatorDelegate#setJndiEnvironment(java.util.Properties)}
 * prior to construction of the {@code JndiPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see JndiLocatorDelegate
 * @see cn.taketoday.context.ApplicationContextInitializer
 * @see cn.taketoday.web.config.WebApplicationInitializer
 * @since 4.0
 */
public class JndiPropertySource extends PropertySource<JndiLocatorDelegate> {
  private static final Logger log = LoggerFactory.getLogger(JndiPropertySource.class);

  /**
   * Create a new {@code JndiPropertySource} with the given name
   * and a {@link JndiLocatorDelegate} configured to prefix any names with
   * "java:comp/env/".
   */
  public JndiPropertySource(String name) {
    this(name, JndiLocatorDelegate.createDefaultResourceRefLocator());
  }

  /**
   * Create a new {@code JndiPropertySource} with the given name and the given
   * {@code JndiLocatorDelegate}.
   */
  public JndiPropertySource(String name, JndiLocatorDelegate jndiLocator) {
    super(name, jndiLocator);
  }

  /**
   * This implementation looks up and returns the value associated with the given
   * name from the underlying {@link JndiLocatorDelegate}. If a {@link NamingException}
   * is thrown during the call to {@link JndiLocatorDelegate#lookup(String)}, returns
   * {@code null} and issues a DEBUG-level log statement with the exception message.
   */
  @Override
  @Nullable
  public Object getProperty(String name) {
    if (getSource().isResourceRef() && name.indexOf(':') != -1) {
      // We're in resource-ref (prefixing with "java:comp/env") mode. Let's not bother
      // with property names with a colon it since they're probably just containing a
      // default value clause, very unlikely to match including the colon part even in
      // a textual property source, and effectively never meant to match that way in
      // JNDI where a colon indicates a separator between JNDI scheme and actual name.
      return null;
    }

    try {
      Object value = this.source.lookup(name);
      if (log.isDebugEnabled()) {
        log.debug("JNDI lookup for name [{}] returned: [{}]", name, value);
      }
      return value;
    }
    catch (NamingException ex) {
      if (log.isDebugEnabled()) {
        log.debug("JNDI lookup for name [{}] threw NamingException " +
                "with message: {}. Returning null.", name, ex.getMessage());
      }
      return null;
    }
  }

}
