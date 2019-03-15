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
package cn.taketoday.web.servlet;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ViewResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2019-01-03 22:55
 */
@Slf4j
@WebDebugMode
@Singleton(Constant.DISPATCHER_SERVLET)
public class DebugDispatcherServlet extends DispatcherServlet {

	@Autowired
	public DebugDispatcherServlet(//
			ViewResolver viewResolver, //
			ParameterResolver parameterResolver, //
			ExceptionResolver exceptionResolver,
			HandlerMappingRegistry handlerMappingRegistry, //
			HandlerInterceptorRegistry handlerInterceptorRegistry) //
	{
		super(viewResolver, parameterResolver, exceptionResolver, handlerMappingRegistry, handlerInterceptorRegistry);
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException {

		final long start = System.currentTimeMillis();

		final HttpServletRequest request = (HttpServletRequest) servletRequest;
		final HttpServletResponse response = (HttpServletResponse) servletResponse;

		// find handler
		HandlerMapping requestMapping = null;
		try {

			String requestURI = request.getMethod() + request.getRequestURI();

			final HandlerMappingRegistry handlerMappingRegistry = getHandlerMappingRegistry();
			Integer index = handlerMappingRegistry.getIndex(requestURI);
			if (index == null) {
				// path variable
				requestURI = StringUtils.decodeUrl(requestURI);// decode
				for (RegexMapping regexMapping : handlerMappingRegistry.getRegexMappings()) {
					if (requestURI.matches(regexMapping.getRegex())) {
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

			request.setAttribute(Constant.KEY_REQUEST_URI, requestURI);
			requestMapping = handlerMappingRegistry.get(index);
			// get intercepter s
			final int[] interceptors = requestMapping.getInterceptors();
			// invoke intercepter
			final HandlerInterceptorRegistry handlerInterceptorRegistry = getHandlerInterceptorRegistry();
			for (Integer interceptor : interceptors) {
				if (!handlerInterceptorRegistry.get(interceptor).beforeProcess(request, response, requestMapping)) {
					log.debug("Before HandlerMethod Process: [{}] return false", handlerInterceptorRegistry.get(interceptor));
					return;
				}
			}

			// Handler Method
			HandlerMethod handlerMethod = requestMapping.getHandlerMethod();
			// method parameter
			MethodParameter[] methodParameters = handlerMethod.getParameter();
			// Handler Method parameter list
			final Object[] args = new Object[methodParameters.length];

			getParameterResolver().resolveParameter(args, methodParameters, request, response);

			log.debug("Parameter list: {}", Arrays.toString(args));
			// do dispatch
			Object result = handlerMethod.getMethod().invoke(requestMapping.getAction(), args); // invoke

			for (Integer interceptor : interceptors) {
				HandlerInterceptor handlerInterceptor = handlerInterceptorRegistry.get(interceptor);
				handlerInterceptor.afterProcess(result, request, response);
				log.debug("After HandlerMethod Process: [{}]", handlerInterceptor);
			}

			switch (handlerMethod.getReutrnType())
			{
				case Constant.RETURN_VIEW : {
					resolveView(request, response, (String) result, getContextPath(), getViewResolver());
					break;
				}
				case Constant.RETURN_STRING : {
					response.getWriter().print(result);
					break;
				}
				case Constant.RETURN_FILE : {
					WebUtils.downloadFile(request, response, (File) result, getDownloadFileBuf());
					break;
				}
				case Constant.RETURN_IMAGE : {
					// need set content type
					ImageIO.write((RenderedImage) result, Constant.IMAGE_PNG, response.getOutputStream());
					break;
				}
				case Constant.RETURN_JSON : {
					resolveJsonView(response, result);
					break;
				}
				case Constant.RETURN_MODEL_AND_VIEW : {
					resolveModelAndView(request, response, (ModelAndView) result);
					break;
				}
				case Constant.RETURN_VOID : {
					Object attribute = request.getAttribute(Constant.KEY_MODEL_AND_VIEW);
					if (attribute != null) {
						resolveModelAndView(request, response, (ModelAndView) attribute);
					}
					break;
				}
				case Constant.RETURN_OBJECT : {
					resolveObject(request, response, result, getViewResolver(), getDownloadFileBuf());
					break;
				}
			}
			log.debug("Process [{}] takes: [{}]ms, with result: [{}]", requestURI, (System.currentTimeMillis() - start), result);
		}
		catch (Throwable exception) {
			try {
				exception = ExceptionUtils.unwrapThrowable(exception);
				getExceptionResolver().resolveException(request, response, exception, requestMapping);
				log("Catch Throwable: [" + exception + "] With Msg: [" + exception.getMessage() + "]", exception);
			}
			catch (Throwable e) {
				log("Handling of [" + exception.getClass().getName() + "]  resulted in Exception: [" + e.getClass().getName() + "]", e);
				throw new ServletException(e);
			}
		}
	}
}
