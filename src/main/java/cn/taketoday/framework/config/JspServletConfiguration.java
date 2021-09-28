/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.config;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.Props;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.core.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the server's JSP servlet.
 *
 * @author TODAY <br>
 * 2019-01-26 16:39
 */
@Getter
@Setter
@Props(prefix = "server.servlet.jsp.")
@ConditionalOnClass(Constant.ENV_SERVLET)
public class JspServletConfiguration {

  private boolean enable;

  private String name = "jsp";
  private String[] urlMappings = { "*.jsp", "*.jspx" };

  private String className = "org.apache.jasper.servlet.JspServlet";

  private Map<String, String> initParameters = new HashMap<>();

  public JspServletConfiguration() {
    this.initParameters.put("development", "false");
  }

}
