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
package cn.taketoday.web.multipart;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.mapping.MethodParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.slf4j.LoggerFactory;

/**
 * @author Today <br>
 *         2018-06-28 9:00:29
 */
@WebDebugMode
@Singleton(Constant.MULTIPART_RESOLVER)
public class DefaultMultipartResolver extends AbstractMultipartResolver {

	@Override
	public Object resolveMultipart(HttpServletRequest request, //
			String methodParameterName, MethodParameter methodParameter) throws Throwable //
	{
		
		if (getMaxRequestSize() < request.getContentLengthLong()) { // exceed max size?
			throw new FileSizeExceededException(getMaxRequestSize(), null).setActual(request.getContentLengthLong());
		}

		switch (methodParameter.getParameterType())
		{
			case Constant.TYPE_MULTIPART_FILE : {
				return new DefaultMultipartFile(request.getPart(methodParameterName));
			}
			case Constant.TYPE_ARRAY_MULTIPART_FILE : {
				Set<DefaultMultipartFile> multipartFiles = new HashSet<>();
				for (Part part : request.getParts()) {
					if (methodParameterName.equals(part.getName())) {
						multipartFiles.add(new DefaultMultipartFile(part));
					}
				}
				return multipartFiles.toArray(new DefaultMultipartFile[0]);
			}
			case Constant.TYPE_SET_MULTIPART_FILE : {
				Set<DefaultMultipartFile> multipartFiles = new HashSet<>();
				for (Part part : request.getParts()) {
					if (methodParameterName.equals(part.getName())) {
						multipartFiles.add(new DefaultMultipartFile(part));
					}
				}
				return multipartFiles;
			}
			case Constant.TYPE_LIST_MULTIPART_FILE : {
				List<DefaultMultipartFile> multipartFiles = new ArrayList<>();
				for (Part part : request.getParts()) {
					if (methodParameterName.equals(part.getName())) {
						multipartFiles.add(new DefaultMultipartFile(part));
					}
				}
				return multipartFiles;
			}
		}
		throw new BadRequestException("Not supported type: [" + methodParameter.getParameterClass() + "]");
	}

	@Override
	public void cleanupMultipart(HttpServletRequest request) {

		try {

			for (Part part : request.getParts()) {
				part.delete();
			}
		} //
		catch (Exception ex) {
			LoggerFactory.getLogger(DefaultMultipartResolver.class).error("cleanup cache error", ex);
		}
	}

}
