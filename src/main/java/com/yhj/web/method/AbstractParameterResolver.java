package com.yhj.web.method;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.yhj.web.conversion.Converter;
import com.yhj.web.reflect.MethodParameter;

public abstract class AbstractParameterResolver implements ParameterResolver {

	
	protected final static Map<Class<?>, Converter<?, ?>> supportParameterTypes  = new HashMap<>();
	
	
	
	@Override
	public final boolean supportsParameter(MethodParameter parameter) {
		
		
		return false;
	}

	@Override
	public boolean resolveParameter(Object[] args, MethodParameter parameter, HttpServletRequest request)
			throws Exception {
		return false;
	}

}
