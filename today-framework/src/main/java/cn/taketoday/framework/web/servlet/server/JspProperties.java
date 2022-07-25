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

package cn.taketoday.framework.web.servlet.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the server's JSP servlet.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 4.0
 */
public class JspProperties {

  /**
   * Class name of the servlet to use for JSPs. If registered is true and this class is
   * on the classpath then it will be registered.
   */
  private String className = "org.apache.jasper.servlet.JspServlet";

  private Map<String, String> initParameters = new HashMap<>();

  /**
   * Whether the JSP servlet is registered.
   */
  private boolean registered = true;

  public JspProperties() {
    this.initParameters.put("development", "false");
  }

  /**
   * Return the class name of the servlet to use for JSPs. If {@link #getRegistered()
   * registered} is {@code true} and this class is on the classpath then it will be
   * registered.
   *
   * @return the class name of the servlet to use for JSPs
   */
  public String getClassName() {
    return this.className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * Return the init parameters used to configure the JSP servlet.
   *
   * @return the init parameters
   */
  public Map<String, String> getInitParameters() {
    return this.initParameters;
  }

  public void setInitParameters(Map<String, String> initParameters) {
    this.initParameters = initParameters;
  }

  /**
   * Return whether the JSP servlet is registered.
   *
   * @return {@code true} to register the JSP servlet
   */
  public boolean getRegistered() {
    return this.registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

}
