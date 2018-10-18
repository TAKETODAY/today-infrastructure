/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

import cn.taketoday.context.StandardApplicationContext;

import javax.servlet.ServletContext;

/**
 * 
 * @author Today <br>
 *         2018-07-10 1:16:17
 */
public class DefaultWebApplicationContext extends StandardApplicationContext implements WebApplicationContext {

	private ServletContext servletContext;

	public DefaultWebApplicationContext() {
		super();
	}

	public DefaultWebApplicationContext(ServletContext servletContext) {
		this();
		this.servletContext = servletContext;
		loadContext();
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	protected void aware(Object bean, String name) {
		super.aware(bean, name);

		if (bean instanceof ServletContextAware) {
			((ServletContextAware) bean).setServletContext(servletContext);
		}
		if (bean instanceof WebApplicationContextAware) {
			((WebApplicationContextAware) bean).setWebApplicationContext(this);
		}
	}

}
