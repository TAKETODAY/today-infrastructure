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

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Convenient superclass for JNDI-based service locators,
 * providing configurable lookup of a specific JNDI resource.
 *
 * <p>Exposes a {@link #setJndiName "jndiName"} property. This may or may not
 * include the "java:comp/env/" prefix expected by Jakarta EE applications when
 * accessing a locally mapped (Environmental Naming Context) resource. If it
 * doesn't, the "java:comp/env/" prefix will be prepended if the "resourceRef"
 * property is true (the default is <strong>false</strong>) and no other scheme
 * (e.g. "java:") is given.
 *
 * <p>Subclasses may invoke the {@link #lookup()} method whenever it is appropriate.
 * Some classes might do this on initialization, while others might do it
 * on demand. The latter strategy is more flexible in that it allows for
 * initialization of the locator before the JNDI object is available.
 *
 * @author Juergen Hoeller
 * @see #setJndiName
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 * @see #lookup()
 * @since 4.0
 */
public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

  @Nullable
  private String jndiName;

  @Nullable
  private Class<?> expectedType;

  /**
   * Specify the JNDI name to look up. If it doesn't begin with "java:comp/env/"
   * this prefix is added automatically if "resourceRef" is set to "true".
   *
   * @param jndiName the JNDI name to look up
   * @see #setResourceRef
   */
  public void setJndiName(@Nullable String jndiName) {
    this.jndiName = jndiName;
  }

  /**
   * Return the JNDI name to look up.
   */
  @Nullable
  public String getJndiName() {
    return this.jndiName;
  }

  /**
   * Specify the type that the located JNDI object is supposed
   * to be assignable to, if any.
   */
  public void setExpectedType(@Nullable Class<?> expectedType) {
    this.expectedType = expectedType;
  }

  /**
   * Return the type that the located JNDI object is supposed
   * to be assignable to, if any.
   */
  @Nullable
  public Class<?> getExpectedType() {
    return this.expectedType;
  }

  @Override
  public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
    if (StringUtils.isEmpty(getJndiName())) {
      throw new IllegalArgumentException("Property 'jndiName' is required");
    }
  }

  /**
   * Perform the actual JNDI lookup for this locator's target resource.
   *
   * @return the located target object
   * @throws NamingException if the JNDI lookup failed or if the
   * located JNDI object is not assignable to the expected type
   * @see #setJndiName
   * @see #setExpectedType
   * @see #lookup(String, Class)
   */
  protected Object lookup() throws NamingException {
    String jndiName = getJndiName();
    Assert.state(jndiName != null, "No JNDI name specified");
    return lookup(jndiName, getExpectedType());
  }

}
