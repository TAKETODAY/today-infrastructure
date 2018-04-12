package com.yhj.web.enums;

public enum RequestMethod {

	GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

	private String method;

	private RequestMethod(String method) {
		this.method = method;
	}
	
	private RequestMethod() {
		
	}
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

}