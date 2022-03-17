/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.context.support;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.ServletContext;

/**
 * {@link FactoryBean} that fetches a specific, existing ServletContext attribute.
 * Exposes that ServletContext attribute when used as bean reference,
 * effectively making it available as named Framework bean instance.
 *
 * <p>Intended to link in ServletContext attributes that exist before
 * the startup of the Framework application context. Typically, such
 * attributes will have been put there by third-party web frameworks.
 * In a purely Framework-based web application, no such linking in of
 * ServletContext attributes will be necessary.
 *
 * <p><b>NOTE:</b>you may also use the "contextAttributes" default
 * bean which is of type Map, and dereference it using an "#{contextAttributes.myKey}"
 * expression to access a specific attribute by name.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.web.servlet.WebServletApplicationContext#CONTEXT_ATTRIBUTES_BEAN_NAME
 * @see ServletContextParameterFactoryBean
 * @since 4.0
 */
public class ServletContextAttributeFactoryBean implements FactoryBean<Object>, ServletContextAware {

  @Nullable
  private String attributeName;

  @Nullable
  private Object attribute;

  /**
   * Set the name of the ServletContext attribute to expose.
   */
  public void setAttributeName(@Nullable String attributeName) {
    this.attributeName = attributeName;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    if (this.attributeName == null) {
      throw new IllegalArgumentException("Property 'attributeName' is required");
    }
    this.attribute = servletContext.getAttribute(this.attributeName);
    if (this.attribute == null) {
      throw new IllegalStateException("No ServletContext attribute '" + this.attributeName + "' found");
    }
  }

  @Override
  @Nullable
  public Object getObject() throws Exception {
    return this.attribute;
  }

  @Override
  public Class<?> getObjectType() {
    return (this.attribute != null ? this.attribute.getClass() : null);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
