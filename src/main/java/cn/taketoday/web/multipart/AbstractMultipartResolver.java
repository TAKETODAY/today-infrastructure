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

import lombok.Getter;
import lombok.Setter;

/**
 * @author Today
 * @date 2018年7月11日 下午12:23:50
 */
@Setter
@Getter
public abstract class AbstractMultipartResolver implements MultipartResolver {

	protected String	location			= System.getProperty("java.io.tmpdir");

	protected String	encoding			= "UTF-8";
	protected long		maxFileSize			= 2048000;								// every single
	protected long		maxRequestSize		= 204800000;							// total size
	protected int		fileSizeThreshold	= 2048000000;							// cache

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

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 * 		2018-07-11 12:23:50
 */
@Setter
@Getter
public abstract class AbstractMultipartResolver implements MultipartResolver {

	protected String	location			= System.getProperty("java.io.tmpdir");

	protected String	encoding			= "UTF-8";
	protected long		maxFileSize			= 2048000;								// every single file
	protected long		maxRequestSize		= 204800000;							// total size in every single request
	protected int		fileSizeThreshold	= 2048000000;							// cache
	
	@Override
	public boolean isMultipart(HttpServletRequest request) {

		if (!"POST".equals(request.getMethod())) {
			return false;
		}
		String contentType = request.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}
	
}
>>>>>>> 2.2.x
