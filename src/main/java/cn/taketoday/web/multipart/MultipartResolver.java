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
package cn.taketoday.web.multipart;

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.mapping.MethodParameter;

/**
 * @author Today
 * @date 2018年6月28日 下午4:35:25
 */
public interface MultipartResolver {

	/**
	 * 
	 * @param request
	 * @return
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws Exception
	 */
	Object resolveMultipart(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter)
			throws Exception;

	/**
	 * 
	 * @param request
	 */
	void cleanupMultipart(HttpServletRequest request);

}
