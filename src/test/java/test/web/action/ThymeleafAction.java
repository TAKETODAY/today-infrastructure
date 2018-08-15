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

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.context.annotation.ActionProcessor;
import cn.taketoday.web.annotation.ActionMapping;

/**
 * @author Today
 * @date 2018年6月26日 下午8:24:43
 */
@ActionProcessor
public class ThymeleafAction extends BaseAction{

	private static final long serialVersionUID = -3151382835705083327L;

	@ActionMapping("/thymeleaf")
	public String thymeleaf(HttpServletRequest request) {
		
		request.setAttribute("hello", "Hello thymeleaf");
		
		return "/hello";
	}
	
	@ActionMapping("/thymeleaf/array")
	public String thymeleaf_array(HttpServletRequest request) {
		
		request.setAttribute("arrays", new Integer[]{1, 100, 22, 33});
		
		return "/array";
	}
	
	
	
}
