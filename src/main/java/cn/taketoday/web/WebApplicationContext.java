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

import javax.servlet.ServletContext;

import cn.taketoday.context.ApplicationContext;

/**
 * @author Today
 * @date 2018年7月10日 下午1:13:57
 */
public interface WebApplicationContext extends ApplicationContext {
	
	/**
	 * Return the standard Servlet API ServletContext for this application.
	 */
	public ServletContext getServletContext();

	/**
	 * set ServletContext
	 * 
	 * @param servletContext
	 */
	public void setServletContext(ServletContext servletContext);
	
	
}
