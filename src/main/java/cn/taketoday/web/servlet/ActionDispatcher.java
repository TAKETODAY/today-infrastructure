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

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.web.core.Constant;
import cn.taketoday.web.core.WebApplicationContext;
import cn.taketoday.web.handler.ActionHandler;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.interceptor.InterceptProcessor;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月25日 下午7:47:14
 * @version 2.0.0
 */
@Slf4j
public final class ActionDispatcher extends HttpServlet {

	private static final long				serialVersionUID	= -9011358593929556322L;

	/** action executor */
	private DispatchHandler<HandlerMapping>	actionHandler;
	/** exception Resolver */
	private ExceptionResolver				exceptionResolver;

	public ActionDispatcher(WebApplicationContext applicationContext) throws NoSuchBeanDefinitionException {

		actionHandler = applicationContext.getBean(Constant.ACTION_HANDLER, ActionHandler.class);
		exceptionResolver = applicationContext.getBean(Constant.EXCEPTION_RESOLVER, ExceptionResolver.class);
		actionHandler.doInit(applicationContext);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");
		HandlerMapping requestMapping = null;

		// enter handler
		try {
			// find request mapping
			final String requestURI = request.getMethod() + Constant.REQUEST_METHOD_PREFIX + request.getRequestURI();

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
			// get interceptors
			Integer[] interceptors = requestMapping.getInterceptors();

			// invoke
			for (Integer interceptor : interceptors) {
				InterceptProcessor interceptProcessor = DispatchHandler.INTERCEPT_POOL.get(interceptor);

				if (!interceptProcessor.beforeProcess(request, response)) {
					log.debug("interceptor number [{}] return false", interceptor);
					return;
				}
			}
			// do dispatch
			actionHandler.doDispatch(requestMapping, request, response);
			// interceptProcessor.afterProcess(request, response);

		} catch (Exception exception) {
			exceptionResolver.resolveException(request, response, exception);
		}
	}

}
