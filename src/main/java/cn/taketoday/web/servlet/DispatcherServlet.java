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
package cn.taketoday.web.servlet;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;

import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-25 19:47:14
 * @version 2.0.0
 * @version 2.2.0
 */
@Slf4j
public final class DispatcherServlet extends HttpServlet implements InitializingBean {

	private static final long serialVersionUID = -9011358593929556322L;

	/** action executor */
	@Autowired(Constant.ACTION_HANDLER)
	private transient DispatchHandler<HandlerMapping> actionHandler;
	/** exception Resolver */
	@Autowired(Constant.EXCEPTION_RESOLVER)
	private transient ExceptionResolver exceptionResolver;

	public DispatcherServlet() {
		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		actionHandler.doInit();
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException{
		super.init(config);
	}

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response) {

		try {

			HandlerMapping requestMapping = null;

			// find handler

			final String requestURI = new StringBuilder(32).append(request.getMethod())
					.append(Constant.REQUEST_METHOD_PREFIX).append(request.getRequestURI()).toString();

			// find request mapping index
			Integer index = DispatchHandler.REQUEST_MAPPING.get(requestURI);
			if (index == null) {
				Iterator<RegexMapping> iterator = DispatchHandler.REGEX_URL.iterator();
				while (iterator.hasNext()) {
					RegexMapping regexMapping = iterator.next();
					if (requestURI.matches(regexMapping.getRegexUrl())) {
						request.setAttribute("REGEX", regexMapping.getMethodUrl());
						index = regexMapping.getIndex();
						break;
					}
				}
				if (index == null) {
					log.debug("NOT FOUND -> [{}]", requestURI);
					response.sendError(404);
					return;
				}
			}
			requestMapping = DispatchHandler.HANDLER_MAPPING_POOL.get(index);
			// get intercepter s
			Integer[] interceptors = requestMapping.getInterceptors();

			// invoke intercepter
			for (Integer interceptor : interceptors) {

				if (!DispatchHandler.INTERCEPT_POOL.get(interceptor).beforeProcess(request, response, requestMapping)) {
					log.debug("Interceptor number -> [{}] return false", interceptor);
					return;
				}
			}

			// do dispatch
			// actionHandler.doDispatch(requestMapping, request, response);
			Object doDispatch = actionHandler.doDispatch(requestMapping, request, response);

			for (Integer interceptor : interceptors) {
				DispatchHandler.INTERCEPT_POOL.get(interceptor).afterProcess(doDispatch, request, response);
			}

		} //
		catch (Throwable exception) {
			exceptionResolver.resolveException(request, response, exception);
		}
	}


}
