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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
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
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2018-06-25 19:47:14
 * @version 2.3.3
 */
public class DispatcherServlet implements Servlet {

	private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

	/** view resolver **/
	private final ViewResolver viewResolver;
	/** parameter resolver */
	private final ParameterResolver parameterResolver;
	/** exception resolver */
	private final ExceptionResolver exceptionResolver;
	/** context path */
	private final String contextPath;
	/** download file buffer */
	@Value(value = "#{download.buff.size}", required = false)
	private int downloadFileBuf = 10240;
	/** Action mapping registry */
	private final HandlerMappingRegistry handlerMappingRegistry;
	/** intercepter registry */
	private final HandlerInterceptorRegistry handlerInterceptorRegistry;

	private final WebApplicationContext applicationContext;

	private ServletConfig servletConfig;

	/**
	 * Default json serialize feature
	 */
	@Value(value = "#{fastjson.features}", required = false)
	private static SerializerFeature[] SERIALIZE_FEATURES = { //
			SerializerFeature.WriteMapNullValue, //
			SerializerFeature.WriteNullListAsEmpty, //
			SerializerFeature.DisableCircularReferenceDetect//
	};

	@Autowired
	public DispatcherServlet(//
			ViewResolver viewResolver, //
			ParameterResolver parameterResolver, //
			ExceptionResolver exceptionResolver, //
			HandlerMappingRegistry handlerMappingRegistry, //
			HandlerInterceptorRegistry handlerInterceptorRegistry) //
	{
		this.viewResolver = viewResolver;

		if (exceptionResolver == null) {
			throw new ConfigurationException("You must provide an 'exceptionResolver'");
		}
		this.exceptionResolver = exceptionResolver;

		if (parameterResolver == null) {
			throw new ConfigurationException("You must provide a 'parameterResolver'");
		}
		this.parameterResolver = parameterResolver;

		if (viewResolver instanceof AbstractViewResolver) {
			JSON.defaultLocale = ((AbstractViewResolver) viewResolver).getLocale();
		}
		this.handlerMappingRegistry = handlerMappingRegistry;
		this.handlerInterceptorRegistry = handlerInterceptorRegistry;

		this.applicationContext = WebUtils.getWebApplicationContext();
		this.contextPath = this.applicationContext.getServletContext().getContextPath();
	}

	@Override
	public void service(final ServletRequest servletRequest, final ServletResponse servletResponse) //
			throws ServletException //
	{
		final HttpServletRequest request = (HttpServletRequest) servletRequest;
		final HttpServletResponse response = (HttpServletResponse) servletResponse;

		// Find handler
		HandlerMapping requestMapping = null;
		try {

			// The key of handler
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
				request.setAttribute(Constant.KEY_REQUEST_URI, requestURI);
			}

			requestMapping = handlerMappingRegistry.get(index);
			// get intercepter s
			final int[] interceptors = requestMapping.getInterceptors();
			// invoke intercepter
			final HandlerInterceptorRegistry handlerInterceptorRegistry = getHandlerInterceptorRegistry();
			for (final int interceptor : interceptors) {
				if (!handlerInterceptorRegistry.get(interceptor).beforeProcess(request, response, requestMapping)) {
					log.debug("Interceptor: [{}] return false", handlerInterceptorRegistry.get(interceptor));
					return;
				}
			}

			// Handler Method
			final HandlerMethod handlerMethod = requestMapping.getHandlerMethod();
			// method parameter
			final MethodParameter[] methodParameters = handlerMethod.getParameter();
			// Handler Method parameter list
			final Object[] args = new Object[methodParameters.length];

			parameterResolver.resolveParameter(args, methodParameters, request, response);

			// log.debug("parameter list -> {}", Arrays.toString(args));
			// do dispatch
			final Object result = handlerMethod.getMethod().invoke(requestMapping.getAction(), args); // invoke

			for (final int interceptor : interceptors) {
				handlerInterceptorRegistry.get(interceptor).afterProcess(result, request, response);
			}

			switch (handlerMethod.getReutrnType())
			{
				case Constant.RETURN_VIEW : {
					resolveView(request, response, (String) result, contextPath, viewResolver);
					break;
				}
				case Constant.RETURN_STRING : {
					response.getWriter().print(result);
					break;
				}
				case Constant.RETURN_FILE : {
					WebUtils.downloadFile(request, response, (File) result, downloadFileBuf);
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
					final Object attribute = request.getAttribute(Constant.KEY_MODEL_AND_VIEW);
					if (attribute != null) {
						resolveModelAndView(request, response, (ModelAndView) attribute);
					}
					break;
				}
				case Constant.RETURN_OBJECT : {
					resolveObject(request, response, result, viewResolver, downloadFileBuf);
					break;
				}
			}
//			response.flushBuffer();
		}
		catch (Throwable exception) {
			try {
				exception = ExceptionUtils.unwrapThrowable(exception);
				exceptionResolver.resolveException(request, response, exception, requestMapping);
				log("Catch Throwable: [" + exception + "] With Msg: [" + exception.getMessage() + "]", exception);
			}
			catch (Throwable e) {
				log("Handling of [" + exception.getClass().getName() + "]  resulted in Exception: [" + e.getClass().getName() + "]", e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param resource
	 * @param contextPath
	 * @param viewResolver
	 * @throws Throwable
	 */
	static void resolveView(HttpServletRequest request, HttpServletResponse response,
			String resource, String contextPath, ViewResolver viewResolver) throws Throwable //
	{
		resolveView(request, response, resource, contextPath, viewResolver, null);
	}

	/**
	 * Resolve String type
	 *
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param result
	 *            String value
	 * @since 2.3.3
	 */
	@SuppressWarnings("unchecked")
	static void resolveView(//
			final HttpServletRequest request, //
			final HttpServletResponse response, //
			final String resource, //
			final String contextPath, //
			final ViewResolver viewResolver, //
			final Map<String, Object> dataModel) throws Throwable //
	{
		if (resource.startsWith(Constant.REDIRECT_URL_PREFIX)) {
			final String redirect = resource.replaceFirst(Constant.REDIRECT_URL_PREFIX, Constant.BLANK);
			if (redirect.startsWith(Constant.HTTP)) {
				response.sendRedirect(redirect);
				return;
			}
			response.sendRedirect(contextPath + redirect);
			return;
		}
		if (dataModel != null) {
			dataModel.forEach(request::setAttribute);
		}
		{
			final HttpSession session = request.getSession();
			final Object attribute = session.getAttribute(Constant.KEY_REDIRECT_MODEL);
			if (attribute != null) {
				((Map<String, Object>) attribute).forEach(request::setAttribute);
				session.removeAttribute(Constant.KEY_REDIRECT_MODEL);
			}
		}

		viewResolver.resolveView(resource, request, response);
	}

	/**
	 * 
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param modelAndView
	 * @throws Throwable
	 * @since 2.3.3
	 */
	void resolveModelAndView(final HttpServletRequest request, //
			final HttpServletResponse response, final ModelAndView modelAndView) throws Throwable //
	{
		if (modelAndView.noView()) {
			return;
		}
		final String contentType = modelAndView.getContentType();
		if (StringUtils.isNotEmpty(contentType)) {
			response.setContentType(contentType);
		}
		final Object view = modelAndView.getView();
		if (view instanceof String) {
			resolveView(request, response, (String) view, contextPath, viewResolver, modelAndView.getDataModel());
		}
		else if (view instanceof StringBuilder || view instanceof StringBuffer) {
			response.getWriter().print(view.toString());
		}
		else if (view instanceof File) {
			WebUtils.downloadFile(request, response, (File) view, downloadFileBuf);
		}
		else if (view instanceof RenderedImage) {
			resolveImage(response, (RenderedImage) view);
		}
		else
			resolveJsonView(response, view);
	}

	/**
	 * Resolve json view
	 * 
	 * @param response
	 *            current response
	 * @param view
	 *            view instance
	 * @throws IOException
	 */
	static void resolveJsonView(final HttpServletResponse response, final Object view) throws IOException {
		response.setContentType(Constant.CONTENT_TYPE_JSON);
		JSON.writeJSONString(response.getWriter(), view, SERIALIZE_FEATURES);
	}

	/**
	 * Resolve image
	 * 
	 * @param response
	 *            current response
	 * @param image
	 *            image instance
	 * @throws IOException
	 * @since 2.3.3
	 */
	static void resolveImage(final HttpServletResponse response, final RenderedImage image) throws IOException {
		// need set content type
		ImageIO.write(image, Constant.IMAGE_PNG, response.getOutputStream());
	}

	/**
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param result
	 *            result instance
	 * @param viewResolver
	 * @throws Throwable
	 */
	static void resolveObject(final HttpServletRequest request, final HttpServletResponse response, //
			final Object result, final ViewResolver viewResolver, final int downloadFileBuf) throws Throwable //
	{
		if (result instanceof String) {
			resolveView(request, response, (String) result, request.getContextPath(), viewResolver);
			return;
		}
		else if (result instanceof StringBuilder || result instanceof StringBuffer) {
			response.getWriter().print(result.toString());
			return;
		}
		else if (result instanceof RenderedImage) {
			resolveImage(response, (RenderedImage) result);
			return;
		}
		else if (result instanceof File) {
			WebUtils.downloadFile(request, response, (File) result, downloadFileBuf);
			return;
		}
		resolveJsonView(response, result);
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		this.servletConfig = servletConfig;
	}

	/*
	 * （非 Javadoc）
	 * 
	 * @see javax.servlet.Servlet#getServletConfig()
	 */
	@Override
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	/**
	 * @param message
	 * @param t
	 */
	final void log(String message, Throwable t) {
		applicationContext.getServletContext().log(getServletName() + ": " + message, t);
	}

	/**
	 * 
	 * @param msg
	 */
	final void log(String msg) {
		applicationContext.getServletContext().log(getServletName() + ": " + msg);
	}

	/**
	 * @return
	 */
	public String getServletName() {
		return "DispatcherServlet";
	}

	@Override
	public String getServletInfo() {
		return "DispatcherServlet, Copyright © Today & 2017 - 2018 All Rights Reserved";
	}

	@Override
	public void destroy() {

		if (applicationContext != null) {
			final State state = applicationContext.getState();

			if (state != State.CLOSING && state != State.CLOSED) {

				final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				final String msg = new StringBuffer()//
						.append("Your application destroyed at: [")//
						.append(simpleDateFormat.format(new Date()))//
						.append("] on startup date: [")//
						.append(simpleDateFormat.format(applicationContext.getStartupDate()))//
						.append("]")//
						.toString();

				applicationContext.close();
				log.info(msg);
				applicationContext.getServletContext().log(msg);
			}
		}
	}

	final HandlerInterceptorRegistry getHandlerInterceptorRegistry() {
		return this.handlerInterceptorRegistry;
	}

	final HandlerMappingRegistry getHandlerMappingRegistry() {
		return this.handlerMappingRegistry;
	}

	final String getContextPath() {
		return this.contextPath;
	}

	final int getDownloadFileBuf() {
		return this.downloadFileBuf;
	}

	final ViewResolver getViewResolver() {
		return this.viewResolver;
	}

	final ExceptionResolver getExceptionResolver() {
		return this.exceptionResolver;
	}

	final ParameterResolver getParameterResolver() {
		return this.parameterResolver;
	}
}
