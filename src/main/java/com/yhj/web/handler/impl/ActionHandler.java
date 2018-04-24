package com.yhj.web.handler.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.annotation.ActionMapping;
import com.yhj.web.annotation.ActionProcessor;
import com.yhj.web.annotation.RequestParam;
import com.yhj.web.core.Constant;
import com.yhj.web.enums.RequestMethod;
import com.yhj.web.handler.DispatcherHandler;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.reflect.MethodInfo;
import com.yhj.web.reflect.MethodParameter;
import com.yhj.web.utils.ClassHelper;
import com.yhj.web.utils.StringUtil;

public final class ActionHandler implements DispatcherHandler ,Constant{

	private static final long serialVersionUID = 4385638260913560140L;

	
	private static final Map<String, RequestMapping>	requestMapping			= new HashMap<>();

	private static String								contextPath				= null;

	private String										prefix					= "/WEB-INF/view";

	private String										suffix					= ".jsp";

	private String										scanPackage				= "com.yhj.core.action";

	private static final List<Class<?>>					actionProcessors		= new ArrayList<>();

	public static final String							REDIRECT_URL_PREFIX		= "redirect:";

//	public static final String							REQUEST_METHOD_PREFIX	= ":METHOD:";
	
	public static final String							REQUEST_METHOD_PREFIX	= ":";

	@Override
	public void doDispatchHandle(String uri, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		long start = System.currentTimeMillis();
		// start = System.currentTimeMillis();

		long nanoStart = System.nanoTime();

		RequestMapping mapping = requestMapping.get(request.getMethod() + REQUEST_METHOD_PREFIX + uri);

		if (mapping == null) {
			response.sendError(404);
			return;
		}

		MethodInfo methodInfo = mapping.getMethodInfo();

		//方法参数
		MethodParameter[] methodParameters = methodInfo.getParameter();

		Object[] args = new Object[methodParameters.length];

		for (int i = 0; i < methodParameters.length; i++) {
			System.out.print("\n参数：" + (i + 1)+"\t");
			final String paramName = methodParameters[i].getParameterName();

			final String requestParameter = request.getParameter(paramName);

			// 判断参数类型
			final String parameterClass = methodParameters[i].getParameterClass();
			
			System.out.print(parameterClass);
			
			if(!this.setParameter(request, response, args, i, methodParameters[i], requestParameter, parameterClass)){
				response.sendError(400);
				return;
			}
		}
		System.out.println("parameter处理时间：" + (System.currentTimeMillis() - start) + "ms");

		System.out.println(Arrays.toString(args));


		//准备执行
		Object actionProcessor = null;// 处理器
		try {
			actionProcessor = mapping.getActionProcessor().newInstance();
		} catch (InstantiationException e) {
			System.err.println("初始化异常");
		} catch (IllegalAccessException e) {
			System.err.println("初始化异常 IllegalAccessException");
		}
		
		Method method = methodInfo.getMethod();
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
		} else if(returnType.equals(Map.class)){
			
		}

		System.out.println(invoke);

		System.out.println(System.currentTimeMillis() - start + "ms");
		System.out.println(System.nanoTime() - nanoStart + "ns");

	}


	/**
	 * 装载参数
	 * @param request
	 * @param response
	 * @param args
	 * @param i
	 * @param methodParameter
	 * @param requestParameter
	 * @param parameterClass
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private final boolean setParameter(HttpServletRequest request, HttpServletResponse response, Object[] args, int i, 
				final MethodParameter methodParameter , final String requestParameter , final String parameterClass)
								throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		System.out.print("\t普通参数注入：");
		switch (parameterClass)
		{
			case "int":
			case TYPE_INTEGER:
				try {
					// 判断参数
					if (methodParameter.isRequired() && StringUtil.isEmpty(requestParameter)) {
						return false;
					}
					args[i] = Integer.parseInt(requestParameter);
				} catch (NumberFormatException e) {
					return false;
				}
				break;
			case "long":
			case TYPE_LONG:
				try {
					if (methodParameter.isRequired() && StringUtil.isEmpty(requestParameter)) {
						return false;
					}
					args[i] = Long.parseLong(requestParameter);
				} catch (NumberFormatException e) {
					return false;
				}
				break;
			case TYPE_STRING:
				if (methodParameter.isRequired() && StringUtil.isEmpty(requestParameter)) {
					return false;
				}
				args[i] = requestParameter;		
			break;
			case HTTP_SESSION: 			args[i] = request.getSession(); break;
			case HTTP_SERVLET_REQUEST: 	args[i] = request; 				break;
			case HTTP_SERVLET_RESPONSE: args[i] = response; 			break;
			
			default:
				System.out.print("\t没有普通参数注入：");
				
				Class<?> forName = Class.forName(parameterClass);
				//判断是否是数组
				if(forName.isArray()) {
					return setArrayParameter(request, args, i, methodParameter, requestParameter, parameterClass, forName);
				}
				Object newInstance = forName.newInstance();
				Enumeration<String> parameterNames = request.getParameterNames();
			
				if(!setBean(request, response, requestParameter, forName, newInstance, parameterNames)) {
					return false;
				}
				args[i] = newInstance;
				
				break;
		}
		return true;
	}


	/**
	 * 注入数组
	 * @param request
	 * @param args
	 * @param i
	 * @param methodParameter
	 * @param requestParameter
	 * @param parameterClass
	 * @param forName
	 * @return
	 */
	private boolean setArrayParameter(HttpServletRequest request, Object[] args, int i,
			final MethodParameter methodParameter, final String requestParameter, final String parameterClass, Class<?> forName) {
		
		System.out.println("数组参数注入");
		
		if(StringUtil.isEmpty(requestParameter)) {
			if (methodParameter.isRequired()) {
				System.out.println("数组必须");
				return false;
			}
			args[i] = null;
			return true;
		}
		
		
		String[] parameterValues = request.getParameterValues(methodParameter.getParameterName());
		final int length = parameterValues.length;//数组长度
		switch (parameterClass)
		{
			case TYPE_ARRAY_INT:
				try {
					int [] newInstance = new int[length];
					for(int j = 0 ; j < length ; j++)
						newInstance[j] = Integer.parseInt(parameterValues[j]);
					args[i] = newInstance;
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			case TYPE_ARRAY_INTEGER:
				try {
					Integer [] newInstance = new Integer[length];
					for(int j = 0 ; j < length ; j++) 
						newInstance[j] = Integer.parseInt(parameterValues[j]);
					args[i] = newInstance;
				} catch (NumberFormatException e) {
					return false;
				}
				return true;
			case TYPE_ARRAY_long:
				try {
					long[] newInstance = new long[length];
					for(int j = 0 ; j < length ; j++)
						newInstance[j] =  Long.parseLong(parameterValues[j]);
					args[i] = newInstance;
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			case TYPE_ARRAY_LONG:
				try {
					Long [] newInstance = new Long[length];
					for(int j = 0 ; j < length ; j++)
						newInstance[j] = Long.parseLong(parameterValues[j]);
					args[i] = newInstance;
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			case TYPE_ARRAY_STRING:
				args[i] = parameterValues;
				return true;
			default:
				
				return true;
		}
	}


	/**
	 * 注入POJO对象
	 * @param request
	 * @param response
	 * @param requestParameter
	 * @param forName
	 * @param newInstance
	 * @param parameterNames
	 * @return
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	private final boolean setBean(HttpServletRequest request, HttpServletResponse response, final String requestParameter,
			Class<?> forName, Object newInstance, Enumeration<String> parameterNames)
			throws IllegalAccessException, IOException {
		System.out.print("\tPOJO参数注入\t");
		while (parameterNames.hasMoreElements()) {
			//遍历参数
			final String parameterName =  parameterNames.nextElement();
			Field field = null;
			try {
				//寻找参数
				field = forName.getDeclaredField(parameterName);
				field.setAccessible(true);
				field.set(newInstance, request.getParameter(parameterName));
			} catch (NoSuchFieldException e) {
				System.out.println("没有：" + parameterName);
			}catch (IllegalArgumentException e) {
				switch (field.getType().getName())
				{
					case "int":
					case TYPE_INTEGER:
						try {
							field.set(newInstance, Integer.parseInt(request.getParameter(parameterName)));
						} catch (NumberFormatException ex) {
							return false;
						}
						break;
					case "long":
					case TYPE_LONG:
						try {
							field.set(newInstance,Long.parseLong(requestParameter));
						} catch (NumberFormatException exx) {
							return false;
						}
						break;
				}
			}
		}
		return true;
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
			RequestMethod[] mappingMethods = mapping.method();
			for (int i = 0; i < values.length; i++) {
				for (int j = 0; j < mappingMethods.length; j++) {
					final String requestMethod = mappingMethods[j].toString();
					RequestMapping requestMapping = getRequestMapping(method, requestMethod, values[i]);
					requestMapping.setActionProcessor(clazz);
					getRequestMapping().put(requestMethod + REQUEST_METHOD_PREFIX + contextPath + values[i],
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
	 */
	public final RequestMapping getRequestMapping(Method method, final String mappingMethod, final String value) {

		RequestMapping requestMapping = new RequestMapping(mappingMethod + REQUEST_METHOD_PREFIX + contextPath + value);

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
			methodParameter.setParameterClass(parameter.getType().getName());
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
