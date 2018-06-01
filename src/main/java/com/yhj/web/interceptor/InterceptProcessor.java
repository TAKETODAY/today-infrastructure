package com.yhj.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface InterceptProcessor {

	/**	前置拦截器
	 * @throws Exception **/
	public boolean beforeProcess(HttpServletRequest request, HttpServletResponse response) throws Exception;	

	/** 后置拦截器
	 * @throws Exception **/
	default void afterProcess(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
	}

	
}
