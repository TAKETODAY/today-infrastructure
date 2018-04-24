package com.yhj.web.handler.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.yhj.web.annotation.ActionMapping;
import com.yhj.web.annotation.ActionProcessor;
import com.yhj.web.annotation.RequestParam;
import com.yhj.web.enums.RequestMethod;
import com.yhj.web.handler.DispatcherHandler;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.reflect.MethodInfo;
import com.yhj.web.reflect.MethodParameter;
import com.yhj.web.utils.ClassHelper;

public final class ActionHandler implements DispatcherHandler {

	private static final Map<String, RequestMapping>	requestMapping		= new HashMap<>();

	private static String								contextPath			= null;

	private String										prefix				= "/WEB-INF/view";

	private String										suffix				= ".jsp";

	private String										scanPackage			= "com.yhj.core.action";

	private static final List<Class<?>>					actionProcessors	= new ArrayList<>();

	public static final String							REDIRECT_URL_PREFIX	= "redirect:";

	@Override
	public void doDispatchHandle(String uri, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		long start = System.currentTimeMillis();
		// start = System.currentTimeMillis();

		long nanoStart = System.nanoTime();

		RequestMapping mapping = requestMapping.get(uri);

		if (mapping == null) {
			response.sendError(404);
			return;
		}

		//判断请求方法是否合法
		if(!hasMethod(request.getMethod(), mapping.getMethod())) {
			response.sendError(405);
			return;
		}

		Object actionProcessor = null;// 处理器
		try {
			actionProcessor = mapping.getActionProcessor().newInstance();
		} catch (InstantiationException e) {
			System.err.println("初始化异常");
		} catch (IllegalAccessException e) {
			System.err.println("初始化异常 IllegalAccessException");
		}

		MethodInfo methodInfo = mapping.getMethodInfo();

		Method method = methodInfo.getMethod();
		MethodParameter[] parameter = methodInfo.getParameter();

		Object[] args = new Object[parameter.length];

		for (int i = 0; i < parameter.length; i++) {

			Object paramName = parameter[i].getParameterName();

			final String requestParameter = request.getParameter((String) paramName);

			// 判断参数
			if (parameter[i].isRequired() && "".equals(requestParameter)) {
				response.sendError(400);
				return;
			}

			// 判断参数类型
			Class<?> parameterClass = parameter[i].getParameterClass();
			if (parameterClass.equals(Integer.class)) {
				try {
					args[i] = Integer.parseInt(requestParameter);
				} catch (NumberFormatException e) {
					response.sendError(400);
					return;
				}
			} else if (parameterClass.equals(HttpServletRequest.class)) {
				args[i] = request;
			} else if (parameterClass.equals(HttpServletResponse.class)) {
				args[i] = response;
			} else if (parameterClass.equals(HttpSession.class)) {
				args[i] = request.getSession();
			} else {
				args[i] = requestParameter;
			}

		}
		System.out.println("parameter处理时间：" + (System.currentTimeMillis() - start) + "ms");

		System.out.println(Arrays.toString(args));

		// 参数注入
		Object invoke = method.invoke(actionProcessor, args);

		Class<?> returnType = methodInfo.getReturnType();

		if (returnType.equals(String.class)) {
			final String returnStr = ((String) invoke);
			if (returnStr.startsWith(REDIRECT_URL_PREFIX)) {
				response.sendRedirect(contextPath + returnStr.replace(REDIRECT_URL_PREFIX, ""));
			} else {
				request.getRequestDispatcher(prefix + returnStr + suffix).forward(request, response);
			}
		}

		System.out.println(invoke);

		System.out.println(System.currentTimeMillis() - start + "ms");
		System.out.println(System.nanoTime() - nanoStart + "ns");

	}

	

	/**
	 * 判断请求是否合法
	 * @param requestMethod
	 * @param mappingMethod
	 * @return
	 */
	public final boolean hasMethod(final String requestMethod, RequestMethod[] mappingMethod) {
		for (int i = 0; i < mappingMethod.length; i++) {
			if (mappingMethod[i].hasMethod(requestMethod)) {
				return true;
			}
		}
		return false;
	}
	
	// 初始化
	@Override
	public final void doInit(ServletConfig config) {
		// 设置注解配置
		contextPath = config.getServletContext().getContextPath();
		setAnnotationConfiguration();
	}

	private final void setAnnotationConfiguration() {

		Set<Class<?>> actions = ClassHelper.getClassSet(scanPackage);
		for (Class<?> clazz : actions) {
			ActionProcessor actionProcessor = clazz.getAnnotation(ActionProcessor.class);
			if (actionProcessor != null) {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				for (Method method : declaredMethods) {
					setActionMapping(clazz, method);
				}
			}
		}
		System.out.println(requestMapping);
	}

	/**
	 * 设置 ActionMapping
	 * 
	 * @param clazz
	 * @param method
	 */
	public final void setActionMapping(Class<?> clazz, Method method) {
		ActionMapping mapping = method.getAnnotation(ActionMapping.class);
		if (mapping != null) {
			String[] values = mapping.value();
			for (int i = 0; i < values.length; i++) {
				RequestMapping requestMapping = getRequestMapping(method, mapping, values, i);
				requestMapping.setActionProcessor(clazz);
				getRequestMapping().put(contextPath + values[i], requestMapping);
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
	 */
	public final RequestMapping getRequestMapping(Method method, ActionMapping mapping, String[] values, final int i) {

		RequestMapping requestMapping = new RequestMapping(contextPath + values[i], mapping.method());

		Class<?> returnType = method.getReturnType();

		Parameter[] parameters = method.getParameters();
		List<MethodParameter> methodParameters = new ArrayList<>();
		// 封装参数
		for (Parameter parameter : parameters) {

			RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
			MethodParameter methodParameter = new MethodParameter();

			if (requestParam == null) {
				// 默认是必须的
				methodParameter.setRequired(true);
				// 获取名称
				methodParameter.setParameterName(parameter.getName());
				// 默认必须有参数
			} else {
				if ("".equals(requestParam.value())) {
					methodParameter.setParameterName(parameter.getName());
				} else {
					methodParameter.setParameterName(requestParam.value());
				}
				// 设置是否必要
				methodParameter.setRequired(requestParam.required());
			}
			// 设置参数类型
			methodParameter.setParameterClass(parameter.getType());
			// 加入到参数列表
			methodParameters.add(methodParameter);
		}

		MethodInfo methodInfo = new MethodInfo(method, methodParameters, returnType);

		return requestMapping.setMethodInfo(methodInfo);
	}

	public static Map<String, RequestMapping> getRequestMapping() {
		return requestMapping;
	}

	public static String getContextPath() {
		return contextPath;
	}

	public static List<Class<?>> getActionProcessors() {
		return actionProcessors;
	}

}
