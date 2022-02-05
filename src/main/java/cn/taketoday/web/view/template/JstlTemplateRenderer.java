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
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.util.Map.Entry;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.http.InternalServerException;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jstl Template
 *
 * @author TODAY <br>
 * 2018-06-26 11:53:43
 */
public class JstlTemplateRenderer extends AbstractTemplateRenderer {

  @Override
  public void render(String template, RequestContext context) throws IOException {
    HttpServletRequest request = ServletUtils.getServletRequest(context);
    HttpServletResponse response = ServletUtils.getServletResponse(context);
    try {
      request.getRequestDispatcher(prepareTemplate(template))
              .forward(request, response);
    }
    catch (ServletException e) {
      throw new InternalServerException(e);
    }
  }

  /**
   * @since 2.3.3
   */
  @Autowired
  public void afterPropertiesSet(WebServletApplicationContext context) {
    String jspServlet = "org.apache.jasper.servlet.JspServlet";

    if (!ClassUtils.isPresent(jspServlet)) {
      throw new ConfigurationException("You must provide: [" + jspServlet + "] to your application's class path");
    }

    ServletContext servletContext = context.getServletContext();

    boolean register = true;
    for (Entry<String, ? extends ServletRegistration> entry : servletContext.getServletRegistrations().entrySet()) {
      if (jspServlet.equals(entry.getValue().getClassName())) {
        register = false;
        break;
      }
    }

    // register
    if (!register) {
      Dynamic servletRegistration = servletContext.addServlet("jsp", jspServlet);
      servletRegistration.addMapping("*.jsp", "*.jspx");
    }

    LoggerFactory.getLogger(getClass())
            .info("Jstl template view renderer configure success. prefix: [{}], suffix: [{}]", prefix, suffix);
  }

}
