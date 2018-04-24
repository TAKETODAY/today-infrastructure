package com.yhj.web.mapping;

import java.io.Serializable;

import com.yhj.web.enums.RequestMethod;
import com.yhj.web.reflect.MethodInfo;

/***
 * 最大的用处是用来返回json数据, 也可以返回视图
 * 
 * @author Today
 */
public final class RequestMapping implements Serializable {

	private static final long	serialVersionUID	= 1430992221283070496L;

	private String				requestUri			= null;

	private RequestMethod[]		requestMethod		= null;

	private Class<?>			actionProcessor		= null;

	private MethodInfo			methodInfo			= null;

	public RequestMapping(String requestUri, RequestMethod[] method) {
		this.requestUri = requestUri;
		this.requestMethod = method;
	}

	public RequestMapping() {

	}

	public RequestMapping(RequestMethod[] method, String requestUri) {
		this.requestMethod = method;
		this.requestUri = requestUri;
	}

	public final RequestMethod[] getMethod() {
		return requestMethod;
	}

	public final String getRequestUri() {
		return requestUri;
	}

	public final RequestMapping setMethod(RequestMethod[] method) {
		this.requestMethod = method;
		return this;
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
	public final String toString() {
		return "{\n\t\"requestUri\":\"" + requestUri + "\",\n\t\"method\":\"" + requestMethod
				+ "\",\n\t\"actionProcessor\":\"" + actionProcessor + "\",\n\t\"methodInfo\":\"" + methodInfo + "\"\n}";
	}
}
