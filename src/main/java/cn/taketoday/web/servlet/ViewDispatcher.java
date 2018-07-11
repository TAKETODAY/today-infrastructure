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
package cn.taketoday.web.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.core.Constant;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.mapping.ViewMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月25日 下午7:48:28
 * @version 2.0.0
 */
@Slf4j
public final class ViewDispatcher extends HttpServlet {

	private static final long serialVersionUID = 7842118161452996065L;

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		final String contextPath = request.getContextPath();
		String requestURI = request.getRequestURI().replace(contextPath, "");

		ViewMapping mapping = DispatchHandler.VIEW_REQUEST_MAPPING.get(requestURI);
		// 转到相应页面
		switch (mapping.getReturnType()) 
		{
			case Constant.TYPE_DISPATCHER:
				request.getRequestDispatcher(mapping.getAssetsPath()).forward(request, response);
				return;
			case Constant.TYPE_REDIRECT:
				response.sendRedirect(contextPath + "/" + mapping.getAssetsPath());
				return;
			default:
				response.sendError(500);
				return;
		}
		
	}

	@Override
	public void destroy() {

		log.debug("------ ViewDispatcher shutdown -------");
	}

}
