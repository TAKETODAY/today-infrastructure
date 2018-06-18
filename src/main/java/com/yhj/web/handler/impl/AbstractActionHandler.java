package com.yhj.web.handler.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.annotation.ActionMapping;
import com.yhj.web.annotation.ActionProcessor;
import com.yhj.web.annotation.Cookie;
import com.yhj.web.annotation.Interceptor;
import com.yhj.web.annotation.RequestParam;
import com.yhj.web.annotation.ResponseBody;
import com.yhj.web.annotation.RestProcessor;
import com.yhj.web.core.Constant;
import com.yhj.web.enums.RequestMethod;
import com.yhj.web.interceptor.InterceptProcessor;
import com.yhj.web.mapping.DefaultParameterResolver;
import com.yhj.web.mapping.MethodParameter;
import com.yhj.web.mapping.ParameterResolver;
import com.yhj.web.mapping.ProcessorMethod;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.utils.ClassHelper;


public abstract class AbstractActionHandler extends AbstractHandler<RequestMapping> implements Constant {

	private static final long	serialVersionUID	= -4555228889305547313L;

	protected String			prefix				= "";
	protected String			suffix				= "";
	
	protected ParameterResolver parameterResolver	= new DefaultParameterResolver();
	
	@Override
	public abstract void doDispatch(RequestMapping mapping, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

	// 初始化
	@Override
	public final void doInit(ServletConfig config) {

		System.out.println("Initializing ActionHandler And ParameterResolver From Base Package [" + scanPackage + "]");
		suffix = config.getInitParameter("suffix");
		prefix = config.getInitParameter("prefix");
		scanPackage = config.getInitParameter("scanPackage");
		// 设置注解配置
		contextPath = config.getServletContext().getContextPath();
		Set<Class<?>> action = setConfiguration();
		
		parameterResolver.doInit(action);
		
	}

	public final Set<Class<?>> setConfiguration() {

		Set<Class<?>> actions = ClassHelper.getClassSet(scanPackage);
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
		//结束
		System.out.println("Interceptors:" + INTERCEPTOR_MAPPING.size());
		System.out.println(INTERCEPTOR_MAPPING);
		
		System.out.println("RequestMapping:" + ACTION_REQUEST_MAPPING.size());
		System.out.println(ACTION_REQUEST_MAPPING);
		return actions;
	}

	/**
	 * 设置 ActionMapping
	 * @param clazz
	 * @param method
	 */
	private final void setActionMapping(Class<?> clazz, Method method, boolean isRest) {
		
		ActionMapping mapping = method.getAnnotation(ActionMapping.class);
		if (mapping == null) {
			return;
		}

		String[] values = mapping.value();
		RequestMethod[] mappingMethods = mapping.method();

		for (int i = 0; i < values.length; i++) {
			
			for (int j = 0; j < mappingMethods.length; j++) {

				final String requestMethod = mappingMethods[j].toString();
				//class ActionMapping Annotation
				ActionMapping clazzMapping = clazz.getAnnotation(ActionMapping.class);

				// 得到包装mapping
				RequestMapping requestMapping = this.getRequestMapping(clazz, method, requestMethod, isRest);

				requestMapping.setActionProcessor(clazz);

				if (clazzMapping == null) {
					ACTION_REQUEST_MAPPING.put(requestMethod + REQUEST_METHOD_PREFIX + contextPath + values[i],
							requestMapping);
				} else {
					ACTION_REQUEST_MAPPING.put(requestMethod + REQUEST_METHOD_PREFIX + contextPath + clazzMapping.value() + values[i],
							requestMapping);
				}
			}
		}
	}

	/**
	 * 封装Mapping
	 * 
	 * @param method
	 * @param mapping
	 * @param values
	 * @param i
	 * @return
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private final RequestMapping getRequestMapping(Class<?> clazz, Method method, final String mappingMethod, boolean isRest){

		RequestMapping requestMapping = new RequestMapping();

		Class<?> returnType = method.getReturnType();
		Parameter[] parameters = method.getParameters();
		List<MethodParameter> methodParameters = new ArrayList<>();// 处理器方法参数列表
		for (Parameter parameter : parameters) {	// 封装参数
			RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
			MethodParameter methodParameter = new MethodParameter();
			if (requestParam == null) { //默认必须有参数
				methodParameter.setRequired(true);
				methodParameter.setParameterName(parameter.getName());// 获取名称

			} 
			else {	// 默认必须有参数
				if ("".equals(requestParam.value())) {
					methodParameter.setParameterName(parameter.getName());
				}
				else {
					methodParameter.setParameterName(requestParam.value());
				}
				methodParameter.setRequired(requestParam.required());// 设置是否必要
			}
			
			methodParameter.setParameterClass(parameter.getType());// 设置参数类型
			Class<?> parameterClass = parameter.getType();
			if(parameterClass == List.class || parameterClass == Set.class) {
				
				ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
				Class<?> beanClass = (Class<?>) paramType.getActualTypeArguments()[0];
				methodParameter.setGenericityClass(beanClass);
			} else if(parameterClass == Map.class) {
				
				ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
				Class<?> beanClass = (Class<?>) paramType.getActualTypeArguments()[1];
				methodParameter.setGenericityClass(beanClass);
			}
			
			parameter.getAnnotation(Cookie.class);

			methodParameters.add(methodParameter); // 加入到参数列表
		}

		ProcessorMethod methodInfo = new ProcessorMethod(method, methodParameters, returnType);

		/** 设置是否响应POJO对象 */
		ResponseBody responseBody = method.getAnnotation(ResponseBody.class);

		// 没有 ResponseBody 的响应方式是取决于是否是REST服务，有ResponseBody取决于ResponseBody的值
		if (responseBody != null) {
			requestMapping.setResponseBody(responseBody.value());
		} else {
			requestMapping.setResponseBody(isRest);
		}

		//设置类拦截器
		Interceptor interceptors = clazz.getAnnotation(Interceptor.class);
		List<String> names = new ArrayList<>();
		if(interceptors != null ) {
			@SuppressWarnings("unchecked")
			Class<InterceptProcessor>[] values = (Class<InterceptProcessor>[]) interceptors.value();
			String[] classInterceptor = setInterceptors(values);
			names = Arrays.asList(classInterceptor);
		}
		
		/**	方法拦截器*/
		Interceptor interceptors_ = method.getAnnotation(Interceptor.class);
		
		if(interceptors_ != null) {
			@SuppressWarnings("unchecked")
			Class<InterceptProcessor>[] methodInterceptor = (Class<InterceptProcessor>[]) interceptors_.value();
			String[] strings = setInterceptors(methodInterceptor);
			for(String name : strings) {
				names.add(name);
			}
		}
		
		requestMapping.setInterceptors(names.toArray(new String[0]));
		
		return requestMapping.setProcessorMethod(methodInfo);
	}

	/***
	 * 得到拦截器名并保存
	 * @param interceptors
	 * @return
	 */
	private final String[] setInterceptors(Class<InterceptProcessor>[] interceptors) {
	
		String[] names = new String[interceptors.length];
		
		int i = 0;
		for(Class<InterceptProcessor> interceptor : interceptors) {
			String name = interceptor.getName();
			names[i++] = name;
			try {
				if(!INTERCEPTOR_MAPPING.containsKey(name)) {
					INTERCEPTOR_MAPPING.put(name, interceptor.getDeclaredConstructor().newInstance());
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		return names;
	}
	
	public final static String getContextPath() {
		return contextPath;
	}

}







