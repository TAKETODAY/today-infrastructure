/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.view;

import java.util.Map.Entry;

import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.utils.ClassUtils;

/**
 * 
 * @author Today <br>
 *         2018-06-26 11:53:43
 */
public class JstlViewResolver extends AbstractViewResolver implements InitializingBean {

	@Override
	public void resolveView(String templateName, //
			HttpServletRequest request, HttpServletResponse response) throws Throwable//
	{
		request.getRequestDispatcher(build(templateName))//
				.forward(request, response);
	}

	private final String build(String templateName) {
		return new StringBuilder(32)//
				.append(prefix)//
				.append(templateName)//
				.append(suffix)//
				.toString();
	}

	/**
	 * 
	 * @see cn.taketoday.context.factory.InitializingBean#afterPropertiesSet()
	 * @since 2.3.3
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		final String jspServlet = "org.apache.jasper.servlet.JspServlet";

		if (!ClassUtils.isPresent(jspServlet)) {
			throw new ConfigurationException("You must provide: [] to your application's class path", jspServlet);
		}

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
		LoggerFactory.getLogger(getClass()).info("Configuration Jstl View Resolver Success.");
	}

}
