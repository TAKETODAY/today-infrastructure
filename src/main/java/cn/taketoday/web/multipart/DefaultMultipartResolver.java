<<<<<<< HEAD
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.web.mapping.MethodParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Today
 * @date 2018年6月28日 下午9:00:29
 */
@Setter
@Getter
public final class DefaultMultipartResolver extends AbstractMultipartResolver {

	protected Logger log = LoggerFactory.getLogger(DefaultMultipartResolver.class);

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		Class<?> parameterClass = methodParameter.getParameterClass();
		if (parameterClass == MultipartFile.class) {
			return new DefaultMultipartFile(request.getPart(methodParameterName));
		} else if (parameterClass == MultipartFile[].class) {
			Collection<Part> parts = request.getParts();// parts

			Set<DefaultMultipartFile> multipartFiles = new HashSet<>();

			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles.toArray(new DefaultMultipartFile[0]);
		} else if (parameterClass == Set.class) {

			Collection<Part> parts = request.getParts();// parts
			Set<DefaultMultipartFile> multipartFiles = new HashSet<>();
			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles;
		} else if (parameterClass == List.class) {
			Collection<Part> parts = request.getParts();// parts
			List<DefaultMultipartFile> multipartFiles = new ArrayList<>();
			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles;
		}
		log.error("method parameter setting error.");
		return null;
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
=======
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.web.mapping.MethodParameter;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 *         2018-06-28 9:00:29
 */
@Setter
@Getter
public final class DefaultMultipartResolver extends AbstractMultipartResolver {

	protected Logger log = LoggerFactory.getLogger(DefaultMultipartResolver.class);

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		Class<?> parameterClass = methodParameter.getParameterClass();
		if (parameterClass == MultipartFile.class) {
			return new DefaultMultipartFile(request.getPart(methodParameterName));
		} else if (parameterClass == MultipartFile[].class) {
			Collection<Part> parts = request.getParts();// parts

			Set<DefaultMultipartFile> multipartFiles = new HashSet<>();

			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles.toArray(new DefaultMultipartFile[0]);
		} else if (parameterClass == Set.class) {

			Collection<Part> parts = request.getParts();// parts
			Set<DefaultMultipartFile> multipartFiles = new HashSet<>();
			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles;
		} else if (parameterClass == List.class) {
			Collection<Part> parts = request.getParts();// parts
			List<DefaultMultipartFile> multipartFiles = new ArrayList<>();
			for (Part part : parts) {
				if (methodParameterName.equals(part.getName())) {
					multipartFiles.add(new DefaultMultipartFile(part));
				}
			}
			return multipartFiles;
		}
		log.error("method parameter setting error.");
		return null;
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
>>>>>>> 2.2.x
