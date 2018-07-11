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
package cn.taketoday.web.multipart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import cn.taketoday.web.mapping.MethodParameter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月28日 下午9:00:29
 */
@Setter
@Getter
@Slf4j
public final class DefaultMultipartResolver implements MultipartResolver {

	private long	maxUploadSize	= 10240000;
	private String	encoding		= "utf-8";

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		// 多文件上传
		if (methodParameter.getParameterClass() == MultipartFile[].class) {

			Collection<Part> parts = request.getParts();// parts

			Set<MultipartFile> multipartFiles = new HashSet<>();

			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new MultipartFile(part));
				}
			}
			return multipartFiles.toArray(new MultipartFile[0]);
		}
		return new MultipartFile(request.getPart(methodParameterName));
	}

	@Override
	public boolean isMultipart(HttpServletRequest request) {

		if (!"POST".equals(request.getMethod())) {
			return false;
		}
		String contentType = request.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}

	@Override
	public void cleanupMultipart(HttpServletRequest request) {

		try {
			for (Part part : request.getParts()) {
				part.delete();
			}
		} catch (Exception ex) {
			log.error("cleanup cache error", ex);
		}
	}

}
