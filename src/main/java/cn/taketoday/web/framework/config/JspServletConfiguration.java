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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework.config;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.properties.Props;

/**
 * Configuration for the server's JSP servlet.
 *
 * @author TODAY <br>
 * 2019-01-26 16:39
 */
@Props(prefix = "server.servlet.jsp")
public class JspServletConfiguration {

  private boolean enabled;

  private String name = "jsp";
  private String[] urlMappings = { "*.jsp", "*.jspx" };

  private String className = "org.apache.jasper.servlet.JspServlet";

  private Map<String, String> initParameters = new HashMap<>();

  public JspServletConfiguration() {
    this.initParameters.put("development", "false");
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getUrlMappings() {
    return urlMappings;
  }

  public void setUrlMappings(String[] urlMappings) {
    this.urlMappings = urlMappings;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Map<String, String> getInitParameters() {
    return initParameters;
  }

  public void setInitParameters(Map<String, String> initParameters) {
    this.initParameters = initParameters;
  }
}
