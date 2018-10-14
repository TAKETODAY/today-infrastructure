/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler;

import cn.taketoday.web.Constant;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * 
 *         2018-1-1 17:30:35
 */
@Slf4j
public final class ActionHandler extends AbstractHandler<HandlerMapping> {

	@Override
	public Object doDispatch(HandlerMapping mapping, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// Handler Method
		HandlerMethod handlerMethod = mapping.getHandlerMethod();
		// method parameter
		MethodParameter[] methodParameters = handlerMethod.getParameter();
		// Handler Method parameter list
		final Object[] args = new Object[methodParameters.length];

		if (!parameterResolver.resolveParameter(args, methodParameters, request, response)) {
			response.sendError(400); // bad request
			return null;
		}
		// log.debug("parameter list -> {}", Arrays.toString(args));

		Object invoke = handlerMethod.getMethod().invoke(applicationContext.getBean(mapping.getAction()), args); // invoke

		if (invoke instanceof String && !mapping.isResponseBody()) { // return view
			final String returnStr = ((String) invoke);
			if (returnStr.startsWith(Constant.REDIRECT_URL_PREFIX)) {
				String url = returnStr.replace(Constant.REDIRECT_URL_PREFIX, "");
				if (!url.startsWith(Constant.HTTP)) {
					url = contextPath + url;
				}
				response.sendRedirect(url);
			} else {
				viewResolver.resolveView(returnStr, request, response);
			}
		} //
		else if (invoke instanceof File) {

			downloadFile(request, response, (File) invoke);
		} //
		else if (invoke.getClass().getSuperclass() == Image.class) {
			// need set content type
			ImageIO.write((RenderedImage) invoke, "png", response.getOutputStream());
			response.flushBuffer();
		} //
		else if (invoke != null) { // 返回字符串
			response.setContentType(Constant.CONTENT_TYPE_JSON);
			response.getWriter().print(JSON.toJSONString(invoke, SerializerFeature.WriteMapNullValue,
					SerializerFeature.WriteNullListAsEmpty));
		}

		log.debug("result -> {}", invoke);
		return invoke;
	}

}
