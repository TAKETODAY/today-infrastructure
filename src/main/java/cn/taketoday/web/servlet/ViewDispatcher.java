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

import cn.taketoday.web.core.Constant;
import cn.taketoday.web.handler.DispatchHandler;
import cn.taketoday.web.mapping.ViewMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * 
 * 		2018-06-25 19:48:28
 * @version 2.0.0
 */
@Slf4j
public final class ViewDispatcher extends HttpServlet {

	private static final long serialVersionUID = 7842118161452996065L;

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		final String contextPath = request.getContextPath();
		
		ViewMapping mapping = DispatchHandler.VIEW_REQUEST_MAPPING.get(request.getRequestURI().replace(contextPath, ""));
		if(mapping == null) {
			response.sendError(404);
			return ;
		}
		// 转到相应页面
		final String assetsPath = mapping.getAssetsPath();
		switch (mapping.getReturnType()) 
		{
			case Constant.TYPE_DISPATCHER:
				request.getRequestDispatcher(assetsPath).forward(request, response);
				return;
			case Constant.TYPE_REDIRECT:
				if(assetsPath.startsWith("http")) {
					response.sendRedirect(assetsPath);
					return ;
				}
				response.sendRedirect(contextPath + "/" + assetsPath);
				return;
			default:
				response.sendError(500);
				return;
		}
	}

	@Override
	public void destroy() {

		log.debug("------ Views Dispatcher SHUTDOWN -------");
	}

}
