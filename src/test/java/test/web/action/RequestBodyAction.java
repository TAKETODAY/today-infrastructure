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
package test.web.action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.RequestBody;
import test.web.domain.User;

/**
 * @author Today
 * @date 2018年6月28日 下午6:35:39
 */
@Controller
public final class RequestBodyAction extends BaseAction{

	private static final long serialVersionUID = -3387876040533333679L;

	
	public RequestBodyAction() {
	
	
	}
	
	@ActionMapping(value = "/body" , method = RequestMethod.GET)
	public String request(HttpServletRequest request) {
		
		request.setAttribute("contextPath", request.getContextPath());
		
		return "/body/body";
	}
	
	@ActionMapping(value = "/body" , method = RequestMethod.POST)
	public User request(@RequestBody User user) {
		
		return user;
	}
	
	@ActionMapping(value = "/body/list" , method = RequestMethod.POST)
	public List<User> request(@RequestBody List<User> users) {
		
		return users;
	}
	
	
}
