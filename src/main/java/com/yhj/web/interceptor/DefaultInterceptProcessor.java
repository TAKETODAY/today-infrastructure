package com.yhj.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class DefaultInterceptProcessor implements InterceptProcessor{

	@Override
	public boolean beforeProcess(HttpServletRequest request, HttpServletResponse response) {
		
		return true;
	}

	@Override
	public void afterProcess(HttpServletRequest request, HttpServletResponse response) {
		
	}
	

}
