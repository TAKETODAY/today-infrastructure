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

/**
 * Mapping handler to a request
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:59:13
 */
public class HandlerMapping {

	private static final int[] EMPTY = new int[0];
	/** 处理器类 */
//	private String				action;
	private Object action;
	/** 处理器方法 */
	private HandlerMethod handlerMethod;
	/** 拦截器 */
	private int[] interceptors = EMPTY;

	/**
	 * @return
	 */
	public final boolean hasInterceptor() {
		return interceptors != EMPTY;
	}

	public Object getAction() {
		return action;
	}

	public HandlerMapping setAction(Object action) {
		this.action = action;
		return this;
	}

	public int[] getInterceptors() {
		return interceptors;
	}

	public HandlerMapping setInterceptors(int[] interceptors) {
		this.interceptors = interceptors;
		return this;
	}

	public HandlerMethod getHandlerMethod() {
		return handlerMethod;
	}

	public HandlerMapping setHandlerMethod(HandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
		return this;
	}

}
