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

import java.io.Serializable;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Today
 * @date 2018年6月25日 下午7:59:13
 */
@Getter
@Setter
public final class HandlerMapping implements Serializable {

	private static final long	serialVersionUID	= 1430992221283070496L;
	/** 处理器类 */
	private Class<?>			actionProcessor		= null;
	/** 处理器方法 */
	private HandlerMethod		handlerMethod		= null;
	/** 拦截器 */
	private Integer[]			interceptors		= null;
	/** 响应方式 */
	private boolean				responseBody		= false;
	/** Produces */
	private String				produces			= null;

	
	public HandlerMapping() {

	}

	@Override
	public String toString() {
		return "[actionProcessor=" + actionProcessor + ", processorMethod=" + handlerMethod.getMethod().getName() + ", interceptors="
				+ Arrays.toString(interceptors) + ", responseBody=" + responseBody + "]";
	}

}
