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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Convenient superclass for classes that can locate any number of JNDI objects.
 * Derives from JndiAccessor to inherit the "jndiTemplate" and "jndiEnvironment"
 * bean properties.
 *
 * <p>JNDI names may or may not include the "java:comp/env/" prefix expected
 * by Jakarta EE applications when accessing a locally mapped (ENC - Environmental
 * Naming Context) resource. If it doesn't, the "java:comp/env/" prefix will
 * be prepended if the "resourceRef" property is true (the default is
 * <strong>false</strong>) and no other scheme (e.g. "java:") is given.
 *
 * @author Juergen Hoeller
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 * @since 4.0
 */
public abstract class JndiLocatorSupport extends JndiAccessor {

  /** JNDI prefix used in a Jakarta EE container. */
  public static final String CONTAINER_PREFIX = "java:comp/env/";

  private boolean resourceRef = false;

  /**
   * Set whether the lookup occurs in a Jakarta EE container, i.e. if the prefix
   * "java:comp/env/" needs to be added if the JNDI name doesn't already
   * contain it. Default is "false".
   * <p>Note: Will only get applied if no other scheme (e.g. "java:") is given.
   */
  public void setResourceRef(boolean resourceRef) {
    this.resourceRef = resourceRef;
  }

  /**
   * Return whether the lookup occurs in a Jakarta EE container.
   */
  public boolean isResourceRef() {
    return this.resourceRef;
  }

  /**
   * Perform an actual JNDI lookup for the given name via the JndiTemplate.
   * <p>If the name doesn't begin with "java:comp/env/", this prefix is added
   * if "resourceRef" is set to "true".
   *
   * @param jndiName the JNDI name to look up
   * @return the obtained object
   * @throws NamingException if the JNDI lookup failed
   * @see #setResourceRef
   */
  protected Object lookup(String jndiName) throws NamingException {
    return lookup(jndiName, null);
  }

  /**
   * Perform an actual JNDI lookup for the given name via the JndiTemplate.
   * <p>If the name doesn't begin with "java:comp/env/", this prefix is added
   * if "resourceRef" is set to "true".
   *
   * @param jndiName the JNDI name to look up
   * @param requiredType the required type of the object
   * @return the obtained object
   * @throws NamingException if the JNDI lookup failed
   * @see #setResourceRef
   */
  protected <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
    Assert.notNull(jndiName, "'jndiName' must not be null");
    String convertedName = convertJndiName(jndiName);
    T jndiObject;
    try {
      jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
    }
    catch (NamingException ex) {
      if (!convertedName.equals(jndiName)) {
        // Try fallback to originally specified name...
        if (logger.isDebugEnabled()) {
          logger.debug("Converted JNDI name [{}] not found - trying original name [{}]. ", convertedName, jndiName, ex);
        }
        jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
      }
      else {
        throw ex;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Located object with JNDI name [{}]", convertedName);
    }
    return jndiObject;
  }

  /**
   * Convert the given JNDI name into the actual JNDI name to use.
   * <p>The default implementation applies the "java:comp/env/" prefix if
   * "resourceRef" is "true" and no other scheme (e.g. "java:") is given.
   *
   * @param jndiName the original JNDI name
   * @return the JNDI name to use
   * @see #CONTAINER_PREFIX
   * @see #setResourceRef
   */
  protected String convertJndiName(String jndiName) {
    // Prepend container prefix if not already specified and no other scheme given.
    if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
      jndiName = CONTAINER_PREFIX + jndiName;
    }
    return jndiName;
  }

}
