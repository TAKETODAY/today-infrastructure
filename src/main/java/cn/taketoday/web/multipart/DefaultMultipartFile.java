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

import javax.servlet.http.Part;

/**
 * 
 * @author Today
 * @date 2018年6月28日 下午10:40:32
 */
public final class DefaultMultipartFile implements MultipartFile {

	private static final long serialVersionUID = 2226234093543929729L;

	private Part part;

	public DefaultMultipartFile() {
		
	}

	public DefaultMultipartFile(Part part) {
		this.part = part;
	}

	public InputStream getInputStream() throws IOException {
		return part.getInputStream();
	}

	public String getContentType() {
		return part.getContentType();
	}

	public long getSize() {
		return part.getSize();

	}

	/**
	 * Gets the name of this part
	 *
	 * @return The name of this part as a <tt>String</tt>
	 */
	public String getName() {
		return part.getName();
	}

	/**
	 * Return the original filename in the client's filesystem.
	 */
	public String getFileName() {
		return part.getSubmittedFileName();
	}

	/**
	 * save file
	 * @param dest
	 * @return 
	 */
	public boolean save(File dest) {

		try {
			part.write(dest.getPath());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isEmpty() {
		return part.getSize() == 0;
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

import javax.servlet.http.Part;

/**
 * 
 * @author Today
 * @date 2018年6月28日 下午10:40:32
 */
public final class DefaultMultipartFile implements MultipartFile {

	private static final long serialVersionUID = 2226234093543929729L;

	private Part part;

	public DefaultMultipartFile() {
		
	}

	public DefaultMultipartFile(Part part) {
		this.part = part;
	}

	public InputStream getInputStream() throws IOException {
		return part.getInputStream();
	}

	public String getContentType() {
		return part.getContentType();
	}

	public long getSize() {
		return part.getSize();

	}

	/**
	 * Gets the name of this part
	 *
	 * @return The name of this part as a <tt>String</tt>
	 */
	public String getName() {
		return part.getName();
	}

	/**
	 * Return the original filename in the client's filesystem.
	 */
	public String getFileName() {
		return part.getSubmittedFileName();
	}

	/**
	 * save file
	 * @param dest
	 * @return 
	 */
	public boolean save(File dest) {

		try {
			part.write(dest.getPath());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isEmpty() {
		return part.getSize() == 0;
	}

}
>>>>>>> 2.2.x
