package com.yhj.web.reflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public final class MethodInfo {

	/** 方法本身 **/
	private Method				method		= null;
	/** 参数列表 **/
	private MethodParameter[]	parameter	= null;
	/** 返回类型 **/
	private Class<?>			returnType	= null;

	
	public MethodInfo() {

	}

	
	public MethodInfo(Method method, List<MethodParameter> parameters, Class<?> returnType) {
		this.method = method;
		this.parameter = parameters.toArray(new MethodParameter [] {});
		this.returnType = returnType;
	}

	public Method getMethod() {
		return method;
	}

	public MethodParameter[] getParameter() {
		return parameter;
	}

	public Class<?> getReturnType() {
		return returnType;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public void setParameter(MethodParameter[] parameter) {
		this.parameter = parameter;
	}

	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n\t\"method\":\"");
		builder.append(method);
		builder.append("\",\n\t\"parameter\":\"");
		builder.append(Arrays.toString(parameter));
		builder.append("\",\n\t\"returnType\":\"");
		builder.append(returnType);
		builder.append("\"\n}");
		return builder.toString();
	}
	
}
