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

import cn.taketoday.web.Constant;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 * 		2018-06-25 20:03:11
 */
@Setter
@Getter
@NoArgsConstructor
public final class HandlerMethod {

	/** action **/
	private Method				method		= null;
	/** parameter list **/
	private MethodParameter[]	parameter	= null;

	/**
	 * use switch case instead of if else
	 * 
	 * @since 2.3.1
	 */
	private byte				reutrnType  = Constant.RETURN_VOID;
	
	public HandlerMethod(Method method, List<MethodParameter> parameters) {
		this.method = method;
		this.parameter = parameters.toArray(new MethodParameter[0]);
	}

	@Override
	public String toString() {
		return "{method=" + method + ", parameter=[" + Arrays.toString(parameter) + "]}";
	}
}
