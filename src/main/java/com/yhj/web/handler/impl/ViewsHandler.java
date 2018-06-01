package com.yhj.web.handler.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.mapping.ViewMapping;

/**
 * 视图控制器
 * 
 * @author Today
 */
public final class ViewsHandler extends AbstractViewHandler {

	/**
	 * 处理视图请求
	 */
	@Override
	public final void doDispatch(ViewMapping mapping, HttpServletRequest request, HttpServletResponse response) throws Exception {
		long start = System.currentTimeMillis();

		// 转到相应页面
			switch (mapping.getReturnType()) 
			{
				case "dispatcher":
					if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
						request.getRequestDispatcher(mapping.getAssetsPath().replace(".jsp", "_content.jsp"))
								.forward(request, response); 
						System.out.println("dispatcher_content : " + (System.currentTimeMillis() - start) + "ms");
						return;
					}
					request.getRequestDispatcher(mapping.getAssetsPath()).forward(request, response);
					System.out.println("dispatcher : " + (System.currentTimeMillis() - start) + "ms");
					return;
				case "redirect":
					response.sendRedirect(contextPath + "/" + mapping.getAssetsPath());
					System.out.println("redirectAction : " + (System.currentTimeMillis() - start) + "ms");
					return;
				default:
					response.sendError(500);
					return;
			}
		

	}

}
