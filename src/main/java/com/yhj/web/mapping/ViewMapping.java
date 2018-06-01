package com.yhj.web.mapping;

import java.io.Serializable;


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

	public String getAssetsPath() {
		return assetsPath;
	}


	public String getReturnType() {
		return returnType;
	}


	public ViewMapping setAssetsPath(String assetsPath) {
		this.assetsPath = assetsPath;
		return this;
	}


	public ViewMapping setReturnType(String returnType) {
		this.returnType = returnType;
		return this;
	}


	@Override
	public String toString() {
		return "{\n\t\"returnType\":\"" + returnType + "\",\n\t\"assetsPath\":\"" + assetsPath + "\"\n}";
	}

}