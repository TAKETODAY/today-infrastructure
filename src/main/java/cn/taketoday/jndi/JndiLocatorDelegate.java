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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * {@link JndiLocatorSupport} subclass with public lookup methods,
 * for convenient use as a delegate.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JndiLocatorDelegate extends JndiLocatorSupport {

  /**
   * System property that instructs to ignore a default JNDI environment, i.e.
   * to always return {@code false} from {@link #isDefaultJndiEnvironmentAvailable()}.
   * <p>The default is "false", allowing for regular default JNDI access e.g. in
   * {@link JndiPropertySource}. Switching this flag to {@code true} is an optimization
   * for scenarios where nothing is ever to be found for such JNDI fallback searches
   * to begin with, avoiding the repeated JNDI lookup overhead.
   * <p>Note that this flag just affects JNDI fallback searches, not explicitly configured
   * JNDI lookups such as for a {@code DataSource} or some other environment resource.
   * The flag literally just affects code which attempts JNDI searches based on the
   * {@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()} check: in particular,
   * {@code StandardServletEnvironment} and {@code StandardPortletEnvironment}.
   *
   * @see #isDefaultJndiEnvironmentAvailable()
   * @see JndiPropertySource
   * @since 4.0
   */
  public static final String IGNORE_JNDI_PROPERTY_NAME = "today.jndi.ignore";

  private static final boolean shouldIgnoreDefaultJndiEnvironment =
          TodayStrategies.getFlag(IGNORE_JNDI_PROPERTY_NAME);

  @Override
  public Object lookup(String jndiName) throws NamingException {
    return super.lookup(jndiName);
  }

  @Override
  public <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
    return super.lookup(jndiName, requiredType);
  }

  /**
   * Configure a {@code JndiLocatorDelegate} with its "resourceRef" property set to
   * {@code true}, meaning that all names will be prefixed with "java:comp/env/".
   *
   * @see #setResourceRef
   */
  public static JndiLocatorDelegate createDefaultResourceRefLocator() {
    JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
    jndiLocator.setResourceRef(true);
    return jndiLocator;
  }

  /**
   * Check whether a default JNDI environment, as in a Jakarta EE environment,
   * is available on this JVM.
   *
   * @return {@code true} if a default InitialContext can be used,
   * {@code false} if not
   */
  public static boolean isDefaultJndiEnvironmentAvailable() {
    if (shouldIgnoreDefaultJndiEnvironment) {
      return false;
    }
    try {
      new InitialContext().getEnvironment();
      return true;
    }
    catch (Throwable ex) {
      return false;
    }
  }

}
