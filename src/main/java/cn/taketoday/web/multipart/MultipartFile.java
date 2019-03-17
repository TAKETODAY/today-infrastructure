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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;

/**
 * 
 * @author Today <br>
 * 
 *         2018-07-11 13:02:52
 */
public interface MultipartFile extends Serializable {

	/**
	 * Get upload file input stream.
	 * 
	 * @return upload file input stream
	 * @throws IOException
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * Get upload file content type.
	 * 
	 * @return upload file content type
	 */
	String getContentType();

	/**
	 * Return the size of the file in bytes.
	 * 
	 * @return the size of the file, or 0 if empty
	 */
	long getSize();

	/**
	 * Gets the name of this part.
	 *
	 * @return The name of this part as a <tt>String</tt>
	 */
	String getName();

	/**
	 * Return the original filename in the client's file system.
	 */
	String getFileName();

	/**
	 * Save upload file to server.
	 * 
	 * @param dest
	 *            the file path
	 * @return
	 */
	boolean save(File dest);

	/**
	 * 
	 * @return
	 */
	boolean isEmpty();

	/**
	 * Returns the contents of the file item as an array of bytes.
	 * 
	 * @since 2.3.3
	 * @throws IOException
	 */
	byte[] getBytes() throws IOException;

	/**
	 * Get {@link Part} or {@link FileItem} ...
	 * 
	 * @since 2.3.3
	 * @return
	 */
	Object getOriginalResource();

	/**
	 * Deletes the underlying storage for a file item, including deleting any
	 * associated temporary disk file.
	 * 
	 * @since 2.3.3
	 * @throws IOException
	 *             if an error occurs.
	 */
	public void delete() throws IOException;

}
