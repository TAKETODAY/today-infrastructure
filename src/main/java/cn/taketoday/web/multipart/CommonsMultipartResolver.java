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

import cn.taketoday.web.Constant;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.MethodParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import lombok.NoArgsConstructor;

/**
 * 
 * @author Today <br>
 *         2018-07-11 0:57:31
 */
@NoArgsConstructor
public final class CommonsMultipartResolver extends AbstractMultipartResolver {

	private ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());

	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request);
	}

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws BadRequestException {

		servletFileUpload.setHeaderEncoding(encoding);
		servletFileUpload.setFileSizeMax(maxFileSize);
		servletFileUpload.setSizeMax(maxRequestSize);
		try {

			return parseFileItems(servletFileUpload.parseRequest(request), methodParameterName, methodParameter);
		} catch (FileUploadException ex) {
			throw new BadRequestException(
					"Failed to parse multipart servlet request With Msg:[" + ex.getMessage() + "]", ex);
		}
	}

	/**
	 * Parse file items
	 * 
	 * @param fileItems
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private Object parseFileItems(List<FileItem> fileItems, String methodParameterName, MethodParameter methodParameter)
			throws BadRequestException {

		switch (methodParameter.getParameterType())
		{
			case Constant.TYPE_FILE_ITEM :
				if (fileItems.isEmpty()) {
					throw new BadRequestException("There isn't a file item, bad request.");
				}
				for (FileItem fileItem : fileItems) {
					if (methodParameterName.equals(fileItem.getFieldName())) {
						return fileItem;
					}
				}
			case Constant.TYPE_MULTIPART_FILE :
				if (fileItems.isEmpty()) {
					throw new BadRequestException("There isn't a file item, bad request.");
				}
				for (FileItem fileItem : fileItems) {
					if (methodParameterName.equals(fileItem.getFieldName())) {
						return new CommonsMultipartFile(fileItem);
					}
				}
			case Constant.TYPE_ARRAY_MULTIPART_FILE : {
				return multipartFile(fileItems, new ArrayList<>(), methodParameterName)//
						.toArray(new MultipartFile[0]);
			}
			case Constant.TYPE_ARRAY_FILE_ITEM : {
				return fileItem(fileItems, new HashSet<>(), methodParameterName)//
						.toArray(new FileItem[0]);
			}
			case Constant.TYPE_SET_MULTIPART_FILE : {
				return multipartFile(fileItems, new HashSet<>(), methodParameterName);
			}
			case Constant.TYPE_SET_FILE_ITEM : {
				return fileItem(fileItems, new HashSet<>(), methodParameterName);
			}
			case Constant.TYPE_LIST_MULTIPART_FILE : {
				return multipartFile(fileItems, new ArrayList<>(), methodParameterName);
			}
			case Constant.TYPE_LIST_FILE_ITEM : {
				return fileItem(fileItems, new ArrayList<>(), methodParameterName);
			}
			default:
				throw new BadRequestException("Not supported type: [" + methodParameter.getParameterClass() + "]");
		}
	}

	/**
	 * parse a set of {@link MultipartFile}
	 * 
	 * @param fileItems
	 *            upload files
	 * @param multipartFiles
	 *            the collection of {@link MultipartFile}
	 * @param methodParameterName
	 * @return
	 */
	private final Collection<MultipartFile> multipartFile(List<FileItem> fileItems,
			Collection<MultipartFile> multipartFiles, String methodParameterName) {

		for (FileItem fileItem : fileItems) {
			if (methodParameterName.equals(fileItem.getFieldName())) {
				multipartFiles.add(new CommonsMultipartFile(fileItem));
			}
		}
		return multipartFiles;
	}

	/**
	 * parse a set of {@link FileItem}
	 * 
	 * @param fileItems
	 *            upload files
	 * @param multipartFiles
	 *            the collection of {@link FileItem}
	 * @param methodParameterName
	 *            method parameter name method parameter name
	 * @return
	 */
	private final Collection<FileItem> fileItem(List<FileItem> fileItems, Collection<FileItem> multipartFiles,
			String methodParameterName) {
		for (FileItem fileItem : fileItems) {
			if (methodParameterName.equals(fileItem.getFieldName())) {
				multipartFiles.add(fileItem);
			}
		}
		return multipartFiles;
	}

	@Override
	public void cleanupMultipart(HttpServletRequest request) {
		//
	}

}
