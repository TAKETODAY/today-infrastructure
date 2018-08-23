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
package cn.taketoday.web.resolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * 
 * @author Today <br>
 *         2018-07-04 18:29
 */
public interface ParameterResolver {

	/**
	 * init resolver
	 * 
	 * @param applicationContext
	 *            application context
	 * @throws ConfigurationException 
	 */
	public void doInit(WebApplicationContext applicationContext) throws ConfigurationException;

	/**
	 * supports parameter ?
	 * 
	 * @param parameter
	 *            method parameter
	 * @return supports ?
	 */
	public boolean supportsParameter(MethodParameter parameter);

	/**
	 * 
	 * @param args
	 *            method parameter instances
	 * @param parameters
	 *            parameters
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @return bad request?
	 * @throws Exception
	 */
	public boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request,
			HttpServletResponse response) throws Exception;

}
