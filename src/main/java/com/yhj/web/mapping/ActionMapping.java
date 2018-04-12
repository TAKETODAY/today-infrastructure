package com.yhj.web.mapping;

import com.yhj.web.enums.RequestMethod;

/***
 * 最大的用处是用来返回json数据,
 * 也可以返回视图
 * @author Today
 */
public class ActionMapping {

	
	
	private RequestMethod[] method;
	
	private String [] requestUri;
	
	
	
	
	public ActionMapping(RequestMethod[] method, String[] requestUri) {
		this.method = method;
		this.requestUri = requestUri;
	}
	
	public ActionMapping() {
		
	}
	

	public RequestMethod[] getMethod() {
		return method;
	}

	public String[] getRequestUri() {
		return requestUri;
	}

	public void setMethod(RequestMethod[] method) {
		this.method = method;
	}

	public void setRequestUri(String[] requestUri) {
		this.requestUri = requestUri;
	}

	@Override
	public String toString() {
		return "{\n\t\"method\":\"" + method + "\",\n\t\"requestUri\":\"" + requestUri + "\"\n}";
	}
	
}
