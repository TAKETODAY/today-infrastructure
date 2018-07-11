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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.context.annotation.ActionProcessor;
import cn.taketoday.context.annotation.RestProcessor;
import cn.taketoday.context.core.Constant;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtil;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Cookie;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.HEAD;
import cn.taketoday.web.annotation.Header;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.Multipart;
import cn.taketoday.web.annotation.OPTIONS;
import cn.taketoday.web.annotation.PATCH;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.Session;
import cn.taketoday.web.annotation.TRACE;
import cn.taketoday.web.core.RequestMethod;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.interceptor.InterceptProcessor;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.mapping.RegexMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月23日 下午4:20:26
 */
@Slf4j
public final class ActionConfig implements WebConfig {

	ConfigurationFactory		configurationFactory;

	private static WebConfig	actionConfig	= new ActionConfig();

	public static WebConfig create() {
		return actionConfig;
	}

	private ActionConfig() {

	}

	@Override
	public boolean init(Object config) throws Exception {

		String scanPackage = (String) config;

		log.info("Initializing ActionHandler And ParameterResolver From Base Package [{}]", scanPackage);

		configurationFactory = ConfigurationFactory.createFactory();
		Set<Class<?>> actions = setConfiguration(scanPackage);

		configurationFactory.setActions(actions);

		return true;
	}

	/**
	 * start config
	 * 
	 * @param scanPackage
	 * @return
	 */
	public final Set<Class<?>> setConfiguration(String scanPackage) {

		Set<Class<?>> actions = ClassUtils.scanPackage(scanPackage);
		for (Class<?> clazz : actions) {
			ActionProcessor actionProcessor = clazz.getAnnotation(ActionProcessor.class);
			if (actionProcessor != null) {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				for (Method method : declaredMethods) {
					this.setActionMapping(clazz, method, false);
				}
			}

			RestProcessor restProcessor = clazz.getAnnotation(RestProcessor.class);
			if (restProcessor != null) {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				for (Method method : declaredMethods) {
					this.setActionMapping(clazz, method, true);
				}
			}
		}
		//configure end
		log.info("Interceptors conut {}", DispatchHandler.INTERCEPT_POOL.size());
		log.info("Interceptors ->  [{}]", Arrays.toString(DispatchHandler.INTERCEPT_POOL.toArray()));
		log.info("Action Mapped count [{}]", DispatchHandler.HANDLER_MAPPING_POOL.size());
		log.info("regx url Mapped [{}]", DispatchHandler.REGEX_URL);

		return actions;
	}

	/**
	 * Set Action Mapping
	 * 
	 * @param clazz
	 * @param method
	 */
	private final void setActionMapping(Class<?> clazz, Method method, boolean isRest) {

		Map<String, Set<RequestMethod>> mapping = new HashMap<>();
		ActionMapping clazzMapping = clazz.getAnnotation(ActionMapping.class);
		annotation(mapping, method);	
		
		//set HandlerMapping
		HandlerMapping requestMapping = this.createHandlerMapping(clazz, method, isRest);
		
		//do map url
		mappingUrl(requestMapping, clazzMapping, mapping);
	}

	private void annotation(Map<String, Set<RequestMethod>> requestMapping, Method method) {
		
		ActionMapping mapping = method.getAnnotation(ActionMapping.class);
		if (mapping != null) {
			String[] value = mapping.value();
			for (String url : value) {
				RequestMethod[] requestMethod_ = mapping.method();
				requestMapping.put(url, new HashSet<>(Arrays.asList(requestMethod_)));
			}
		}
		GET GET = method.getAnnotation(GET.class);
		PUT PUT = method.getAnnotation(PUT.class);
		POST POST = method.getAnnotation(POST.class);
		DELETE DELETE = method.getAnnotation(DELETE.class);
		
		HEAD HEAD = method.getAnnotation(HEAD.class);
		PATCH PATCH = method.getAnnotation(PATCH.class);
		TRACE TRACE = method.getAnnotation(TRACE.class);
		OPTIONS OPTIONS = method.getAnnotation(OPTIONS.class);
		
		if(GET != null) {
			requestMapping.put(GET.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.GET)));
		}
		if(POST != null) {
			requestMapping.put(POST.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.POST)));
		}
		if(PUT != null) {
			requestMapping.put(PUT.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.PUT)));
		}
		if(DELETE != null) {
			requestMapping.put(DELETE.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.DELETE)));
		}
		if(HEAD != null) {
			requestMapping.put(HEAD.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.HEAD)));
		}
		if(OPTIONS != null) {
			requestMapping.put(OPTIONS.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.OPTIONS)));
		}
		if(PATCH != null) {
			requestMapping.put(PATCH.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.PATCH)));
		}
		if(TRACE != null) {
			requestMapping.put(TRACE.value(), 
					new HashSet<>(Arrays.asList(RequestMethod.TRACE)));
		}
		
	}
	
	/**
	 * create url mapping
	 * @param mapping
	 * @param requestMapping
	 * @param clazzMapping
	 * @param mappingMethods
	 */
	private void mappingUrl(HandlerMapping requestMapping, ActionMapping clazzMapping, Map<String, Set<RequestMethod>> mapping_) {
		
		Set<String> urls = mapping_.keySet();

		String url = "";
		
		for (String uri : urls) {
			
			Set<RequestMethod> mappingMethods = mapping_.get(uri);
			uri = check(uri);
			
			if (clazzMapping != null) {
				uri = check(clazzMapping.value()[0]) + uri	; //class mapping only use first url
				RequestMethod[] method = clazzMapping.method();
				if(method.length != 4) {	// not default method
					mappingMethods.addAll(Arrays.asList(method));
				}
			}
			
			url = configurationFactory.contextPath +  uri;
			
			for(RequestMethod requestMethod : mappingMethods) {
				String requestMethod_ = requestMethod.toString() + Constant.REQUEST_METHOD_PREFIX;
				url = requestMethod_ + configurationFactory.contextPath +  uri;
				
				//add the mapping 
				int index = DispatchHandler.HANDLER_MAPPING_POOL.add(requestMapping);
				
				this.createRegexUrl(url, requestMapping.getHandlerMethod().getParameter(), index, requestMethod_);
				
				DispatchHandler.REQUEST_MAPPING.put(url, index);
				
				log.info("Action Mapped [{}] -> [{}] interceptors -> {}", 
						url,
						requestMapping.getActionProcessor().getName() + "." 
						+ requestMapping.getHandlerMethod().getMethod().getName() + "()",
						Arrays.toString(requestMapping.getInterceptors())
				);
			}
		}
	}

	/**
	 * 
	 * @param regexUrl
	 * @param methodParameters
	 */
	private void createRegexUrl(String regexUrl, MethodParameter[] methodParameters, int index, String requestMethod_) {
		
		if(!regexUrl.contains("*") && !regexUrl.contains("{")) {	//
			return ;
		}
		
		String methodUrl = regexUrl;
		
		regexUrl = regexUrl.replaceAll("\\*\\*", "[\\\\d|\\\\w|/]+");
		regexUrl = regexUrl.replaceAll("\\*", "[\\\\d|\\\\w]+");
		
		for (MethodParameter methodParameter : methodParameters) {
			
			if(!methodParameter.hasPathVariable()) {
				continue;
			}
			
			Class<?> parameterClass = methodParameter.getParameterClass();
			String parameterName = methodParameter.getParameterName();
			
			if(parameterClass == String.class) {
				regexUrl = regexUrl.replace("{" + parameterName + "}", "\\w+");
			} else {
				regexUrl = regexUrl.replace("{" + parameterName + "}", "\\d+");
			}
			
			String[] splitRegex = methodUrl.split(Constant.PATH_VARIABLE_REGEXP);
			String tempMethodUrl = methodUrl;
			for (String reg : splitRegex) {
				tempMethodUrl = tempMethodUrl.replaceFirst(reg, "\\\\");
			}
			
			String [] regexArr = tempMethodUrl.split("\\\\");		
			
			for (int i = 0; i < regexArr.length; i++) {
				if(regexArr[i].equals("{" + parameterName + "}")) {
					methodParameter.setPathIndex(i);
				}
			}
		}
		
		DispatchHandler.REGEX_URL.add(new RegexMapping(regexUrl, methodUrl.replace(requestMethod_, ""), index));
		
	}

	/**
	 * check uri
	 * @param uri
	 * @return
	 */
	private final String check(String uri) {
		return uri.startsWith("/") ? uri : "/" + uri;
	}


	/**
	 * 封装Mapping
	 * @param clazz
	 * @param method
	 * @param isRest
	 * @return
	 */
	private final HandlerMapping createHandlerMapping(Class<?> clazz, Method method, boolean isRest) {

		HandlerMapping requestMapping = new HandlerMapping();
		Parameter[] parameters = method.getParameters();
		String[] methodArgsNames = null;
		try {
			methodArgsNames = ClassUtils.getMethodArgsNames(clazz, method);
		} catch (IOException e) {
			log.error("get method parameters error.", e);
		}
		
		List<MethodParameter> methodParameters = new ArrayList<>();// 处理器方法参数列表
		setMethodParameter(parameters, methodParameters, methodArgsNames); // 设置 MethodParameter
		
		// 设置请求处理器
		HandlerMethod methodInfo = new HandlerMethod(method, methodParameters);
		requestMapping.setHandlerMethod(methodInfo);
		requestMapping.setActionProcessor(clazz);
		
		setInterceptor(clazz, method, isRest, requestMapping);

		return requestMapping;
	}

	/***
	 * 设置方法参数列表
	 * 
	 * @param parameters
	 * @param methodParameters
	 */
	private void setMethodParameter(Parameter[] parameters, List<MethodParameter> methodParameters, String[] methodArgsNames) {
		
		for(int i = 0; i < parameters.length; i++) {
			
			MethodParameter methodParameter = new MethodParameter();
			
			methodParameter.setParameterClass(parameters[i].getType());// 设置参数类型
			Class<?> parameterClass = parameters[i].getType();
			if (parameterClass == List.class || parameterClass == Set.class) {

				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[0]);
			} else if (parameterClass == Map.class) {

				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[1]);
			} else if (parameterClass == Optional.class) {
				
				methodParameter.setRequired(false);
				ParameterizedType paramType = (ParameterizedType) parameters[i].getParameterizedType();
				methodParameter.setGenericityClass((Class<?>) paramType.getActualTypeArguments()[0]);
			}

			setAnnotation(parameters[i], methodParameter);// 设置注解

			// 保证必须有参数名
			if (StringUtil.isEmpty(methodParameter.getParameterName())) {
				
				String parameterName = parameters[i].getName();
				if(parameterName.matches("arg[\\d]+")) {
					parameterName = methodArgsNames[i];
				}
				
				methodParameter.setParameterName(parameterName);
			}
			methodParameters.add(methodParameter); // 加入到参数列表
		}
		
	}

	/**
	 * 
	 * @param clazz
	 * @param method
	 * @param isRest
	 * @param requestMapping
	 */
	private void setInterceptor(Class<?> clazz, Method method, boolean isRest, HandlerMapping requestMapping) {

		ResponseBody responseBody = method.getAnnotation(ResponseBody.class);

		if (responseBody != null) {
			requestMapping.setResponseBody(responseBody.value());
		} else {
			requestMapping.setResponseBody(isRest);
		}

		int length = 0;
		Integer[] classInterceptor = new Integer[0];
		// 设置类拦截器
		Interceptor interceptors = clazz.getAnnotation(Interceptor.class);
		if (interceptors != null) {
			@SuppressWarnings("unchecked")
			Class<InterceptProcessor>[] values = (Class<InterceptProcessor>[]) interceptors.value();
			length = interceptors.value().length;
			classInterceptor = addInterceptors(values);
		}
		// 方法拦截器
		Interceptor interceptors_ = method.getAnnotation(Interceptor.class);

		Integer[] methodInterceptor = new Integer[0];
		if (interceptors_ != null) {
			@SuppressWarnings("unchecked")
			Class<InterceptProcessor>[] values = (Class<InterceptProcessor>[]) interceptors_.value();
			length += interceptors_.value().length;
			methodInterceptor = addInterceptors(values);
		}

		Integer[] ids = new Integer[length];

		System.arraycopy(classInterceptor, 0, ids, 0, classInterceptor.length);
		System.arraycopy(methodInterceptor, 0, ids, classInterceptor.length, methodInterceptor.length);

		requestMapping.setInterceptors(ids);
	}

	/***
	 * 设置注解
	 * 
	 * @param parameter
	 * @param methodParameter
	 */
	private void setAnnotation(Parameter parameter, MethodParameter methodParameter) {

		String parameterName = "";
		boolean required = true;
		byte annotation = Constant.ANNOTATION_NULL;

		Cookie cookie = parameter.getAnnotation(Cookie.class); // cookie
		Header header = parameter.getAnnotation(Header.class); // header
		Session session = parameter.getAnnotation(Session.class); // session

		Multipart multipart = parameter.getAnnotation(Multipart.class); // 多段式
		RequestBody requestBody = parameter.getAnnotation(RequestBody.class); // RequestBody

		RequestParam requestParam = parameter.getAnnotation(RequestParam.class); // 普通请求参数

		PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);

		if (requestParam != null) {
			parameterName = requestParam.value();
			required = requestParam.required();
		}
		if (cookie != null) {
			parameterName = cookie.value();
			annotation = Constant.ANNOTATION_COOKIE;
			required = cookie.required();
		}
		if (header != null) {
			parameterName = header.value();
			annotation = Constant.ANNOTATION_HEADER;
			required = header.required();
		}
		if (session != null) {
			parameterName = session.value();
			annotation = Constant.ANNOTATION_SESSION;
		}
		if (multipart != null) {
			parameterName = multipart.value();
			annotation = Constant.ANNOTATION_MULTIPART;
		}
		if (requestBody != null) {
			parameterName = requestBody.value();
			annotation = Constant.ANNOTATION_REQUESTBODY;
		}
		if (pathVariable != null) {
			parameterName = pathVariable.value();
			annotation = Constant.ANNOTATION_PATH_VARIABLE;
			required = true;
		}

		methodParameter.setRequired(required);
		methodParameter.setAnnotation(annotation);
		methodParameter.setParameterName(parameterName);
	}

	/***
	 * register interceptor id into interceptors pool
	 * 
	 * @param interceptors
	 * @return interceptors id
	 */
	private final Integer[] addInterceptors(Class<InterceptProcessor>[] interceptors) {

		Integer[] ids = new Integer[interceptors.length];
		int i = 0;
		for (Class<InterceptProcessor> interceptor : interceptors) {
			int size = DispatchHandler.INTERCEPT_POOL.size();
			try {
				{
					int index = DispatchHandler.INTERCEPT_POOL.indexOf(interceptor); // 获得对象的位置
					if (index >= 0) {
						ids[i++] = index;
					} else {
						InterceptProcessor newInstance = interceptor.getDeclaredConstructor().newInstance();
						if (DispatchHandler.INTERCEPT_POOL.add(newInstance)) {
							ids[i++] = size;
						} else {
							log.error("");
						}
					}
				}
			} catch (Exception e) {
				log.error("interceptor -> [{}] register error", interceptor, e);
				e.printStackTrace();
			}
		}
		return ids;
	}
	

}
