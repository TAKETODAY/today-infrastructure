
package com.yhj.web.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.conversion.Converter;


public interface ParameterResolver {

	/**
	 * 初始化处理器
	 * @param config 初始化参数
	 */
	public void doInit(Set<Class<?>> action);
	
	Map<Class<?>, Converter<String, Object>> supportParameterTypes  = new HashMap<>(1);
	
	public boolean supportsParameter(MethodParameter parameter);

	public boolean resolveParameter(Object[] args , MethodParameter[] parameters, HttpServletRequest request, HttpServletResponse response) throws Exception;

	
	
}
