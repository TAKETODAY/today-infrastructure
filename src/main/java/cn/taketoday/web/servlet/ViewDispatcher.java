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

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.web.Constant;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.view.ViewResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:48:28
 * @version 2.0.0
 */
public final class ViewDispatcher extends HttpServlet {

	private static final long serialVersionUID = 7842118161452996065L;

	@Autowired(Constant.VIEW_RESOLVER)
	protected transient ViewResolver viewResolver;
	/** exception Resolver */
	@Autowired(Constant.EXCEPTION_RESOLVER)
	private transient ExceptionResolver exceptionResolver;
	/** view 视图映射池 */
	public static final Map<String, ViewMapping> VIEW_REQUEST_MAPPING = new HashMap<>(8);

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		final ViewMapping mapping = VIEW_REQUEST_MAPPING.get(request.getRequestURI());
		if (mapping == null) {
			response.sendError(404);
			return;
		}

		try {

			// 转到相应页面
			switch (mapping.getReturnType())
			{
				case Constant.TYPE_FORWARD :
					viewResolver.resolveView(mapping.getAssetsPath(), request, response);
					return;
				case Constant.TYPE_REDIRECT :
					response.sendRedirect(mapping.getAssetsPath());
					return;
				default:
					response.sendError(500);// never get there
					return;
			}

		} catch (Throwable e) {
			exceptionResolver.resolveException(request, response, e);
		}
	}

}
