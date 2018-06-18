package com.yhj.web.mapping;

import java.lang.annotation.Annotation;

public final class MethodParameter {

	/** 是否不能为空 */
	private boolean		required		= true;
	/** 参数名 */
	private String		parameterName	= null;
	/** 参数类型 */
	private Class<?>	parameterClass	= null;
	/** 泛型参数类型 */
	private Class<?>	genericityClass	= null;
	/** 注解支持 */
	private Annotation	annotation		= null;
	/**	*/
	private boolean		requestBody		= false;

	public MethodParameter(String parameterName, Class<?> parameterClass, boolean required) {
		this.parameterName = parameterName;
		this.parameterClass = parameterClass;
		this.required = required;
	}

	public MethodParameter(String parameterName, Class<?> parameterClass) {
		this.parameterName = parameterName;
		this.parameterClass = parameterClass;
	}

	public MethodParameter() {

	}

	public final boolean hasAnnotation() {
		return annotation != null;
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

	public final boolean isRequired() {
		return required;
	}

	public final void setRequired(boolean required) {
		this.required = required;
	}

	public final Class<?> getGenericityClass() {
		return genericityClass;
	}

	public final void setGenericityClass(Class<?> genericityClass) {
		this.genericityClass = genericityClass;
	}

	public final Annotation getAnnotation() {
		return annotation;
	}

	public final void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	public final boolean isRequestBody() {
		return requestBody;
	}

	public final void setRequestBody(boolean requestBody) {
		this.requestBody = requestBody;
	}

	
	@Override
	public String toString() {
		return " { \"required\":\"" + required + "\", \"parameterName\":\"" + parameterName
				+ "\", \"parameterClass\":\"" + parameterClass + "\", \"genericityClass\":\"" + genericityClass
				+ "\", \"annotation\":\"" + annotation + "\"}";
	}

}
