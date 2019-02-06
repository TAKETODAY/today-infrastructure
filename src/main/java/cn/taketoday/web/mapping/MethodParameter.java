/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.mapping;

import cn.taketoday.web.Constant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today
 * 
 * @version 2.2.2 <br>
 *          2018-06-25 20:01:52
 */
@Setter
@Getter
public class MethodParameter {
	//@off
	/** 是否不能为空 */
	private final boolean	required;
	/** 参数名 */
	private final String	parameterName;
	/** 参数类型 */
	private final Class<?>	parameterClass;
	/** 泛型参数类型 */
	private final Class<?>	genericityClass;
	/** 注解支持 */
	private final byte		annotation;
	/**	*/
	private int				pathIndex		= 0;
	/** the default value */
	private final String	defaultValue;
	
	/**
	 * @since 2.3.0
	 */
	private final byte		parameterType;

	/**
	 * @since 2.3.1
	 */
	private String[]		splitMethodUrl  = null;
	
	//@on

	public MethodParameter() {
		this(false, null, null, null, Constant.ANNOTATION_COOKIE, null, Constant.ANNOTATION_NULL);
	}

	public final boolean hasPathVariable() {
		return annotation == Constant.ANNOTATION_PATH_VARIABLE;
	}

	public final boolean isRequestBody() {
		return annotation == Constant.ANNOTATION_REQUEST_BODY;
	}

	public final boolean hasAnnotation() {
		return annotation != Constant.ANNOTATION_NULL;
	}

	public MethodParameter(boolean required, String parameterName, Class<?> parameterClass, Class<?> genericityClass, byte annotation,
			String defaultValue, byte parameterType) {
		this.required = required;
		this.parameterName = parameterName;
		this.parameterClass = parameterClass;
		this.genericityClass = genericityClass;
		this.annotation = annotation;
		this.defaultValue = defaultValue;
		this.parameterType = parameterType;
	}

//	@Override
//	public String toString() {
//		return new StringBuilder()//
//				.append("{\n\t\"required\":\"")//
//				.append(required)//
//				.append("\",\n\t\"parameterName\":\"")//
//				.append(parameterName).append("\",\n\t\"parameterClass\":\"")//
//				.append(parameterClass)//
//				.append("\",\n\t\"genericityClass\":\"")//
//				.append(genericityClass)//
//				.append("\",\n\t\"annotation\":\"").append(annotation)//
//				.append("\",\n\t\"pathIndex\":\"")//
//				.append(pathIndex).append("\"\n}")//
//				.toString();
//	}

}
