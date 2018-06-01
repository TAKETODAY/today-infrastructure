package com.yhj.web.handler.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.reflect.MethodParameter;
import com.yhj.web.reflect.ProcessorMethod;
import com.yhj.web.utils.NumberUtils;
import com.yhj.web.utils.StringUtil;

public final class ActionHandler extends AbstractActionHandler {

	
	private static final long serialVersionUID = 4385638260913560140L;
	
	/**
	 * 处理请求
	 */
	@Override
	public void doDispatch(RequestMapping mapping, HttpServletRequest request, HttpServletResponse response) throws Exception {

		long start = System.currentTimeMillis();
		// start = System.currentTimeMillis();

		long nanoStart = System.nanoTime();

		//获取处理器方法
		ProcessorMethod methodInfo = mapping.getProcessorMethod();
		//方法参数
		MethodParameter[] methodParameters = methodInfo.getParameter();
		
		
		final Object[] args = new Object[methodParameters.length];//处理器参数列表

		
		for (int i = 0; i < methodParameters.length; i++) 	{
			
			MethodParameter methodParameter = methodParameters[i];

			Class<?> parameterClass = methodParameter.getParameterClass();
			
			System.out.print("\n参数：" + (i + 1)+"\t");
			//判断参数类型
			System.out.print(parameterClass);

			if(!this.setParameter(request, response, args, i, methodParameter, methodParameter.getParameterName(), parameterClass)){
				response.sendError(400);
				return;
			}
		}

		System.out.println("parameter处理时间：" + (System.currentTimeMillis() - start) + "ms");

		System.out.println(Arrays.toString(args));

		//准备执行+
		Object actionProcessor = null;// 处理器
		try {
			actionProcessor = mapping.getActionProcessor().newInstance();
		} catch (InstantiationException e) {
			System.err.println("初始化异常");
		} catch (IllegalAccessException e) {
			System.err.println("初始化异常 IllegalAccessException");
		}
		
		Method method = methodInfo.getMethod();//得到具体的处理器方法
		Object invoke = method.invoke(actionProcessor, args);// 参数注入并执行

		Class<?> returnType = methodInfo.getReturnType();

		if (returnType.equals(String.class) && !mapping.isResponseBody()) {//返回视图
			final String returnStr = ((String) invoke);
			if (returnStr.startsWith(REDIRECT_URL_PREFIX)) {
				response.sendRedirect(contextPath + returnStr.replace(REDIRECT_URL_PREFIX, ""));
			} else {
				request.getRequestDispatcher(prefix + returnStr + suffix).forward(request, response);
			}
		} else {//返回字符串\
			response.setContentType(CONTENT_TYPE_JSON);
			response.getWriter().print(JSON.toJSON(invoke));
		}

		System.out.println(invoke);

		System.out.println(System.currentTimeMillis() - start + "ms");
		System.out.println(System.nanoTime() - nanoStart + "ns");

	}


	/**
	 * 装载参数
	 * @param request
	 * @param response
	 * @param arg
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
	private final boolean setParameter(HttpServletRequest request, HttpServletResponse response, Object args[],final int i, 
				final MethodParameter methodParameter , final String paramName , final Class<?> parameterClass)
								throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		System.out.print("\t普通参数注入：");
		
		final String requestParameter = request.getParameter(paramName);//得到
		
		switch (parameterClass.getName())
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
				
				//判断是否是数组
				if(parameterClass.isArray()) {
					System.out.println("数组参数注入");
					
					if(StringUtil.isEmpty(requestParameter)) {
						if (methodParameter.isRequired()) {
							System.out.println("数组必须");
							return false;
						}
						args[i] = null;
						return true;
					}
					try {
//						StringToArrayFactory arrayFactory = new StringToArrayFactory();
						
//						args[i] = arrayFactory.getConverter(forName).doConvert(request.getParameterValues(methodParameter.getParameterName()));
						
						args[i] = NumberUtils.parseArray(request.getParameterValues(methodParameter.getParameterName()), parameterClass);
						return true;
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
					
				}
//				Enumeration<String> parameterNames = request.getParameterNames();
				args[i] = parameterClass.newInstance();
				
				System.out.println(request.getParameterMap());
				
				if(!setBean(request, parameterClass, args[i], request.getParameterNames())) {
					return false;
				}
				break;
		}
		return true;
	}



	/**
	 * 注入POJO对象
	 * @param request
	 * @param response
	 * @param requestParameter
	 * @param forName
	 * @param bean
	 * @param parameterNames
	 * @return
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	private final boolean setBean(HttpServletRequest request , Class<?> forName, Object bean, Enumeration<String> parameterNames)
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
				
				if(!set(request, parameterName, bean, field)) {
					return false;
				}
			} catch (NoSuchFieldException e) {
				System.out.println("没有：" + parameterName);
				return false;
			}
		}
		return true;
	}


	private final boolean set(HttpServletRequest request, final String parameterName, Object bean, Field field) throws IllegalAccessException {
		
		switch (field.getType().getName()) 
		{
			case "int":
			case TYPE_INTEGER:
				try {
					field.set(bean, Integer.parseInt(request.getParameter(parameterName)));
				} catch (NumberFormatException ex) {
					return false;
				}
				return true;
			case "long":
			case TYPE_LONG:
				try {
					field.set(bean, Long.parseLong(request.getParameter(parameterName)));
				} catch (NumberFormatException exx) {
					return false;
				}
				return true;
			default:
				field.set(bean, request.getParameter(parameterName));
				return true;
		}
	}

	
	
	
	
	
	
	

}
