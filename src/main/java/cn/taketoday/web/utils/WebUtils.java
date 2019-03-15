/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.exception.BadRequestException;

/**
 * 
 * @author TODAY <br>
 *         2019-03-15 19:53
 */
public abstract class WebUtils {

	/**
	 * Get {@link WebApplicationContext}
	 * 
	 * @return WebApplicationContext
	 */
	public final static WebApplicationContext getWebApplicationContext() {
		return WebApplicationLoader.getWebApplicationContext();
	}

	/**
	 * @param type
	 *            type
	 * @param methodParameterName
	 *            parameter name
	 */
	public final static BadRequestException newBadRequest(String type, String methodParameterName, Throwable ex) {
		StringBuilder msg = new StringBuilder(64);

		if (StringUtils.isNotEmpty(type)) {
			msg.append(type);
		}
		else {
			msg.append("Parameter");
		}

		msg.append(": [").append(methodParameterName).append("] is required and it can't be resolve, bad request.");

		return new BadRequestException(msg.toString(), ex);
	}

	/**
	 * Download file to client.
	 *
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param download
	 *            file to download
	 * @param downloadFileBuf
	 *            download buff
	 * @since 2.1.x
	 */
	public final static void downloadFile(HttpServletRequest request, //
			HttpServletResponse response, File download, int downloadFileBuf) throws IOException //
	{
		response.setContentLengthLong(download.length());
		response.setContentType(Constant.APPLICATION_FORCE_DOWNLOAD);

		response.setHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
		response.setHeader(Constant.CONTENT_DISPOSITION, new StringBuilder(Constant.ATTACHMENT_FILE_NAME)//
				.append(StringUtils.encodeUrl(download.getName()))//
				.append(Constant.QUOTATION_MARKS)//
				.toString()//
		);

		try (InputStream in = new FileInputStream(download);
				OutputStream out = response.getOutputStream()) {

			byte[] buff = new byte[downloadFileBuf];
			int len = 0;
			while ((len = in.read(buff)) != -1) {
				out.write(buff, 0, len);
			}
		}
	}

}
