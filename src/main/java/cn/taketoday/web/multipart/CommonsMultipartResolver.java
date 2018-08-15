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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeLimitExceededException;
import cn.taketoday.web.mapping.MethodParameter;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Today
 * @date 2018年7月11日 下午12:57:31
 */
@NoArgsConstructor
public final class CommonsMultipartResolver extends AbstractMultipartResolver {

	
	private Logger log = LoggerFactory.getLogger(CommonsMultipartResolver.class);
	
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new ServletFileUpload(fileItemFactory);
	}

	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request);
	}

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
		servletFileUpload.setHeaderEncoding(encoding);
		servletFileUpload.setFileSizeMax(maxFileSize);
		servletFileUpload.setSizeMax(maxRequestSize);
		try {
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			return parseFileItems(fileItems, methodParameter.getParameterClass(), methodParameterName, methodParameter);

		} catch (FileUploadBase.SizeLimitExceededException ex) {
			log.error("the request was rejected because its size exceeds the configured maximum -> [{}] bytes", maxRequestSize);
			throw new FileSizeLimitExceededException(maxRequestSize, ex);
		} catch (FileUploadBase.FileSizeLimitExceededException ex) {
			log.error("The upload file exceeds its maximum permitted size -> [{}] bytes", methodParameterName, maxFileSize);
			throw new FileSizeLimitExceededException(maxFileSize, ex);
		} catch (FileUploadException ex) {
			log.error("ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
			throw new BadRequestException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * parse file items
	 * @param fileItems
	 * @param parameterClass
	 * @param methodParameterName
	 * @return 
	 * @throws BadRequestException
	 */
	private Object parseFileItems(List<FileItem> fileItems, Class<?> parameterClass, String methodParameterName,
			MethodParameter methodParameter) throws BadRequestException {

		if (parameterClass == MultipartFile.class) {
			if (fileItems.isEmpty()) {
				throw new BadRequestException("bad request.");
			}
			return new CommonsMultipartFile(fileItems.get(0));
		} else if (parameterClass == MultipartFile[].class) {
			Set<CommonsMultipartFile> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles.toArray(new MultipartFile[0]);
		} else if (parameterClass == Set.class) {
			return resolveSet(fileItems, methodParameterName, methodParameter);
		} else if (parameterClass == List.class) {
			return resolveList(fileItems, methodParameterName, methodParameter);
		} else if (parameterClass == FileItem.class) {
			if (fileItems.isEmpty()) {
				throw new BadRequestException("bad request.");
			}
			return fileItems.get(0);
		} else if (parameterClass == FileItem[].class) {
			return fileItems.toArray(new FileItem[0]);
		}
		log.error("method parameter setting error.");
		return null;
	}

	/**
	 * resolve list
	 * @param fileItems
	 * @param methodParameterName
	 * @param methodParameter
	 * @return Return List&lt;MultipartFile&gt; or List&lt;FileItem&gt;
	 */
	private Object resolveList(List<FileItem> fileItems, String methodParameterName, MethodParameter methodParameter) {
		Class<?> genericityClass = methodParameter.getGenericityClass();
		if (genericityClass == MultipartFile.class) {
			List<MultipartFile> multipartFiles = new ArrayList<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles;
		} else if(genericityClass == FileItem.class) {
			List<FileItem> multipartFiles = new ArrayList<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(fileItem);
				}
			}
			return multipartFiles;
		}
		log.error("method parameter setting error.");
		return null;
	}

	/**
	 * 
	 * @param fileItems
	 * @param methodParameterName
	 * @param methodParameter
	 * @return Return Set&lt;MultipartFile&gt; or Set&lt;FileItem&gt;
	 */
	private Object resolveSet(List<FileItem> fileItems, String methodParameterName, MethodParameter methodParameter) {
		Class<?> genericityClass = methodParameter.getGenericityClass();
		if (genericityClass == MultipartFile.class) {
			Set<MultipartFile> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles;
		} else if(genericityClass == FileItem.class) {
			Set<FileItem> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(fileItem);
				}
			}
			return fileItems;
		}
		log.error("method parameter setting error.");
		return null;
	}

	@Override
	public void cleanupMultipart(HttpServletRequest request) {
		//
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeLimitExceededException;
import cn.taketoday.web.mapping.MethodParameter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-07-11 0:57:31
 */
@Slf4j
@NoArgsConstructor
public final class CommonsMultipartResolver extends AbstractMultipartResolver {

	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request);
	}

	@Override
	public Object resolveMultipart(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
		servletFileUpload.setHeaderEncoding(encoding);
		servletFileUpload.setFileSizeMax(maxFileSize);
		servletFileUpload.setSizeMax(maxRequestSize);
		try {
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			return parseFileItems(fileItems, methodParameter.getParameterClass(), methodParameterName, methodParameter);

		} catch (FileUploadBase.SizeLimitExceededException ex) {
			log.error("the request was rejected because its size exceeds the configured maximum -> [{}] bytes",
					maxRequestSize);
			throw new FileSizeLimitExceededException(maxRequestSize, ex);
		} catch (FileUploadBase.FileSizeLimitExceededException ex) {
			log.error("The upload file exceeds its maximum permitted size -> [{}] bytes", methodParameterName,
					maxFileSize);
			throw new FileSizeLimitExceededException(maxFileSize, ex);
		} catch (FileUploadException ex) {
			log.error("ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
			throw new BadRequestException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * Parse file items.
	 * 
	 * @param fileItems
	 * @param parameterClass
	 * @param methodParameterName
	 * @return
	 * @throws BadRequestException
	 */
	private Object parseFileItems(List<FileItem> fileItems, Class<?> parameterClass, String methodParameterName,
			MethodParameter methodParameter) throws BadRequestException {

		if (parameterClass == MultipartFile.class) {
			if (fileItems.isEmpty()) {
				throw new BadRequestException("bad request.");
			}
			return new CommonsMultipartFile(fileItems.get(0));
		} else if (parameterClass == MultipartFile[].class) {
			Set<CommonsMultipartFile> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles.toArray(new MultipartFile[0]);
		} else if (parameterClass == Set.class) {
			return resolveSet(fileItems, methodParameterName, methodParameter);
		} else if (parameterClass == List.class) {
			return resolveList(fileItems, methodParameterName, methodParameter);
		} else if (parameterClass == FileItem.class) {
			if (fileItems.isEmpty()) {
				throw new BadRequestException("bad request.");
			}
			return fileItems.get(0);
		} else if (parameterClass == FileItem[].class) {
			return fileItems.toArray(new FileItem[0]);
		}
		log.error("method parameter setting error.");
		return null;
	}

	/**
	 * resolve list.
	 * 
	 * @param fileItems
	 * @param methodParameterName
	 * @param methodParameter
	 * @return Return List&lt;MultipartFile&gt; or List&lt;FileItem&gt;
	 */
	private Object resolveList(List<FileItem> fileItems, String methodParameterName, MethodParameter methodParameter) {
		Class<?> genericityClass = methodParameter.getGenericityClass();
		if (genericityClass == MultipartFile.class) {
			List<MultipartFile> multipartFiles = new ArrayList<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles;
		} else if (genericityClass == FileItem.class) {
			List<FileItem> multipartFiles = new ArrayList<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(fileItem);
				}
			}
			return multipartFiles;
		}
		log.error("method parameter setting error.");
		return null;
	}

	/**
	 * 
	 * @param fileItems
	 * @param methodParameterName
	 * @param methodParameter
	 * @return Return Set&lt;MultipartFile&gt; or Set&lt;FileItem&gt;
	 */
	private Object resolveSet(List<FileItem> fileItems, String methodParameterName, MethodParameter methodParameter) {
		Class<?> genericityClass = methodParameter.getGenericityClass();
		if (genericityClass == MultipartFile.class) {
			Set<MultipartFile> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(new CommonsMultipartFile(fileItem));
				}
			}
			return multipartFiles;
		} else if (genericityClass == FileItem.class) {
			Set<FileItem> multipartFiles = new HashSet<>();
			for (FileItem fileItem : fileItems) {
				if (methodParameterName.equals(fileItem.getFieldName())) {
					multipartFiles.add(fileItem);
				}
			}
			return fileItems;
		}
		log.error("method parameter setting error.");
		return null;
	}

	@Override
	public void cleanupMultipart(HttpServletRequest request) {
		//
	}

}
>>>>>>> 2.2.x
