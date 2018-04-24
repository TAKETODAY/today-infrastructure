package com.yhj.web.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.handler.DispatcherHandler;
import com.yhj.web.handler.impl.ActionHandler;

public final class ViewsDispatcher extends HttpServlet {

	private static final long						serialVersionUID	= 1L;

//	private static final DispatcherHandler			viewsHandler		= new ViewsHandler();

	private static final DispatcherHandler			actionHandler		= new ActionHandler();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
//		viewsHandler.doInit(config);
//		actionHandler.doInit(config);
		actionHandler.doInit(config);
		
		
		this.getServletContext().setAttribute("CDN", "//weixiub.oss-cn-beijing.aliyuncs.com");
		this.getServletContext().setAttribute("contextPath", config.getServletContext().getContextPath());

	}

	/**
	 * 解析url
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long start = System.currentTimeMillis();

		response.setCharacterEncoding("UTF-8");
		String requestURI = request.getRequestURI();
		System.out.println("uri: " + requestURI);
		// 进入处理器处理相应的请求
//		try {
//			viewsHandler.doDispatchHandle(requestURI, request, response);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		if(response.isCommitted()) {
//			return ;
//		}
		try {
			actionHandler.doDispatchHandle(requestURI, request, response);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(500);
			response.sendError(500);
		}
		
		System.out.println("整个过程: " + (System.currentTimeMillis() - start) + "ms");
		
	}

	@Override
	public void destroy() {
		System.out.println("------ shutdown ------");
	}


}
