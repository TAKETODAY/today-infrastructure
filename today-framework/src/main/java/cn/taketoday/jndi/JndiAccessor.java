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

import java.util.Properties;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Convenient superclass for JNDI accessors, providing "jndiTemplate"
 * and "jndiEnvironment" bean properties.
 *
 * @author Juergen Hoeller
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @since 4.0
 */
public class JndiAccessor {

  /**
   * Logger, available to subclasses.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private JndiTemplate jndiTemplate = new JndiTemplate();

  /**
   * Set the JNDI template to use for JNDI lookups.
   * <p>You can also specify JNDI environment settings via "jndiEnvironment".
   *
   * @see #setJndiEnvironment
   */
  public void setJndiTemplate(@Nullable JndiTemplate jndiTemplate) {
    this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
  }

  /**
   * Return the JNDI template to use for JNDI lookups.
   */
  public JndiTemplate getJndiTemplate() {
    return this.jndiTemplate;
  }

  /**
   * Set the JNDI environment to use for JNDI lookups.
   * <p>Creates a JndiTemplate with the given environment settings.
   *
   * @see #setJndiTemplate
   */
  public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
    this.jndiTemplate = new JndiTemplate(jndiEnvironment);
  }

  /**
   * Return the JNDI environment to use for JNDI lookups.
   */
  @Nullable
  public Properties getJndiEnvironment() {
    return this.jndiTemplate.getEnvironment();
  }

}
