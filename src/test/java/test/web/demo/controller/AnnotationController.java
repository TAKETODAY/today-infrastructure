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
package test.web.demo.controller;

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.annotation.Application;
import cn.taketoday.web.annotation.Cookie;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Header;
import cn.taketoday.web.annotation.RestController;

/**
 * 
 * @author Today <br>
 *         2018-10-27 10:07
 */
@RestController
public class AnnotationController {

	@GET("header")
	public String annotation(HttpServletRequest request, @Header("User-Agent") String agent) {

		return request.getMethod() + " User-Agent -> " + agent;
	}

	@GET("cookie")
	public String cookie(HttpServletRequest request, @Cookie("JSESSIONID") String sessionId) {

		return request.getMethod() + " your sessionId -> " + sessionId;
	}

	@GET("application")
	public String application(HttpServletRequest request, @Application("contextPath") String sessionId) {

		return request.getMethod() + " your contextPath -> " + sessionId;
	}

}
