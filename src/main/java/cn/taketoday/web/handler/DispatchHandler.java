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
package cn.taketoday.web.handler;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.web.mapping.HandlerMappingPool;
import cn.taketoday.web.mapping.InterceptPool;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.mapping.ViewMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author Today <br>
 *         2018-06-30 18:24:30
 */
public interface DispatchHandler<T> {

	// Set<RegexMapping> REGEX_URL = new LinkedHashSet<>(8);
	/** **/
	Set<RegexMapping>			REGEX_URL				= new HashSet<>(8);
	/** view 视图映射池 */
	Map<String, ViewMapping>	VIEW_REQUEST_MAPPING	= new HashMap<>(8);
	/** Action mapping pool */
	HandlerMappingPool			HANDLER_MAPPING_POOL	= new HandlerMappingPool();
	/** mapping */
	Map<String, Integer>		REQUEST_MAPPING			= new HashMap<>(8);
	/** 拦截器池 */
	InterceptPool				INTERCEPT_POOL			= new InterceptPool();

	/**
	 * Initialize view resolver and parameter resolver.
	 * 
	 * @throws ConfigurationException 
	 */
	public void doInit() throws ConfigurationException;

	/**
	 * Resolve request and invoke HandlerMathod.
	 * 
	 * @param mapping
	 *            the HandlerMapping
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @throws Exception
	 */
	public Object doDispatch(T mapping, HttpServletRequest request, HttpServletResponse response) throws Exception;

}