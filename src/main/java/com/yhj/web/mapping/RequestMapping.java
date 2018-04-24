package com.yhj.web.mapping;

import java.io.Serializable;

import com.yhj.web.reflect.MethodInfo;

/***
 * 最大的用处是用来返回json数据, 也可以返回视图
 * @author Today
 */
public final class RequestMapping implements Serializable {

	private static final long	serialVersionUID	= 1430992221283070496L;

	private String				requestUri			= null;

	private Class<?>			actionProcessor		= null;

	private MethodInfo			methodInfo			= null;

//	private 
	
	
	
	public RequestMapping() {

	}

	public RequestMapping(String requestUri) {
		this.requestUri = requestUri;
	}

	public final String getRequestUri() {
		return requestUri;
	}

	public final RequestMapping setRequestUri(String requestUri) {
		this.requestUri = requestUri;
		return this;
	}

	public final Class<?> getActionProcessor() {
		return actionProcessor;
	}

	public final MethodInfo getMethodInfo() {
		return methodInfo;
	}

	public final RequestMapping setActionProcessor(Class<?> actionProcessor) {
		this.actionProcessor = actionProcessor;
		return this;
	}

	public final RequestMapping setMethodInfo(MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
		return this;
	}

	@Override
	public String toString() {
		return "{\n\t\"requestUri\":\"" + requestUri + "\",\n\t\"actionProcessor\":\"" + actionProcessor
				+ "\",\n\t\"methodInfo\":\"" + methodInfo + "\"\n}";
	}
}
