package com.yhj.web.handler.impl;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.yhj.web.mapping.MethodParameter;
import com.yhj.web.mapping.ProcessorMethod;
import com.yhj.web.mapping.RequestMapping;

public final class ActionHandler extends AbstractActionHandler {

	private static final long serialVersionUID = 4385638260913560140L;
	
	
	/**
	 * 处理请求
	 */
	@Override
	public void doDispatch(RequestMapping mapping, HttpServletRequest request, HttpServletResponse response) throws Exception {

		long start = System.currentTimeMillis();

		long nanoStart = System.nanoTime();

		//获取处理器方法
		ProcessorMethod methodInfo = mapping.getProcessorMethod();
		//方法参数
		MethodParameter[] methodParameters = methodInfo.getParameter();
		
		final Object[] args = new Object[methodParameters.length];//处理器参数列表
		
		if(!parameterResolver.resolveParameter(args, methodParameters, request, response)){
			response.sendError(400);	//bad request
			return;
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
		} else {//返回字符串
			response.setContentType(CONTENT_TYPE_JSON);
			response.getWriter().print(JSON.toJSON(invoke));
		}

		System.out.println(invoke);

		System.out.println(System.currentTimeMillis() - start + "ms");
		System.out.println(System.nanoTime() - nanoStart + "ns");

	}

}
