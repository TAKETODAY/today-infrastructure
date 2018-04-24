package com.yhj.web.enums;

public enum RequestMethod {

	GET, POST, PUT, DELETE;
//	GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

	public final boolean hasMethod(String method) {
		return this.toString().equals(method);
	}
	
}