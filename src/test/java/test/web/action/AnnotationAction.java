/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package test.web.action;

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Header;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RestController;

/**
 * @author Today
 * @date 2018年7月2日 下午11:39:50
 */
@RestController
public class AnnotationAction {

	@GET("ann")
	@POST("post")
	public String annotation(HttpServletRequest request, @Header("User-Agent") String agent) {

		return request.getMethod() + " User-Agent -> " + agent;
	}


}
