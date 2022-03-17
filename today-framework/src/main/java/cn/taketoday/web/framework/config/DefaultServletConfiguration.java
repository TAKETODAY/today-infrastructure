/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
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
import jakarta.servlet.Servlet;

/**
 * @author TODAY <br>
 * 2019-02-03 18:06
 */
@Props(prefix = "server.servlet.default")
public class DefaultServletConfiguration {

  private boolean enable;
  private String[] urlMappings;
  private Servlet defaultServlet;

  private Map<String, String> initParameters = new HashMap<>();

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public String[] getUrlMappings() {
    return urlMappings;
  }

  public void setUrlMappings(String[] urlMappings) {
    this.urlMappings = urlMappings;
  }

  public Servlet getDefaultServlet() {
    return defaultServlet;
  }

  public void setDefaultServlet(Servlet defaultServlet) {
    this.defaultServlet = defaultServlet;
  }

  public Map<String, String> getInitParameters() {
    return initParameters;
  }

  public void setInitParameters(Map<String, String> initParameters) {
    this.initParameters = initParameters;
  }
}
