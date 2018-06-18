package com.yhj.web.mapping;

import java.lang.reflect.Method;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.annotation.ParameterConverter;
import com.yhj.web.conversion.Converter;

public abstract class AbstractParameterResolver implements ParameterResolver {

	
	@Override
	@SuppressWarnings("unchecked")
	public final void doInit(Set<Class<?>> actions) {
		
		try {
			
			for (Class<?> clazz : actions) {
				
				if(clazz.isInterface()) {
					continue;
				}
				
				ParameterConverter converter = clazz.getAnnotation(ParameterConverter.class);
				if (converter == null) {
					continue;
				}
				Method[] declaredMethods = clazz.getDeclaredMethods();				
				for (Method method : declaredMethods) {
					String methodName = method.getName();
					if("doConvert".equals(methodName)) {
						supportParameterTypes.put(method.getReturnType(), (Converter<String, Object>) clazz.newInstance());
					}
				}
			}
		} 
		catch (InstantiationException | IllegalAccessException e) {
			
			e.printStackTrace();
		}
		
		System.out.println(supportParameterTypes);

	}

	@Override
	public final boolean supportsParameter(MethodParameter parameter) {
		return supportParameterTypes.containsKey(parameter.getParameterClass());
	}


	@Override
	public abstract boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request, 
			HttpServletResponse response) throws Exception ;

}
