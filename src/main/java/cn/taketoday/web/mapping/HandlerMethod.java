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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Today <br>
 *         2018-06-25 20:03:11
 */
public final class HandlerMethod {

	/** action **/
	private final Method method;
	/** parameter list **/
	private final MethodParameter[] parameter;

	/**
	 * use switch case instead of if else
	 * 
	 * @since 2.3.1
	 */
	private final byte reutrnType;

	public HandlerMethod(Method method, List<MethodParameter> parameters, byte reutrnType) {
		this.method = method;
		this.reutrnType = reutrnType;
		this.parameter = parameters.toArray(new MethodParameter[0]);
	}

	public final Method getMethod() {
		return method;
	}

	public final MethodParameter[] getParameter() {
		return parameter;
	}

	public final byte getReutrnType() {
		return reutrnType;
	}

	@Override
	public String toString() {
		return "{method=" + getMethod() + ", parameter=[" + Arrays.toString(getParameter()) + "]}";
	}
}
