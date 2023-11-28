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

package cn.taketoday.web.servlet.support;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * {@link FactoryBean} that retrieves a specific ServletContext init parameter
 * (that is, a "context-param" defined in {@code web.xml}).
 * Exposes that ServletContext init parameter when used as bean reference,
 * effectively making it available as named Framework bean instance.
 *
 * <p><b>NOTE:</b> you may also use the "contextParameters" default
 * bean which is of type Map, and dereference it using an "#{contextParameters.myKey}"
 * expression to access a specific parameter by name.
 *
 * @author Juergen Hoeller
 * @see WebApplicationContext#CONTEXT_PARAMETERS_BEAN_NAME
 * @see ServletContextAttributeFactoryBean
 * @since 4.0
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {

  @Nullable
  private String initParamName;

  @Nullable
  private String paramValue;

  /**
   * Set the name of the ServletContext init parameter to expose.
   */
  public void setInitParamName(@Nullable String initParamName) {
    this.initParamName = initParamName;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    if (this.initParamName == null) {
      throw new IllegalArgumentException("initParamName is required");
    }
    this.paramValue = servletContext.getInitParameter(this.initParamName);
    if (this.paramValue == null) {
      throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
    }
  }

  @Override
  @Nullable
  public String getObject() {
    return this.paramValue;
  }

  @Override
  public Class<String> getObjectType() {
    return String.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
