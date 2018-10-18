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
package cn.taketoday.web.config;

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.ActionMappingImpl;
import cn.taketoday.web.annotation.Application;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Cookie;
import cn.taketoday.web.annotation.Header;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.Multipart;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.Session;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelMap;

import java.awt.Image;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-23 16:20:26<br>
 *         2018-08-21 20:50 change
 */
@Slf4j
@Singleton(Constant.ACTION_CONFIG)
public final class ActionConfig implements WebApplicationContextAware {

	private String contextPath;

	private WebApplicationContext applicationContext;

	public final String[] getDefaultUrlPatterns() {

		return new String[] { "*.gif", "*.jpg", "*.jpeg", "*.png", "*.swf", "*.js", "*.css", "*.ico", "*.rar", "*.zip",
				"*.txt", "*.flv", "*.mid", "*.doc", "*.ppt", "*.pdf", "*.xls", "*.mp3", "*.wma", "*.map", "*.woff2",
				"*.woff", "*.docx" };
	}

	public ActionConfig() {

	}

	public void init() throws Exception {

		log.info("Initializing ActionHandler And ParameterResolver");
		this.contextPath = applicationContext.getServletContext().getContextPath();

		setConfiguration();
	}

	/**
	 * start config
	 * 
	 * @param scanPackage
	 * @return
	 * @throws Exception
	 */
	public final void setConfiguration() throws Exception {

		Collection<Class<?>> actions = ClassUtils.getClassCache();
		for (Class<?> clazz : actions) {

			Controller actionProcessor = clazz.getAnnotation(Controller.class);
			if (actionProcessor != null) {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				for (Method method : declaredMethods) {
					this.setActionMapping(clazz, method, false);
				}
			}

			RestController restProcessor = clazz.getAnnotation(RestController.class);
			if (restProcessor != null) {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				for (Method method : declaredMethods) {
					this.setActionMapping(clazz, method, true);
				}
			}
		}
		// configure end
		log.info("Interceptors conut {}", DispatcherServlet.INTERCEPT_POOL.size());
		log.info("Interceptors ->  [{}]", Arrays.toString(DispatcherServlet.INTERCEPT_POOL.toArray()));
		log.info("Action Mapped count [{}]", DispatcherServlet.HANDLER_MAPPING_POOL.size());

		return;
	}

	/**
	 * Set Action Mapping
	 * 
	 * @param clazz
	 * @param method
	 * @throws Exception
	 */
	private final void setActionMapping(Class<?> clazz, Method method, boolean isRest) throws Exception {

		Map<String, Set<RequestMethod>> mapping = new HashMap<>();

		ActionMapping[] clazzMapping = ClassUtils.getClassAnntation(clazz, ActionMapping.class,
				ActionMappingImpl.class);
		ActionMapping clazzMapping_ = null;

		if (clazzMapping != null && clazzMapping.length != 0) {
			clazzMapping_ = clazzMapping[0];
		}

		annotation(mapping, method);
		// set HandlerMapping
		HandlerMapping requestMapping = this.createHandlerMapping(clazz, method, isRest);

		// do map url
		mappingUrl(requestMapping, clazzMapping_, mapping);
	}

	/**
	 * set request annotation
	 * 
	 * @param requestMapping
	 * @param method
	 * @throws Exception
	 */
	private void annotation(Map<String, Set<RequestMethod>> requestMapping, Method method) throws Exception {

		ActionMapping[] mapping = ClassUtils.getMethodAnntation(method, ActionMapping.class, ActionMappingImpl.class);

		if (mapping.length == 0) {
			return;
		}

		for (ActionMapping actionMapping : mapping) {
			String[] value = actionMapping.value();
			for (String url : value) {
				RequestMethod[] requestMethod_ = actionMapping.method();
				requestMapping.put(url, new HashSet<>(Arrays.asList(requestMethod_)));
			}
		}

	}

	/**
	 * create url mapping
	 * 
	 * @param mapping
	 * @param requestMapping
	 * @param clazzMapping
	 * @param mappingMethods
	 */
	private void mappingUrl(HandlerMapping requestMapping, ActionMapping clazzMapping,
			Map<String, Set<RequestMethod>> mapping_) {

		Set<String> urls = mapping_.keySet();

		String url = "";

		for (String uri : urls) {

			Set<RequestMethod> mappingMethods = mapping_.get(uri);
			uri = check(uri);

			if (clazzMapping != null) {
				uri = check(clazzMapping.value()[0]) + uri; // class mapping only use first url
				RequestMethod[] method = clazzMapping.method();
				if (method.length != 4) { // not default method
					mappingMethods.addAll(Arrays.asList(method));
				}
			}

			url = contextPath + uri; // add contextPath

			for (RequestMethod requestMethod : mappingMethods) {
				String requestMethod_ = requestMethod.toString() + Constant.REQUEST_METHOD_PREFIX;
				url = requestMethod_ + contextPath + uri;
				//
				url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

				// add the mapping
				int index = DispatcherServlet.HANDLER_MAPPING_POOL.add(requestMapping);

				this.createRegexUrl(url, requestMapping.getHandlerMethod().getParameter(), index, requestMethod_);

				DispatcherServlet.REQUEST_MAPPING.put(url, index);

				log.info(
						"Action Mapped [{}] -> [{}] interceptors -> {}", url, requestMapping.getAction() + "."
								+ requestMapping.getHandlerMethod().getMethod().getName() + "()",
						Arrays.toString(requestMapping.getInterceptors()));
			}
		}
	}

	/**
	 * Create regex Url.
	 * 
	 * @param regexUrl
	 * @param methodParameters
	 */
	private void createRegexUrl(String regexUrl, MethodParameter[] methodParameters, int index, String requestMethod_) {

		if (!regexUrl.contains("*") && !regexUrl.contains("{")) { //
			return;
		}

		String methodUrl = regexUrl;

		regexUrl = regexUrl.replaceAll(Constant.ANY_PATH, Constant.ANY_PATH_REGEXP);
		regexUrl = regexUrl.replaceAll(Constant.ONE_PATH, Constant.ONE_PATH_REGEXP);

		for (MethodParameter methodParameter : methodParameters) {

			if (!methodParameter.hasPathVariable()) {
				continue;
			}

			Class<?> parameterClass = methodParameter.getParameterClass();
			String parameterName = methodParameter.getParameterName();

			if (parameterClass == String.class) {
				regexUrl = regexUrl.replace("{" + parameterName + "}", Constant.STRING_REGEXP);
			} else {
				regexUrl = regexUrl.replace("{" + parameterName + "}", Constant.NUMBER_REGEXP);
			}

			String[] splitRegex = methodUrl.split(Constant.PATH_VARIABLE_REGEXP);
			String tempMethodUrl = methodUrl;
			for (String reg : splitRegex) {
				tempMethodUrl = tempMethodUrl.replaceFirst(reg, "\\\\");
			}

			String[] regexArr = tempMethodUrl.split("\\\\");

			for (int i = 0; i < regexArr.length; i++) {
				if (regexArr[i].equals("{" + parameterName + "}")) {
					methodParameter.setPathIndex(i);
				}
			}
			methodParameter
					.setSplitMethodUrl(methodUrl.replace(requestMethod_, "").split(Constant.PATH_VARIABLE_REGEXP));
		}
		DispatcherServlet.REGEX_URL.put(regexUrl, index);
		log.info("regx url Mapped [{}] -> [{}]", regexUrl, index);
	}

	/**
	 * check uri
	 * 
	 * @param uri
	 * @return
	 */
	private final String check(String uri) {
		return uri.startsWith("/") ? uri : "/" + uri;
	}

	/**
	 * Set Handler Mapping.
	 * 
	 * @param clazz
	 * @param method
	 * @param isRest
	 * @return
	 * @throws Exception
	 */
	private final HandlerMapping createHandlerMapping(Class<?> clazz, Method method, boolean isRest) throws Exception {

		HandlerMapping requestMapping = new HandlerMapping();
		Parameter[] parameters = method.getParameters();
		String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);

		List<MethodParameter> methodParameters = new ArrayList<>();// 处理器方法参数列表
		setMethodParameter(parameters, methodParameters, methodArgsNames); // 设置 MethodParameter

		// 设置请求处理器
		HandlerMethod methodInfo = new HandlerMethod(method, methodParameters);

		methodInfo.setReutrnType(reutrnType(method, method.getReturnType(), isRest));

		requestMapping.setHandlerMethod(methodInfo);

		Component[] component = ClassUtils.getClassAnntation(clazz, Component.class, ComponentImpl.class);

		String[] value = component[0].value();

		if (value.length == 0 || "".equals(value[0])) {
			requestMapping.setAction(clazz.getSimpleName());
		} else {
			requestMapping.setAction(value[0]);
		}

		setInterceptor(clazz, method, requestMapping);

		return requestMapping;
	}

	/**
	 * 
	 * Resolve Handler Method's return type
	 * 
	 * @param method
	 *            handler method
	 * @param reutrnType_
	 *            return type
	 * @param isRest
	 *            class rest?
	 * @return
	 */
	private byte reutrnType(Method method, Class<?> reutrnType_, boolean isRest) {
		// image
		if (Image.class.isAssignableFrom(reutrnType_)) {
			return Constant.RETURN_IMAGE;
		}
		// file
		if (File.class == reutrnType_) {
			return Constant.RETURN_FILE;
		}
		// void
		if (void.class == reutrnType_) {
			return Constant.RETURN_VOID;
		}
		// rest
		ResponseBody annotation = method.getAnnotation(ResponseBody.class);
		if (annotation != null) {
			isRest = annotation.value();
		}
		// view
		if (String.class == reutrnType_ && !isRest) {
			return Constant.RETURN_VIEW;
		}
		return Constant.RETURN_JSON;
	}

	/***
	 * set method parameter list
	 * 
	 * @param parameters
	 * @param methodParameters
	 */
	private void setMethodParameter(Parameter[] parameters, List<MethodParameter> methodParameters,
			String[] methodArgsNames) {

		for (int i = 0; i < parameters.length; i++) {

			MethodParameter methodParameter = new MethodParameter();

			Class<?> parameterClass = parameters[i].getType();
			methodParameter.setParameterClass(parameterClass);// 设置参数类型

			if (Set.class.isAssignableFrom(parameterClass)) {
				methodParameter.setParameterType(Constant.TYPE_SET);
				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[0]);
			} else if (List.class.isAssignableFrom(parameterClass)) {
				methodParameter.setParameterType(Constant.TYPE_LIST);
				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[0]);
			} else if (Map.class.isAssignableFrom(parameterClass) && ModelMap.class != parameterClass) {
				methodParameter.setParameterType(Constant.TYPE_MAP);

				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[1]);
				// Model Map
				if (methodParameter.getGenericityClass() == Object.class) {
					methodParameter.setParameterType(Constant.TYPE_MODEL);
				}
			} else if (parameterClass == Optional.class) {
				methodParameter.setRequired(false);
				methodParameter.setParameterType(Constant.TYPE_OPTIONAL);
				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[0]);
			} else if (parameterClass == int.class || parameterClass == Integer.class) {
				methodParameter.setParameterType(Constant.TYPE_INT);
			} else if (parameterClass == long.class || parameterClass == Long.class) {
				methodParameter.setParameterType(Constant.TYPE_LONG);
			} else if (parameterClass == short.class || parameterClass == Short.class) {
				methodParameter.setParameterType(Constant.TYPE_SHORT);
			} else if (parameterClass == byte.class || parameterClass == Byte.class) {
				methodParameter.setParameterType(Constant.TYPE_BYTE);
			} else if (parameterClass == double.class || parameterClass == Double.class) {
				methodParameter.setParameterType(Constant.TYPE_DOUBLE);
			} else if (parameterClass == float.class || parameterClass == Float.class) {
				methodParameter.setParameterType(Constant.TYPE_FLOAT);
			} else if (parameterClass == boolean.class || parameterClass == Boolean.class) {
				methodParameter.setParameterType(Constant.TYPE_BOOLEAN);
			} else if (parameterClass == HttpServletRequest.class) {
				methodParameter.setParameterType(Constant.TYPE_HTTP_SERVLET_REQUEST);
			} else if (parameterClass == HttpServletResponse.class) {
				methodParameter.setParameterType(Constant.TYPE_HTTP_SERVLET_RESPONSE);
			} else if (parameterClass == HttpSession.class) {
				methodParameter.setParameterType(Constant.TYPE_HTTP_SESSION);
			} else if (parameterClass == ServletContext.class) {
				methodParameter.setParameterType(Constant.TYPE_SERVLET_CONTEXT);
			} else if (parameterClass == Model.class || parameterClass == ModelMap.class) {

				methodParameter.setParameterType(Constant.TYPE_MODEL);
			} else if (parameterClass == String.class) {
				methodParameter.setParameterType(Constant.TYPE_STRING);
			}
			// array
			if (parameterClass.isArray()) {
				methodParameter.setParameterType(Constant.TYPE_ARRAY);
			}
			setAnnotation(parameters[i], methodParameter);// 设置注解

			// 保证必须有参数名
			if (StringUtils.isEmpty(methodParameter.getParameterName())) {
				String parameterName = parameters[i].getName();
				if (parameterName.matches("arg[\\d]+")) {
					parameterName = methodArgsNames[i];
				}
				methodParameter.setParameterName(parameterName);
			}
			methodParameters.add(methodParameter); // 加入到参数列表
		}

	}

	/**
	 * add intercepter to handler .
	 * 
	 * @param clazz
	 * @param method
	 * @param isRest
	 * @param requestMapping
	 */
	private void setInterceptor(Class<?> clazz, Method method, HandlerMapping requestMapping) {

		int length = 0;
		Integer[] classInterceptor = new Integer[0];
		// 设置类拦截器
		Interceptor interceptors = clazz.getAnnotation(Interceptor.class);
		if (interceptors != null) {
			@SuppressWarnings("unchecked")
			Class<HandlerInterceptor>[] values = (Class<HandlerInterceptor>[]) interceptors.value();
			length = interceptors.value().length;
			classInterceptor = addInterceptors(values);
		}
		// 方法拦截器
		Interceptor interceptors_ = method.getAnnotation(Interceptor.class);

		Integer[] methodInterceptor = new Integer[0];
		if (interceptors_ != null) {
			@SuppressWarnings("unchecked")
			Class<HandlerInterceptor>[] values = (Class<HandlerInterceptor>[]) interceptors_.value();
			length += interceptors_.value().length;
			methodInterceptor = addInterceptors(values);
		}

		Integer[] ids = new Integer[length];

		System.arraycopy(classInterceptor, 0, ids, 0, classInterceptor.length);
		System.arraycopy(methodInterceptor, 0, ids, classInterceptor.length, methodInterceptor.length);

		requestMapping.setInterceptors(ids);
	}

	/***
	 * set annotation.
	 * 
	 * @param parameter
	 * @param methodParameter
	 */
	private void setAnnotation(Parameter parameter, MethodParameter methodParameter) {

		boolean required = false;
		String parameterName = "";
		String defaultValue = null;
		byte annotation = Constant.ANNOTATION_NULL;

		Cookie cookie = parameter.getAnnotation(Cookie.class); // cookie
		Header header = parameter.getAnnotation(Header.class); // header
		Session session = parameter.getAnnotation(Session.class); // session
		Application application = parameter.getAnnotation(Application.class);

		Multipart multipart = parameter.getAnnotation(Multipart.class); // 多段式
		RequestBody requestBody = parameter.getAnnotation(RequestBody.class); // RequestBody
		RequestParam requestParam = parameter.getAnnotation(RequestParam.class); // 普通请求参数
		PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);

		if (requestParam != null) {
			required = requestParam.required();
			parameterName = requestParam.value();
			defaultValue = requestParam.defaultValue();
		}
		if (cookie != null) {
			parameterName = cookie.value();
			required = cookie.required();
			defaultValue = cookie.defaultValue();
			annotation = Constant.ANNOTATION_COOKIE;
		}
		if (header != null) {
			parameterName = header.value();
			required = header.required();
			defaultValue = header.defaultValue();
			annotation = Constant.ANNOTATION_HEADER;
		}
		if (session != null) {
			parameterName = session.value();
			annotation = Constant.ANNOTATION_SESSION;
		}
		if (multipart != null) {
			required = true;
			parameterName = multipart.value();
			annotation = Constant.ANNOTATION_MULTIPART;
		}
		if (requestBody != null) {
			required = true;
			parameterName = requestBody.value();
			annotation = Constant.ANNOTATION_REQUESTBODY;
		}

		if (pathVariable != null) {
			required = true;
			parameterName = pathVariable.value();
			annotation = Constant.ANNOTATION_PATH_VARIABLE;
		}

		if (application != null) {
			required = true;
			parameterName = application.value();
			annotation = Constant.ANNOTATION_SERVLET_CONTEXT;
		}
		methodParameter.setRequired(required)//
				.setAnnotation(annotation)//
				.setDefaultValue(defaultValue)//
				.setParameterName(parameterName);
	}

	/***
	 * Register intercepter id into intercepters pool
	 * 
	 * @param interceptors
	 * @return intercepters id
	 */
	private final Integer[] addInterceptors(Class<HandlerInterceptor>[] interceptors) {

		Integer[] ids = new Integer[interceptors.length];
		int i = 0;
		for (Class<HandlerInterceptor> interceptor : interceptors) {
			int size = DispatcherServlet.INTERCEPT_POOL.size();
			try {
				{
					int index = DispatcherServlet.INTERCEPT_POOL.indexOf(interceptor); // 获得对象的位置
					if (index >= 0) {
						ids[i++] = index;
					} else {

						BeanDefinition beanDefinition = applicationContext.getBeanDefinitionLoader()
								.getBeanDefinition(interceptor);

						Object newInstance = applicationContext.refresh(beanDefinition);

						if (DispatcherServlet.INTERCEPT_POOL.add((HandlerInterceptor) newInstance)) {
							ids[i++] = size;
						} else {
							log.error("interceptor -> [{}] register error", interceptor);
						}
					}
				}
			} catch (Exception e) {
				log.error("interceptor -> [{}] register error", interceptor, e);
			}
		}
		return ids;
	}

	@Override
	public void setWebApplicationContext(WebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
