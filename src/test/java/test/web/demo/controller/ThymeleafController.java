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

import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;

/**
 * 
 * @author Today <br>
 *         2018-10-27 10:09
 */
@Controller
public class ThymeleafController extends BaseController {

	private static final long serialVersionUID = -3151382835705083327L;

	@ActionMapping("/thymeleaf")
	public String thymeleaf(HttpServletRequest request) {

		request.setAttribute("hello", "Hello thymeleaf");

		return "/hello";
	}

	@ActionMapping("/thymeleaf/array")
	public String thymeleaf_array(HttpServletRequest request) {

		request.setAttribute("arrays", new Integer[] { 1, 100, 22, 33 });

		return "/array";
	}

}
