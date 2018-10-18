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
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingPool;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.InterceptPool;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.view.ViewResolver;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-25 19:47:14
 * @version 2.0.0
 * @version 2.2.0
 */
@Slf4j
public final class DispatcherServlet extends HttpServlet implements InitializingBean, WebApplicationContextAware {

	private static final long serialVersionUID = -9011358593929556322L;

	/** view **/
	@Autowired(Constant.VIEW_RESOLVER)
	protected ViewResolver viewResolver;

	@Autowired(Constant.PARAMETER_RESOLVER)
	private ParameterResolver parameterResolver;
	/** exception Resolver */
	@Autowired(Constant.EXCEPTION_RESOLVER)
	private transient ExceptionResolver exceptionResolver;

	private String contextPath;

	@Value(value = "#{download.buff.size}", required = false)
	private int downloadFileBuf = 10240;

	// Set<RegexMapping> REGEX_URL = new LinkedHashSet<>(8);
	/** regex **/
	public static final Map<String, Integer> REGEX_URL = new HashMap<>(8);
	/** intercept pool */
	public static final InterceptPool INTERCEPT_POOL = new InterceptPool();
	/** mapping */
	public static final Map<String, Integer> REQUEST_MAPPING = new HashMap<>(8);
	/** Action mapping pool */
	public static final HandlerMappingPool HANDLER_MAPPING_POOL = new HandlerMappingPool();

	private WebApplicationContext applicationContext;

	public DispatcherServlet() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {

			viewResolver.initViewResolver(applicationContext);// view resolver init
			contextPath = applicationContext.getServletContext().getContextPath();
			parameterResolver.doInit(applicationContext);// parameter resolver init -> scan extensions
		} catch (Throwable ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response) {

		try {

			HandlerMapping requestMapping = null;
			// find handler

			final String requestURI = new StringBuilder(32)//
					.append(request.getMethod())//
					.append(Constant.REQUEST_METHOD_PREFIX)//
					.append(request.getRequestURI())//
					.toString();

			// find request mapping index
			Integer index = REQUEST_MAPPING.get(requestURI);
			if (index == null) {
				Iterator<Entry<String, Integer>> iterator = REGEX_URL.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, Integer> entry = iterator.next();
					if (requestURI.matches(entry.getKey())) {
						index = entry.getValue();
						break;
					}
				}
				if (index == null) {
					log.debug("NOT FOUND -> [{}]", requestURI);
					response.sendError(404);
					return;
				}
			}
			requestMapping = HANDLER_MAPPING_POOL.get(index);
			// get intercepter s
			Integer[] interceptors = requestMapping.getInterceptors();

			// invoke intercepter
			for (Integer interceptor : interceptors) {
				if (!INTERCEPT_POOL.get(interceptor).beforeProcess(request, response, requestMapping)) {
					log.debug("Interceptor number -> [{}] return false", interceptor);
					return;
				}
			}

			// Handler Method
			HandlerMethod handlerMethod = requestMapping.getHandlerMethod();
			// method parameter
			MethodParameter[] methodParameters = handlerMethod.getParameter();
			// Handler Method parameter list
			final Object[] args = new Object[methodParameters.length];

			parameterResolver.resolveParameter(args, methodParameters, request, response);

			// log.debug("parameter list -> {}", Arrays.toString(args));

			// do dispatch
			Object result = handlerMethod.getMethod().invoke(//
					applicationContext.getBean(requestMapping.getAction()), args//
			); // invoke

			for (Integer interceptor : interceptors) {
				INTERCEPT_POOL.get(interceptor).afterProcess(result, request, response);
			}

			switch (handlerMethod.getReutrnType())
			{
				case Constant.RETURN_VIEW :
					String url = ((String) result);
					if (!url.startsWith(Constant.REDIRECT_URL_PREFIX)) {
						viewResolver.resolveView(url, request, response);
						break;
					}
					url = url.replace(Constant.REDIRECT_URL_PREFIX, Constant.BLANK);
					if (!url.startsWith(Constant.HTTP)) {
						response.sendRedirect(contextPath + url);
						break;
					}
					response.sendRedirect(url);
					break;
				case Constant.RETURN_FILE :
					downloadFile(request, response, (File) result);
					break;
				case Constant.RETURN_IMAGE :
					// need set content type
					ImageIO.write((RenderedImage) result, Constant.IMAGE_PNG, response.getOutputStream());
					response.flushBuffer();
					break;
				case Constant.RETURN_JSON :
					response.setContentType(Constant.CONTENT_TYPE_JSON);
					response.getWriter().print(JSON.toJSONString(//
							result, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullListAsEmpty//
					));
					break;
			}
			log.debug("result -> {}", result);
		} //
		catch (Throwable exception) {
			exceptionResolver.resolveException(request, response, exception);
		}
	}

	/**
	 * Download file to client.
	 * 
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param download
	 *            file
	 * @throws IOException
	 */
	private void downloadFile(HttpServletRequest request, HttpServletResponse response, File download)
			throws IOException {

		response.setContentLengthLong(download.length());
		response.setHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
		response.setHeader(Constant.CONTENT_TYPE, Constant.APPLICATION_FORCE_DOWNLOAD);
		response.setHeader(Constant.CONTENT_DISPOSITION, Constant.ATTACHMENT_FILE_NAME
				+ URLEncoder.encode(download.getName(), Constant.DEFAULT_ENCODING) + "\"");

		try (//
				InputStream in = new FileInputStream(download.getAbsolutePath());
				OutputStream out = response.getOutputStream()) {

			byte[] b = new byte[downloadFileBuf];
			int len = 0;
			while ((len = in.read(b)) != -1) {
				out.write(b, 0, len);
			}
			out.flush();
			response.flushBuffer();
		}
	}

	@Override
	public void setWebApplicationContext(WebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
