package com.yhj.web.mapping;

import java.io.Serializable;
import java.lang.reflect.Method;


/***
 * 请求路径映射类,用来映射一个路径对应的资源.
 * @author Today
 */
public final class ViewMapping implements Serializable {

	private static final long	serialVersionUID	= -8130348090936881368L;
	/** 返回类型 */
	private String				returnType			= null;
	/** 资源路径 */
	private String				assetsPath			= "";
	/** 请求路径 */
	private String				requestUri			= "";
	/** 前置方法 (可以用来做拦截器)*/
	private Method				method				= null;
	/** 方法所在类 */
	private Class<?>			clazz				= null;

	public String getAssetsPath() {
		return assetsPath;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public Method getMethod() {
		return method;
	}

	public String getRequestUri() {
		return requestUri;
	}

	public String getReturnType() {
		return returnType;
	}

	public boolean hasMethod() {
		return method != null;
	}

	public ViewMapping setAssetsPath(String assetsPath) {
		this.assetsPath = assetsPath;
		return this;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public ViewMapping setMethod(Method method) {
		this.method = method;
		return this;
	}

	public ViewMapping setRequestUri(String requestUri) {
		this.requestUri = requestUri;
		return this;
	}

	public ViewMapping setReturnType(String returnType) {
		this.returnType = returnType;
		return this;
	}

	@Override
	public String toString() {
		return " {\n\t\"returnType\":\"" + returnType + "\",\n\t\"assetsPath\":\"" + assetsPath
				+ "\",\n\t\"requestUri\":\"" + requestUri + "\",\n\t\"method\":\"" + method + "\",\n\t\"clazz\":\""
				+ clazz + "\"\n}";
	}
}