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
package cn.taketoday.web.handler;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;

import cn.taketoday.context.core.Constant;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年7月1日 下午5:30:35
 */
@Slf4j
public final class ActionHandler extends AbstractHandler<HandlerMapping> {
	
	/**
	 * 处理请求
	 */
	@Override
	public void doDispatch(HandlerMapping mapping, HttpServletRequest request, HttpServletResponse response) throws Exception {

		//获取处理器方法
		HandlerMethod handlerMethod = mapping.getHandlerMethod();
		//方法参数
		MethodParameter[] methodParameters = handlerMethod.getParameter();
		//处理器参数列表
		final Object[] args = new Object[methodParameters.length];
		
		if(!parameterResolver.resolveParameter(args, methodParameters, request, response)){
			log.debug("bad request");
			response.sendError(400);	//bad request
			return;
		}
//		log.debug("parameter list -> {}", Arrays.toString(args));

		//准备执行+
		Object actionProcessor = null;// 处理器
		try {
			//TODO get instance in IOC
			actionProcessor = mapping.getActionProcessor().newInstance();
		} 
		catch (InstantiationException e) {
			log.error("Initialized Exception -> {}" ,e);
		} 
		catch (IllegalAccessException e) {
			log.error("IllegalAccessException -> {}" ,e);
		}
		
		Object invoke = handlerMethod.getMethod().invoke(actionProcessor, args);	// 参数注入并执行

		if (invoke instanceof String && !mapping.isResponseBody()) {	//返回视图
			final String returnStr = ((String) invoke);
			if (returnStr.startsWith(Constant.REDIRECT_URL_PREFIX)) {
				response.sendRedirect(contextPath + returnStr.replace(Constant.REDIRECT_URL_PREFIX, ""));
			} else {
				viewResolver.resolveView(returnStr, request, response);
			}
			
		} else if (invoke instanceof File) {
			
			downloadFile(request, response, (File) invoke);
		} else if (invoke.getClass().getSuperclass() == Image.class){
			//need set content type
			ImageIO.write((RenderedImage)invoke, "jpg", response.getOutputStream());
			response.flushBuffer();
		} else if(invoke != null){	//返回字符串
			response.setContentType(Constant.CONTENT_TYPE_JSON);
			response.getWriter().print(JSON.toJSON(invoke));
		} 
		
		log.debug("result -> {}", invoke);

	}
	
	/**
	 * download file
	 * @param request
	 * @param response
	 * @param download
	 * @throws IOException
	 */
	private void downloadFile(HttpServletRequest request, HttpServletResponse response, File download) throws IOException{
		
		response.setContentLengthLong(download.length());
		response.setHeader("Content-Transfer-Encoding", "binary");
		response.setHeader("Content-Type", "pplication/force-download;");
		response.setHeader("Content-Disposition", "attachment;filename=\""+ 
						URLEncoder.encode(download.getName(), "UTF-8") + "\"");
		
		InputStream in = new FileInputStream(download.getAbsolutePath());
		OutputStream out = response.getOutputStream();
		byte[] b = new byte[2048];
		int len = 0;
		while ((len = in.read(b)) != -1) {
			out.write(b, 0, len);
		}
		out.flush();
		response.flushBuffer();
	    out.close();
	    in.close();
	}

}
