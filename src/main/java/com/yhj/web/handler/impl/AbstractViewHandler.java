package com.yhj.web.handler.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.yhj.web.handler.DispatchHandler;
import com.yhj.web.mapping.ViewMapping;

public abstract class AbstractViewHandler implements DispatchHandler<ViewMapping>{

	protected static String	contextPath	= null;
	
	@Override
	public abstract void doDispatch(ViewMapping mapping, HttpServletRequest request, HttpServletResponse response) throws Exception ;

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
		if ("view".equals(element.getName())) {
			setViews(contextPath, element, mapping);
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
		mapping.setAssetsPath(element.getParent().attribute("baseDir").getValue() 
				+ element.attribute("jsp").getValue());

		mapping.setReturnType(element.attribute("type").getValue());
		VIEW_REQUEST_MAPPING.put(contextPath + "/" + element.attribute("name").getValue(), mapping);
		System.out.println("View 映射:" + VIEW_REQUEST_MAPPING.get(contextPath + "/" + element.attribute("name").getValue()));
	}


}
