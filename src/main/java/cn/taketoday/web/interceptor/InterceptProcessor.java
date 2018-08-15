<<<<<<< HEAD
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
package cn.taketoday.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Intercept Processor process before action , process after action like log
 * @author Today
 * @date 2018年6月25日 下午8:06:11
 */
public interface InterceptProcessor {

	/**	前置拦截器
	 * @throws Exception **/
	public boolean beforeProcess(HttpServletRequest request, HttpServletResponse response) throws Exception;	

	/** 后置拦截器
	 * @throws Exception **/
	default void afterProcess(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
	}

	
=======
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
package cn.taketoday.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.mapping.HandlerMapping;

/**
 * Intercept Processor process before action , process after action.
 * 
 * @author Today <br>
 * 
 *         2018-06-25 20:06:11
 */
@FunctionalInterface
public interface InterceptProcessor {

	/**
	 * Before HandlerMethod process.
	 * 
	 * @param request
	 *            request
	 * @param response
	 *            response
	 * @param handlerMapping
	 *            request mapping
	 * @return
	 * @throws Exception
	 */
	boolean beforeProcess(HttpServletRequest request, HttpServletResponse response, HandlerMapping handlerMapping)
			throws Exception;

	/**
	 * After HandlerMethod process.
	 * 
	 * @param result
	 *            HandlerMethod returned value
	 * @param request
	 *            request
	 * @param response
	 *            response
	 * @throws Exception
	 */
	default void afterProcess(Object result, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

	}

>>>>>>> 2.2.x
}