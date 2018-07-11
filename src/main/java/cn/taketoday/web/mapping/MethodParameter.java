/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.mapping;

import cn.taketoday.context.core.Constant;
import lombok.Getter;
import lombok.Setter;


/**
 * @author Today
 * @date 2018年6月25日 下午8:01:52
 * @version 2.0.0
 */
@Setter
@Getter
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
	private int			annotation		= Constant.ANNOTATION_NULL;
	/**	*/
	private int 		pathIndex		= 0;
	
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

	public final boolean hasPathVariable(){
		return annotation == Constant.ANNOTATION_PATH_VARIABLE;
	}
	
	public final boolean isRequestBody() {
		return annotation == Constant.ANNOTATION_REQUESTBODY;
	}
	
	public final boolean hasAnnotation() {
		return annotation != Constant.ANNOTATION_NULL;
	}

	@Override
	public String toString() {
		return " {\"required\":\"" + required + "\", \"parameterName\":\"" + parameterName
				+ "\", \"parameterClass\":\"" + parameterClass + "\", \"genericityClass\":\"" + genericityClass
				+ "\", \"annotation\":\"" + annotation + "\"}";
	}

}
