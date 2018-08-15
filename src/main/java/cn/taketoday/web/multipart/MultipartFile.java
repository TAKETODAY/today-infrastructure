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
import java.io.Serializable;

/**
 * @author Today
 * @date 2018年7月11日 下午1:02:52
 */
public interface MultipartFile extends Serializable {

	public InputStream getInputStream() throws IOException;

	public String getContentType();

	/**
	 * Return the size of the file in bytes.
	 * @return the size of the file, or 0 if empty
	 */
	public long getSize();

	/**
	 * Gets the name of this part
	 *
	 * @return The name of this part as a <tt>String</tt>
	 */
	public String getName();

	/**
	 * Return the original filename in the client's filesystem.
	 */
	public String getFileName();

	/**
	 * save file
	 * 
	 * @param dest
	 * @return
	 */
	public boolean save(File dest);

	/**
	 * 
	 * @return
	 */
	public boolean isEmpty();

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
import java.io.Serializable;

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
	public InputStream getInputStream() throws IOException;

	/**
	 * Get upload file content type.
	 * 
	 * @return upload file content type
	 */
	public String getContentType();

	/**
	 * Return the size of the file in bytes.
	 * 
	 * @return the size of the file, or 0 if empty
	 */
	public long getSize();

	/**
	 * Gets the name of this part.
	 *
	 * @return The name of this part as a <tt>String</tt>
	 */
	public String getName();

	/**
	 * Return the original filename in the client's filesystem.
	 */
	public String getFileName();

	/**
	 * Save upload file to server.
	 * 
	 * @param dest
	 * @return
	 */
	public boolean save(File dest);

	/**
	 * 
	 * @return
	 */
	public boolean isEmpty();

}
>>>>>>> 2.2.x
