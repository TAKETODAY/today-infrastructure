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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commons file upload implement
 * 
 * @author Today
 * @date 2018年7月11日 下午3:48:00
 */
public final class CommonsMultipartFile implements MultipartFile {

	private static final long	serialVersionUID	= -8499057935018080732L;

	protected Logger			log					= LoggerFactory.getLogger(CommonsMultipartFile.class);

	private FileItem			fileItem;

	/**
	 * Create an instance wrapping the given FileItem.
	 * 
	 * @param fileItem
	 *            the FileItem to wrap
	 */
	public CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
	}

	public final FileItem getFileItem() {
		return this.fileItem;
	}

	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public String getContentType() {
		return this.fileItem.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return (this.getSize() == 0);
	}

	@Override
	public long getSize() {
		return this.fileItem.getSize();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.fileItem.getInputStream();
	}

	@Override
	public String getFileName() {
		return fileItem.getName();
	}

	@Override
	public boolean save(File dest) {

		try {
			fileItem.write(dest);
			return true;
		} catch (Exception e) {
			log.error("file [{}] upload failure.", getName(), e);
			return false;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Commons file upload implement.
 * 
 * @author Today <br>
 * 
 *         2018-07-11 15:48:00
 */
@Slf4j
public final class CommonsMultipartFile implements MultipartFile {

	private static final long	serialVersionUID	= -8499057935018080732L;

	private FileItem			fileItem;

	/**
	 * Create an instance wrapping the given FileItem.
	 * 
	 * @param fileItem
	 *            the FileItem to wrap
	 */
	public CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
	}

	public final FileItem getFileItem() {
		return this.fileItem;
	}

	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public String getContentType() {
		return this.fileItem.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return (this.getSize() == 0);
	}

	@Override
	public long getSize() {
		return this.fileItem.getSize();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.fileItem.getInputStream();
	}

	@Override
	public String getFileName() {
		return fileItem.getName();
	}

	@Override
	public boolean save(File dest) {

		try {
			fileItem.write(dest);
			return true;
		} catch (Exception e) {
			log.error("file [{}] upload failure.", getName(), e);
			return false;
		}
	}

}
>>>>>>> 2.2.x
