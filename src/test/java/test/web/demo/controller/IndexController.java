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

import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author Today <br>
 *         2018-10-27 10:10
 */
@Controller
public final class IndexController extends BaseController {

	private static final long serialVersionUID = -2144421103258985200L;

	@ActionMapping(value = { "/", "/index", "/index.html" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String index(HttpServletRequest request, HttpSession session, @RequestParam String arr) {

		String userId = request.getParameter("userId");
		String userName = request.getParameter("userName");
		request.setAttribute("q", arr);
		request.setAttribute("userId", userId);
		request.setAttribute("userName", userName);
		request.setAttribute("url", request.getRequestURL());

		return "/index/index";
	}

	@ResponseBody
	@ActionMapping(value = { "/index.action" }, method = RequestMethod.GET)
	public String index(@RequestParam(required = false) final String q, String userName, Integer userId, Model model,
			HttpServletRequest request) {

		model.addAttribute("q", q);
		model.addAttribute("userId", userId);
		model.addAttribute("userName", userName);
		model.addAttribute("url", request.getRequestURL());

		return "{\"q\":" + q + ",\"userId\":\"" + userId + "\",\"userName\":\"" + userName + "\"}";
	}

	@GET({ "/redirect/{path}" })
	public String redirect(@PathVariable String path) {

		return "redirect:/" + path;
	}

	@ResponseBody(false)
	@ActionMapping(value = { "/index.htm" }, method = RequestMethod.GET)
	public String index(String userName, Integer userId, HttpServletRequest request,
			@RequestParam(required = false) String[] Q, Integer[] q) {
		request.setAttribute("Q", Q);
		request.setAttribute("q", q);
		request.setAttribute("userId", userId);
		request.setAttribute("userName", userName);
		return "/index/index";
	}

	@RequestMapping(value = { "/index.h" }, method = RequestMethod.GET)
	public String index(String userName, Integer userId, HttpServletRequest request, Long[] q, long[] Q) {
		request.setAttribute("Q", Q);
		request.setAttribute("q", q);
		request.setAttribute("userId", userId);
		request.setAttribute("userName", userName);
		return "/index/index";
	}

}
