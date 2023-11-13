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

package cn.taketoday.framework.web.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.Registration;
import jakarta.servlet.ServletContext;

/**
 * Base class for Servlet 3.0+ {@link Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @param <D> the dynamic registration result
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class DynamicRegistrationBean<D extends Registration.Dynamic> extends RegistrationBean {

  private String name;

  private boolean asyncSupported = true;

  private Map<String, String> initParameters = new LinkedHashMap<>();

  /**
   * Set the name of this registration. If not specified the bean name will be used.
   *
   * @param name the name of the registration
   */
  public void setName(String name) {
    Assert.hasLength(name, "Name must not be empty");
    this.name = name;
  }

  /**
   * Sets if asynchronous operations are supported for this registration. If not
   * specified defaults to {@code true}.
   *
   * @param asyncSupported if async is supported
   */
  public void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  /**
   * Returns if asynchronous operations are supported for this registration.
   *
   * @return if async is supported
   */
  public boolean isAsyncSupported() {
    return this.asyncSupported;
  }

  /**
   * Set init-parameters for this registration. Calling this method will replace any
   * existing init-parameters.
   *
   * @param initParameters the init parameters
   * @see #getInitParameters
   * @see #addInitParameter
   */
  public void setInitParameters(Map<String, String> initParameters) {
    Assert.notNull(initParameters, "InitParameters is required");
    this.initParameters = new LinkedHashMap<>(initParameters);
  }

  /**
   * Returns a mutable Map of the registration init-parameters.
   *
   * @return the init parameters
   */
  public Map<String, String> getInitParameters() {
    return this.initParameters;
  }

  /**
   * Add a single init-parameter, replacing any existing parameter with the same name.
   *
   * @param name the init-parameter name
   * @param value the init-parameter value
   */
  public void addInitParameter(String name, String value) {
    Assert.notNull(name, "Name is required");
    initParameters.put(name, value);
  }

  @Override
  protected final void register(String description, ServletContext servletContext) {
    D registration = addRegistration(description, servletContext);
    if (registration == null) {
      LoggerFactory.getLogger(RegistrationBean.class)
              .info("{} was not registered (possibly already registered?)", StringUtils.capitalize(description));
      return;
    }
    configure(registration);
  }

  protected abstract D addRegistration(String description, ServletContext servletContext);

  protected void configure(D registration) {
    registration.setAsyncSupported(asyncSupported);
    if (!initParameters.isEmpty()) {
      registration.setInitParameters(initParameters);
    }
  }

  /**
   * Deduces the name for this registration. Will return user specified name or fallback
   * to convention based naming.
   *
   * @param value the object used for convention based names
   * @return the deduced name
   */
  protected final String getOrDeduceName(Object value) {
    return (this.name != null) ? this.name : Conventions.getVariableName(value);
  }

}
