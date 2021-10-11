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
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.http.HttpServletRequest;

import cn.taketoday.lang.Autowired;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.InternalServerException;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.WebServletApplicationContext;

/**
 * Jstl Template
 *
 * @author TODAY <br>
 * 2018-06-26 11:53:43
 */
public class JstlTemplateRenderer extends AbstractTemplateRenderer {

  @Override
  public void render(final String template, final RequestContext context) throws IOException {

    HttpServletRequest request = ServletUtils.getServletRequest(context);
    try {
      request.getRequestDispatcher(prepareTemplate(template))
              .forward(request, context.nativeResponse());
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
    final String jspServlet = "org.apache.jasper.servlet.JspServlet";

    if (!ClassUtils.isPresent(jspServlet)) {
      throw new ConfigurationException("You must provide: [" + jspServlet + "] to your application's class path");
    }

    final ServletContext servletContext = context.getServletContext();

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
            .info("Configuration Jstl Template View Resolver Success. prefix: [{}], suffix: [{}]", prefix, suffix);
  }

}
