package com.yhj.web.handler.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.yhj.web.handler.DispatcherHandler;
import com.yhj.web.mapping.ViewMapping;

/**
 * 视图控制器
 * @author Today
 */
public final class ViewsHandler implements DispatcherHandler{

	
	private static final Map<String, ViewMapping>	requestMapping		= new HashMap<>();
	
	private static String							contextPath			= null;
	
	
	@Override
	public final void doDispatchHandle(final String requestURI , HttpServletRequest request, HttpServletResponse response) throws Exception {
		long start = System.currentTimeMillis();
		response.setContentType("text/html;charset=UTF-8");

		if (requestMapping.containsKey(requestURI)) {
			
			ViewMapping mapping = requestMapping.get(requestURI);
			// 执行页面转向前的方法
			if (mapping.hasMethod()) {
				// TODO IOC
				try {
					Object obj = mapping.getMethod().invoke(mapping.getClazz().newInstance(), request, response);
					System.out.println("invoke : " + (System.currentTimeMillis() - start) + "ms");
					if (obj != null) {
						response.getWriter().print(obj.toString());
						System.out.println("String : " + (System.currentTimeMillis() - start) + "ms");
						return;
					}
				} catch (Exception e) {
					response.sendError(500);
				}
			}
			// 转到相应页面
			if (!response.isCommitted()) {
				switch (mapping.getReturnType()) {
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
				case "redirectAction":
					response.sendRedirect(contextPath + "/" + mapping.getAssetsPath());
					System.out.println("redirectAction : " + (System.currentTimeMillis() - start) + "ms");
					return;
				case "redirect":
					response.sendRedirect(contextPath + "/" + mapping.getAssetsPath());
					System.out.println("redirectAction : " + (System.currentTimeMillis() - start) + "ms");
					return;
				case "json":
					response.setContentType("application/json;charset=UTF-8");
					System.out.println("json : " + (System.currentTimeMillis() - start) + "ms");
					return;

				}
			}
		} else {
			System.out.println("NOT Found : " + requestURI);
			response.sendError(404);
			System.out.println("NOT Found : " + (System.currentTimeMillis() - start) + "ms");
		}
		
	}

	/**
	 * viewsConfigLocation 加载配置文件
	 */
	@Override
	public void doInit(ServletConfig config) {
		long start = System.currentTimeMillis();

		String con = config.getInitParameter("viewsConfigLocation");
		contextPath = config.getServletContext().getContextPath();
		
		String realPath = config.getServletContext().getRealPath(con);
		
		System.out.println("Initializing ViewsHandler from [" + realPath + "]");
		try {
			setXMLConfiguration(contextPath , realPath);
		} catch (MalformedURLException e) {
			System.err.println("ViewsHandler Initialization ERROR !");
		}
		System.out.println("ViewsHandler Initialization Takes " + (System.currentTimeMillis() - start) + "ms");
	}

	

	/**
	 * 读取配置文件
	 * 
	 * @param configFilePath
	 * @throws MalformedURLException
	 */
	private void setXMLConfiguration(String contextPath, String configFilePath) throws MalformedURLException {
		SAXReader reader = new SAXReader();
		// 读取文件 转换成Document
		Document document = null;
		try {
			document = reader.read(new File(configFilePath));
		} catch (DocumentException e) {
			System.err.println(e.getMessage());
		}
		Element element = document.getRootElement();
		//开始读取
		getConfiguration(contextPath, element);
	}

	/**
	 * 从xml文件中得到配置信息
	 * 
	 * @param element
	 */
	@SuppressWarnings("unchecked")
	private void getConfiguration(String contextPath, Element element) {
		ViewMapping mapping = new ViewMapping();
		// if (!"constant".equals("")) {
		// if (element.attribute("view.extension") != null) {
		// extension = element.attribute("view.extension").getValue();
		// }
		// }
		if ("view".equals(element.getName())) {
			setViews(contextPath, element, mapping);

		} else if ("action".equals(element.getName())) {
			setActions(contextPath,element, mapping);

		}
		// 使用递归
		Iterator<Element> iterator = element.elementIterator();
		while (iterator.hasNext()) {
			getConfiguration(contextPath, iterator.next());
		}
	}

	/***
	 * 设置视图映射
	 * 
	 * @param element
	 * @param mapping
	 */
	public void setViews(String contextPath, Element element, ViewMapping mapping) {
		try {
			mapping.setClazz(Class.forName(element.getParent().attribute("class").getValue()));
		} catch (ClassNotFoundException e) {
			System.err.println(
					element.getParent().attribute("class").getValue() + "-类定义错误! 请仔细检查 [" + e.getMessage() + "]");
		}
		// mapping.setBaseDir(element.getParent().attribute("baseDir").getValue());
		mapping.setAssetsPath(
				element.getParent().attribute("baseDir").getValue() + element.attribute("jsp").getValue());
		mapping.setRequestUri(contextPath + "/" + element.attribute("name").getValue());
		mapping.setReturnType(element.attribute("type").getValue());

		requestMapping.put(contextPath + "/" + element.attribute("name").getValue(), mapping);

		System.out.println("View 映射:" + requestMapping.get(contextPath + "/" + element.attribute("name").getValue()));
	}

	/***
	 * 设置控制器映射
	 * 
	 * @param element
	 * @param mapping
	 */
	public final void setActions(String contextPath, Element element, ViewMapping mapping) {
		if (element.attribute("jsp") != null) {
			mapping.setAssetsPath(
					element.getParent().attribute("baseDir").getValue() + element.attribute("jsp").getValue());
		}
		mapping.setReturnType(element.attribute("type").getValue());
		mapping.setRequestUri(contextPath + "/" + element.attribute("name").getValue());
		// mapping.setBaseDir(element.getParent().attribute("baseDir").getValue());
		try {
			mapping.setClazz(Class.forName(element.getParent().attribute("class").getValue()));
		} catch (ClassNotFoundException e) {

			System.err.println(element.getParent().attribute("class").getValue() + "-类定义错误! 请仔细检查 ["
					+ e.getLocalizedMessage() + "]");
		}
		try {
			mapping.setMethod(mapping.getClazz().getMethod(element.attribute("method").getValue(),
					HttpServletRequest.class, HttpServletResponse.class));
		} catch (NoSuchMethodException e) {
			System.err.println(
					element.getParent().attribute("class").getValue() + "类中未找到此方法(" + e.getLocalizedMessage() + ")");
		} catch (SecurityException e) {
			System.err.println(e.getMessage());
		}

		requestMapping.put(contextPath + "/" + element.attribute("name").getValue(), mapping);

		System.out.println("Action 映射:" + requestMapping.get(contextPath + "/" + element.attribute("name").getValue()));
	}
	
}
