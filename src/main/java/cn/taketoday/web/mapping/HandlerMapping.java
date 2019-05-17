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

import java.util.List;
import java.util.Objects;

import cn.taketoday.context.Constant;

/**
 * Mapping handler to a request
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:59:13
 */
public final class HandlerMapping {

	private static final int[] EMPTY = Constant.EMPTY_INT_ARRAY;
	/** 处理器类 */
//	private String				action;
	private final Object action;
	/** 处理器方法 */
	private final HandlerMethod handlerMethod;
	/** 拦截器 */
	private final int[] interceptors;

	public HandlerMapping(Object action, HandlerMethod handlerMethod, List<Integer> interceptors) {

		this.action = action;
		this.handlerMethod = handlerMethod;

		this.interceptors = //
				Objects.requireNonNull(interceptors).size() > 0 //
						? interceptors.stream().mapToInt(Integer::intValue).toArray() //
						: EMPTY;
	}

	public final boolean hasInterceptor() {
		return interceptors != EMPTY;
	}

	public final Object getAction() {
		return action;
	}

	public final int[] getInterceptors() {
		return interceptors;
	}

	public final HandlerMethod getHandlerMethod() {
		return handlerMethod;
	}

}
