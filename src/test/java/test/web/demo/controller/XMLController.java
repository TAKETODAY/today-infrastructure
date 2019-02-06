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

import cn.taketoday.context.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Today <br>
 * 
 *         2018-12-08 23:15
 */
public class XMLController {

	public void test(HttpServletRequest request, HttpServletResponse response) {

		request.setAttribute("key", "World");
		System.err.println(request);
	}

	public Object obj(HttpServletRequest request, HttpServletResponse response) {

		String key = request.getParameter("r");
		if (StringUtils.isNotEmpty(key)) {
			return "redirect:/" + key;
		}
		request.setAttribute("key", request.getParameter("key"));

		return "/xml/test";
	}

}
