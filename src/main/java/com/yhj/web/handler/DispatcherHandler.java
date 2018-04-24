package com.yhj.web.handler;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DispatcherHandler {

	public void doDispatchHandle(String requestURI, HttpServletRequest request, HttpServletResponse response) throws Exception;

	public void doInit(ServletConfig config);

}
