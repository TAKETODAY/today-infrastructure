package com.yhj.web.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.core.Constant;
import com.yhj.web.handler.DispatchHandler;
import com.yhj.web.handler.impl.ActionHandler;
import com.yhj.web.handler.impl.ViewsHandler;
import com.yhj.web.interceptor.InterceptProcessor;
import com.yhj.web.mapping.RequestMapping;
import com.yhj.web.mapping.ViewMapping;

public final class ViewsDispatcher extends HttpServlet implements Constant{

	private static final long serialVersionUID	= 1L;

	private static final DispatchHandler<ViewMapping> VIEW_HANDLER = new ViewsHandler();

	private static final DispatchHandler<RequestMapping> ACTION_HANDLER = new ActionHandler();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		VIEW_HANDLER.doInit(config);
		ACTION_HANDLER.doInit(config);
		
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
		request.setCharacterEncoding("UTF-8");
		String requestURI = request.getRequestURI();
		System.out.println("uri: " + requestURI);
		
		//进入处理器处理相应的请求
		try {
			
			ViewMapping viewMapping = DispatchHandler.VIEW_REQUEST_MAPPING.get(requestURI);
			
			if(viewMapping != null) {
				VIEW_HANDLER.doDispatch(viewMapping, request, response);
				return;
			}
			
			System.out.println("NOT Found : " + (System.currentTimeMillis() - start) + "ms");
			System.out.println("ViewsHandler NOT Found : " + request.getRequestURI());
			
			RequestMapping requestMapping = DispatchHandler.ACTION_REQUEST_MAPPING.get(request.getMethod() + REQUEST_METHOD_PREFIX + requestURI);
			if (requestMapping == null) {
				response.sendError(404);
				return;
			}
			/**	进入拦截*/
			String[] interceptors = requestMapping.getInterceptors();
			
			for(String interceptor : interceptors) {
				System.out.println(interceptor);
				InterceptProcessor interceptProcessor = DispatchHandler.INTERCEPTOR_MAPPING.get(interceptor);
				
				if(!interceptProcessor.beforeProcess(request, response)){
					System.out.println("整个过程: " + (System.currentTimeMillis() - start) + "ms");
					return;
				}
				
			}
			
			
			ACTION_HANDLER.doDispatch(requestMapping, request, response);
		
//			interceptProcessor.afterProcess(request, response);
			
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
