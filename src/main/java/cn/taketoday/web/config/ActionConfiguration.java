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
package cn.taketoday.web.config;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.config.initializer.OrderedInitializer;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.ModelAttributes;
import cn.taketoday.web.ui.RedirectModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-23 16:20:26<br>
 *         2018-08-21 20:50 change
 */
@Slf4j
@Singleton(Constant.ACTION_CONFIG)
public class ActionConfiguration implements OrderedInitializer, WebApplicationContextAware, DisposableBean {

	@Getter
	private String contextPath;

	private Map<String, Integer> regexUrls = new HashMap<>();
	private Map<String, Integer> requestMappings = new HashMap<>();

	@Autowired(Constant.HANDLER_MAPPING_REGISTRY)
	public HandlerMappingRegistry handlerMappingRegistry;

	@Autowired(Constant.HANDLER_INTERCEPTOR_REGISTRY)
	public HandlerInterceptorRegistry handlerInterceptorRegistry;

	private WebApplicationContext applicationContext;

	private BeanDefinitionLoader beanDefinitionLoader;

	private Properties variables;

	public ActionConfiguration() {

	}

	/**
	 * Build {@link HandlerMapping}
	 * 
	 * @param beanClass
	 *            bean class
	 * @throws Exception
	 *             if any {@link Exception} occurred
	 * @since 2.3.7
	 */
	public void buildHandlerMapping(final Class<?> beanClass) throws Exception {

		final Collection<AnnotationAttributes> controllerAttributes = //
				ClassUtils.getAnnotationAttributes(beanClass, Controller.class);

		if (controllerAttributes.isEmpty()) {
			return;
		}

		final Collection<ActionMapping> controllerMappings = //
				ClassUtils.getAnnotation(beanClass, ActionMapping.class); // find mapping on class

		final Set<String> namespaces = new HashSet<>(4, 1.0f); // name space
		final Set<RequestMethod> methodsOnClass = new HashSet<>(8, 1.0f); // method

		if (!controllerMappings.isEmpty()) {
			ActionMapping controllerMapping = controllerMappings.iterator().next(); // get first mapping on class
			for (String value : controllerMapping.value()) {
				namespaces.add(checkUrl(value));
			}
			Collections.addAll(methodsOnClass, controllerMapping.method());
		}

		final boolean restful = controllerAttributes.iterator().next().getBoolean("restful"); // restful

		for (Method method : beanClass.getDeclaredMethods()) {
			this.setActionMapping(beanClass, method, namespaces, methodsOnClass, restful);
		}
	}

	/**
	 * start config
	 * 
	 * @throws Exception
	 */
	void startConfiguration() throws Exception {
		// @since 2.3.3
		for (Entry<String, BeanDefinition> entry : applicationContext.getBeanDefinitionsMap().entrySet()) {
			buildHandlerMapping(entry.getValue().getBeanClass());
		}
	}

	/**
	 * Set Action Mapping
	 * 
	 * @param beanClass
	 * @param method
	 * @param namespaces
	 * @param methodsOnClass
	 * @param restful
	 * @throws Exception
	 */
	private void setActionMapping(Class<?> beanClass, Method method, //
			Set<String> namespaces, Set<RequestMethod> methodsOnClass, boolean restful) throws Exception //
	{
		final Collection<AnnotationAttributes> annotationAttributes = //
				ClassUtils.getAnnotationAttributes(method, ActionMapping.class);

		if (!annotationAttributes.isEmpty()) {
			// do mapping url
			this.mappingHandlerMapping(this.createHandlerMapping(beanClass, method, restful), // create HandlerMapping
					namespaces, methodsOnClass, annotationAttributes);
		}
	}

	/**
	 * 
	 * @param elements
	 * @return
	 * @off
	 */
	@SafeVarargs
	@SuppressWarnings("serial")
	static <E> Set<E> newHashSet(E... elements) {
		return new HashSet<E>(8, 1.0f) {{
			for (E element : elements) {
				add(element);
			}
		}};
	}
	//@on

	/**
	 * Mapping given HandlerMapping to {@link HandlerMappingRegistry}
	 * 
	 * @param handlerMapping
	 *            current {@link HandlerMapping}
	 * @param namespaces
	 *            path on class
	 * @param classRequestMethods
	 *            methods on class
	 * @param annotationAttributes
	 *            {@link ActionMapping} Attributes
	 */
	private void mappingHandlerMapping(HandlerMapping handlerMapping, Set<String> namespaces, //
			Set<RequestMethod> classRequestMethods, Collection<AnnotationAttributes> annotationAttributes) //
	{
		HandlerMethod handlerMethod = handlerMapping.getHandlerMethod();

		for (AnnotationAttributes handlerMethodMapping : annotationAttributes) {
			boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?

			Set<RequestMethod> requestMethods = //
					newHashSet(handlerMethodMapping.getAttribute("method", RequestMethod[].class));

			requestMethods.addAll(classRequestMethods);

			for (String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
				// splice urls and request methods
				for (RequestMethod requestMethod : requestMethods) {
					if (exclude || namespaces.isEmpty()) {
						doMapping(handlerMapping, handlerMethod, checkUrl(urlOnMethod), requestMethod);
						continue;
					}
					for (String namespace : namespaces) {
						doMapping(handlerMapping, handlerMethod, namespace + checkUrl(urlOnMethod), requestMethod);
					}
				}
			}
		}
	}

	/**
	 * Mapping to {@link HandlerMappingRegistry}
	 * 
	 * @param handlerMapping
	 * @param handlerMethod
	 * @param urlOnMethod
	 * @param requestMethod
	 */
	private void doMapping(HandlerMapping handlerMapping, HandlerMethod handlerMethod, String urlOnMethod, RequestMethod requestMethod) {

		urlOnMethod = requestMethod.name() //
				+ contextPath //
				+ ContextUtils.resolvePlaceholder(variables, urlOnMethod); // GET/blog/users/1 GET/blog/#{key}/1

		// add the mapping
		int index = handlerMappingRegistry.add(handlerMapping); // index of handler method
		if (!doMappingPathVariable(urlOnMethod, //
				handlerMethod.getParameter(), handlerMethod.getMethod(), index, requestMethod.name())) {

			this.requestMappings.put(urlOnMethod, index);
			log.info(//
					"Mapped [{}] -> [{}] interceptors -> {}", //
					urlOnMethod, handlerMethod.getMethod(), Arrays.toString(handlerMapping.getInterceptors())//
			);
		}
	}

	/**
	 * Create path variable mapping.
	 * 
	 * @param regexUrl
	 *            regex url
	 * @param methodParameters
	 */
	private boolean doMappingPathVariable(String regexUrl, //
			MethodParameter[] methodParameters, Method method, int index, String requestMethod_) //
	{
		if (!regexUrl.contains("*") && !regexUrl.contains("{")) { //
			return false; // not a path variable
		}

		String methodUrl = regexUrl; // copy regex url

		regexUrl = regexUrl.replaceAll(Constant.ANY_PATH, Constant.ANY_PATH_REGEXP);
		regexUrl = regexUrl.replaceAll(Constant.ONE_PATH, Constant.ONE_PATH_REGEXP);

		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			MethodParameter methodParameter = methodParameters[i];
			if (!methodParameter.hasPathVariable()) {
				continue;
			}
			Class<?> parameterClass = methodParameter.getParameterClass();

			PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
			if (pathVariable == null) {
				throw new ConfigurationException(//
						"You must specify a @PathVariable Like this: [public String update(@PathVariable int id, ..) {...}]"//
				);
			}
			String regex = pathVariable.regex(); // customize regex
			if (StringUtils.isEmpty(regex)) {
				if (parameterClass == String.class) {
					regex = Constant.STRING_REGEXP;
				}
				else {
					regex = Constant.NUMBER_REGEXP;
				}
			}

			String parameterName = methodParameter.getParameterName();
			regexUrl = regexUrl.replace("{" + parameterName + "}", regex);

			String[] splitRegex = methodUrl.split(Constant.PATH_VARIABLE_REGEXP);
			String tempMethodUrl = methodUrl;

			for (String reg : splitRegex) {
				tempMethodUrl = tempMethodUrl.replaceFirst(reg, "\\\\");
			}

			String[] regexArr = tempMethodUrl.split("\\\\");
			for (int j = 0; j < regexArr.length; j++) {
				if (regexArr[j].equals("{" + parameterName + "}")) {
					methodParameter.setPathIndex(j);
				}
			}
			methodParameter.setSplitMethodUrl(//
					methodUrl.replace(requestMethod_, Constant.BLANK).split(Constant.PATH_VARIABLE_REGEXP)//
			);
		}
		this.regexUrls.put(regexUrl, index);
		log.info("Mapped [{}] -> [{}]", regexUrl, method);
		return true;
	}

	/**
	 * Check Url, format url like :
	 * 
	 * <pre>
	 * users	-> /users
	 * /users	-> /users
	 * </pre>
	 * 
	 * @param url
	 * @return
	 */
	static String checkUrl(String url) {
		return StringUtils.isEmpty(url) ? Constant.BLANK : (url.startsWith("/") ? url : "/" + url);
	}

	/**
	 * Set Handler Mapping.
	 * 
	 * @param beanCLass
	 * @param method
	 * @param restful
	 * @return
	 * @throws Exception
	 */
	private HandlerMapping createHandlerMapping(Class<?> beanClass, Method method, boolean restful) throws Exception {

		HandlerMapping handlerMapping = new HandlerMapping();
		Parameter[] parameters = method.getParameters();

		List<MethodParameter> methodParameters = new ArrayList<>();// 处理器方法参数列表
		setMethodParameter(parameters, methodParameters, method); // 设置 MethodParameter

		// 设置请求处理器
		HandlerMethod handlerMethod = new HandlerMethod(method, methodParameters);

		handlerMethod.setReutrnType(returnType(method, method.getReturnType(), restful));
		handlerMapping.setHandlerMethod(handlerMethod);

		final Object bean = applicationContext.getBean(beanClass);
		if (bean == null) {
			throw new ConfigurationException(//
					"An unexpected exception occurred: [Can't get bean with given type: [{}]]", //
					beanClass.getName()//
			);
		}
		handlerMapping.setAction(bean);
		setInterceptor(beanClass, method, handlerMapping);

		return handlerMapping;
	}

	/**
	 * 
	 * Resolve Handler Method's return type
	 * 
	 * @param method
	 *            handler method
	 * @param reutrnType
	 *            return type
	 * @param restful
	 *            class rest?
	 * @return
	 */
	private byte returnType(Method method, Class<?> returnType, boolean restful) {
		if (Object.class == returnType) { // @since 2.3.3
			return Constant.RETURN_OBJECT;
		}
		// image
		if (Image.class.isAssignableFrom(returnType) || RenderedImage.class.isAssignableFrom(returnType)) {
			return Constant.RETURN_IMAGE;
		}
		// file
		if (File.class == returnType) {
			return Constant.RETURN_FILE;
		}
		// void
		if (void.class == returnType) {
			return Constant.RETURN_VOID;
		}
		// rest
		ResponseBody annotation = method.getAnnotation(ResponseBody.class);
		if (annotation != null) {
			restful = annotation.value();
		}

		if (ModelAndView.class == returnType) {// @since v2.3.3
			return Constant.RETURN_MODEL_AND_VIEW;
		}
		if (returnType == StringBuilder.class || returnType == StringBuffer.class) {
			return Constant.RETURN_STRING;
		}
		// view
		if (String.class == returnType) {
			if (restful) { // @since v2.3.3
				return Constant.RETURN_STRING;
			}
			return Constant.RETURN_VIEW;
		}
		return Constant.RETURN_JSON;
	}

	/***
	 * set method parameter list
	 * 
	 * @param parameters
	 * @param methodParameters
	 * @throws IOException
	 */
	private void setMethodParameter(Parameter[] parameters, //
			List<MethodParameter> methodParameters, Method method) throws IOException //
	{
		String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);

		for (int i = 0; i < parameters.length; i++) {
			methodParameters.add(createMethodParameter(parameters[i], methodArgsNames[i]));
		}
	}

	private MethodParameter createMethodParameter(Parameter parameter, String methodArgsName) {

		Class<?> genericityClass = null;
		String parameterName = Constant.BLANK;
		byte parameterType = Constant.TYPE_OTHER;
		Class<?> parameterClass = parameter.getType();
		// annotation
		boolean required = false;
		String defaultValue = null;
		byte annotation = Constant.ANNOTATION_NULL;

		if (Set.class.isAssignableFrom(parameterClass)) {
			parameterType = Constant.TYPE_SET;
			ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
			genericityClass = (Class<?>) paramType.getActualTypeArguments()[0];
		}
		else if (List.class.isAssignableFrom(parameterClass)) {
			parameterType = Constant.TYPE_LIST;
			ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
			genericityClass = (Class<?>) paramType.getActualTypeArguments()[0];
		}
		else if (Map.class.isAssignableFrom(parameterClass) && !Model.class.isAssignableFrom(parameterClass)) {
			parameterType = Constant.TYPE_MAP;
			ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
			genericityClass = (Class<?>) paramType.getActualTypeArguments()[1];
			// Model Map
			if (genericityClass == Object.class) {
				parameterType = Constant.TYPE_MODEL;
			}
		}
		else if (parameterClass == int.class || parameterClass == Integer.class) {
			parameterType = Constant.TYPE_INT;
		}
		else if (parameterClass == long.class || parameterClass == Long.class) {
			parameterType = Constant.TYPE_LONG;
		}
		else if (parameterClass == short.class || parameterClass == Short.class) {
			parameterType = Constant.TYPE_SHORT;
		}
		else if (parameterClass == byte.class || parameterClass == Byte.class) {
			parameterType = Constant.TYPE_BYTE;
		}
		else if (parameterClass == double.class || parameterClass == Double.class) {
			parameterType = Constant.TYPE_DOUBLE;
		}
		else if (parameterClass == float.class || parameterClass == Float.class) {
			parameterType = Constant.TYPE_FLOAT;
		}
		else if (parameterClass == boolean.class || parameterClass == Boolean.class) {
			parameterType = Constant.TYPE_BOOLEAN;
		}
		else if (parameterClass == HttpServletRequest.class) {
			parameterType = Constant.TYPE_HTTP_SERVLET_REQUEST;
		}
		else if (parameterClass == HttpServletResponse.class) {
			parameterType = Constant.TYPE_HTTP_SERVLET_RESPONSE;
		}
		else if (parameterClass == HttpSession.class) {
			parameterType = Constant.TYPE_HTTP_SESSION;
		}
		else if (parameterClass == ServletContext.class) {
			parameterType = Constant.TYPE_SERVLET_CONTEXT;
		}
		else if (parameterClass == Model.class || parameterClass == ModelAttributes.class) {
			parameterType = Constant.TYPE_MODEL;
		}
		else if (parameterClass == String.class) {
			parameterType = Constant.TYPE_STRING;
		}
		else if (MultipartFile.class.isAssignableFrom(parameterClass)) {
			parameterType = Constant.TYPE_MULTIPART_FILE;
		}
		else if (RedirectModel.class.isAssignableFrom(parameterClass)) {
			parameterType = Constant.TYPE_REDIRECT_MODEL;
		}
		else if (ModelAndView.class == parameterClass) {
			parameterType = Constant.TYPE_MODEL_AND_VIEW;
		}
		else if (Reader.class == parameterClass || BufferedReader.class == parameterClass) {
			parameterType = Constant.TYPE_READER;
		}
		else if (Writer.class == parameterClass || PrintWriter.class == parameterClass) {
			parameterType = Constant.TYPE_WRITER;
		}
		else if (InputStream.class == parameterClass) {
			parameterType = Constant.TYPE_INPUT_STREAM;
		}
		else if (OutputStream.class == parameterClass) {
			parameterType = Constant.TYPE_OUT_STREAM;
		}
		else if (Locale.class == parameterClass) {
			parameterType = Constant.TYPE_LOCALE;
		}
		else if (Principal.class == parameterClass) {
			parameterType = Constant.TYPE_PRINCIPAL;
		}

		// array
		if (parameterClass.isArray()) {
			parameterType = Constant.TYPE_ARRAY;
			if (parameterClass.getComponentType() == MultipartFile.class) {
				parameterType += Constant.TYPE_MULTIPART_FILE;
			}
		}
		// multipart
		if (genericityClass == MultipartFile.class) {
			parameterType += Constant.TYPE_MULTIPART_FILE;
		}

		Collection<RequestParam> requestParams = ClassUtils.getAnnotation(parameter, RequestParam.class);

		if (!requestParams.isEmpty()) {
			RequestParam requestParam = requestParams.iterator().next();
			annotation = requestParam.type();
			required = requestParam.required();
			parameterName = requestParam.value();
			defaultValue = requestParam.defaultValue();
		}

		if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameter.getType())) {
			defaultValue = "0"; // fix default value
		}

		if (StringUtils.isEmpty(parameterName)) {
			// use method parameter name
//			parameterName = parameter.getName();
//			if (parameterName.matches("arg[\\d]+")) {
			parameterName = methodArgsName;
//			}
		}

		return new MethodParameter(required, parameterName, parameterClass, //
				genericityClass, annotation, defaultValue, parameterType);
	}

	/**
	 * Add intercepter to handler .
	 * 
	 * @param controllerClass
	 *            controller class
	 * @param action
	 *            method
	 * @param requestMapping
	 *            mapping of request
	 */
	@SuppressWarnings("unchecked")
	private void setInterceptor(Class<?> controllerClass, Method action, HandlerMapping requestMapping) {

		int length = 0;
		int[] classInterceptor = new int[0];
		// 设置类拦截器
		Interceptor controllerInterceptors = controllerClass.getAnnotation(Interceptor.class);
		if (controllerInterceptors != null) {
			Class<? extends HandlerInterceptor>[] value = controllerInterceptors.value();
			length = value.length;
			classInterceptor = addInterceptors((Class<HandlerInterceptor>[]) value);
		}
		// 方法拦截器
		Interceptor actionInterceptors = action.getAnnotation(Interceptor.class);

		int[] methodInterceptor = new int[0];
		if (actionInterceptors != null) {
			Class<? extends HandlerInterceptor>[] value = actionInterceptors.value();
			length += value.length;
			methodInterceptor = addInterceptors(value);
		}

		int[] ids = new int[length];

		System.arraycopy(classInterceptor, 0, ids, 0, classInterceptor.length);
		System.arraycopy(methodInterceptor, 0, ids, classInterceptor.length, methodInterceptor.length);

		requestMapping.setInterceptors(ids);
	}

	/***
	 * Register intercepter id into intercepters registry
	 * 
	 * @param interceptors
	 * @return
	 */
	private final int[] addInterceptors(Class<? extends HandlerInterceptor>[] interceptors) {

		final HandlerInterceptorRegistry handlerInterceptorRegistry = this.handlerInterceptorRegistry;

		int[] ids = new int[interceptors.length];
		int i = 0;
		for (Class<? extends HandlerInterceptor> interceptor : interceptors) {
			int size = handlerInterceptorRegistry.size();
			try {
				{
					int index = handlerInterceptorRegistry.indexOf(interceptor); // 获得对象的位置
					if (index >= 0) {
						ids[i++] = index;
					}
					else {

						Object newInstance = applicationContext//
								.refresh(beanDefinitionLoader.createBeanDefinition(interceptor));

						if (handlerInterceptorRegistry.add((HandlerInterceptor) newInstance)) {
							ids[i++] = size;
						}
						else {
							throw new ConfigurationException("Interceptor: [{}] register error", interceptor.getName());
						}
					}
				}
			}
			catch (Exception e) {
				throw new ConfigurationException("Interceptor: [{}] register error", interceptor.getName(), e);
			}
		}
		return ids;
	}

	@Override
	public void setWebApplicationContext(WebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.contextPath = applicationContext.getServletContext().getContextPath();
		ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
		this.variables = environment.getProperties();
		this.beanDefinitionLoader = environment.getBeanDefinitionLoader();
	}

	@Override
	public void destroy() throws Exception {
		if (regexUrls != null) {
			this.regexUrls.clear();
		}
		if (requestMappings != null) {
			this.requestMappings.clear();
		}
	}

	@Override
	public void onStartup(ServletContext servletContext) throws Throwable {

		log.info("Initializing Controllers");
		startConfiguration();
		handlerMappingRegistry.setRegexMappings(new HashMap<>(regexUrls))//
				.setRequestMappings(new HashMap<>(requestMappings));
	}

	public void reBuiltControllers() throws Throwable {

		log.info("rebuilding Controllers");

		regexUrls.clear();
		requestMappings.clear();

		startConfiguration();
		handlerMappingRegistry.setRegexMappings(new HashMap<>(regexUrls))//
				.setRequestMappings(new HashMap<>(requestMappings));
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE - 99;
	}

}
