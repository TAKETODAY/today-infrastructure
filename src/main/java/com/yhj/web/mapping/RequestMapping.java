package com.yhj.web.mapping;

import java.io.Serializable;

/***
 * 最大的用处是用来返回json数据, 也可以返回视图
 * @author Today
 */
public final class RequestMapping implements Serializable {

	private static final long	serialVersionUID	= 1430992221283070496L;
	/**	处理器类*/
	private Class<?>			actionProcessor		= null;
	/**	处理器方法*/
	private ProcessorMethod		processorMethod		= null;
	/**	拦截器*/
	private String[] 			interceptors		= null;
	/**	响应方式*/
	private boolean 			responseBody   		= false;
	
 	
	public RequestMapping() {

	}
	
	public final String[] getInterceptors() {
		return interceptors;
	}

	public final void setInterceptors(String[] interceptors) {
		this.interceptors = interceptors;
	}
	
	public final boolean isResponseBody() {
		return responseBody;
	}

	public final void setResponseBody(boolean responseBody) {
		this.responseBody = responseBody;
	}


	public final Class<?> getActionProcessor() {
		return actionProcessor;
	}

	public final ProcessorMethod getProcessorMethod() {
		return processorMethod;
	}

	public final RequestMapping setActionProcessor(Class<?> actionProcessor) {
		this.actionProcessor = actionProcessor;
		return this;
	}

	public final RequestMapping setProcessorMethod(ProcessorMethod methodInfo) {
		this.processorMethod = methodInfo;
		return this;
	}

	@Override
	public String toString() {
		return "{\n\t\"actionProcessor\":\"" + actionProcessor + "\",\n\t\"processorMethod\":\"" + processorMethod
				+ "\",\n\t\"responseBody\":\"" + responseBody + "\"\n}";
	}
}
