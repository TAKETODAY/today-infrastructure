package com.yhj.web.reflect;

import java.lang.annotation.Annotation;

public final class MethodParameter {

	/**	参数名*/
	private String parameterName;

	/**	参数类型*/
	private Class<?> parameterClass;

	
	private volatile Annotation[] parameterAnnotations;

	
	public MethodParameter(String parameterName, Class<?> parameterClass, Annotation[] parameterAnnotations) {
		this.parameterName = parameterName;
		this.parameterClass = parameterClass;
		this.parameterAnnotations = parameterAnnotations;
	}
	
	
	public MethodParameter(String parameterName, Class<?> parameterClass) {
		this.parameterName = parameterName;
		this.parameterClass = parameterClass;
	}


	public MethodParameter() {
		
	}
	
	
	public String getParameterName() {
		return parameterName;
	}

	public Class<?> getParameterClass() {
		return parameterClass;
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	public void setParameterClass(Class<?> parameterClass) {
		this.parameterClass = parameterClass;
	}


	@Override
	public String toString() {
		return "{\n\t\"parameterName\":\"" + parameterName + "\",\n\t\"parameterClass\":\"" + parameterClass
				+ "\",\n\t\"parameterAnnotations\":\"" + parameterAnnotations + "\"\n}";
	}
}
