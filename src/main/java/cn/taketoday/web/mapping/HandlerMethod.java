<<<<<<< HEAD
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Today
 * @date 2018年6月25日 下午8:03:11
 */
@Setter
@Getter
public final class HandlerMethod {

	/** 方法本身 **/
	private Method				method		= null;
	/** 参数列表 **/
	private MethodParameter[]	parameter	= null;
	
	public HandlerMethod() {

	}
	
	public HandlerMethod(Method method, List<MethodParameter> parameters) {
		this.method = method;
		this.parameter = parameters.toArray(new MethodParameter [] {});
	}


	@Override
	public String toString() {
		return "{method=" + method + ", parameter=[" + Arrays.toString(parameter) + "]}";
	}
}

=======
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

	public HandlerMethod(Method method, List<MethodParameter> parameters) {
		this.method = method;
		this.parameter = parameters.toArray(new MethodParameter[0]);
	}

	@Override
	public String toString() {
		return "{method=" + method + ", parameter=[" + Arrays.toString(parameter) + "]}";
	}
}
>>>>>>> 2.2.x
