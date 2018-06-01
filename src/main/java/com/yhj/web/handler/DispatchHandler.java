package com.yhj.web.handler;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.interceptor.InterceptProcessor;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.mapping.ViewMapping;

public interface DispatchHandler<T>{
	
	/**	拦截器池*/
	Map<String, InterceptProcessor> INTERCEPTOR_MAPPING = new HashMap<>();
	/** view 视图映射池 */
	Map<String, ViewMapping> VIEW_REQUEST_MAPPING = new HashMap<>();
	/** Action 映射池 */
	Map<String, RequestMapping>	ACTION_REQUEST_MAPPING = new HashMap<>();

	/**
	 * 初始化处理器
	 * @param config 初始化参数
	 */
	public void doInit(ServletConfig config);

	/**
	 * 处理请求
	 * @param requestURI
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void doDispatch(T mapping, HttpServletRequest request, HttpServletResponse response) throws Exception;
	
	
	
}
