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

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.Constant;
import cn.taketoday.context.utils.DataSize;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 *         2018-07-11 12:23:50
 */
@Setter
@Getter
public abstract class AbstractMultipartResolver implements MultipartResolver {

    /*** file upload location */
    private String location = System.getProperty("java.io.tmpdir");

    private String encoding = Constant.DEFAULT_ENCODING;

    /**
     * Maximum size of a single uploaded file.
     */
    private long maxFileSize = DataSize.ofMegabytes(512).toBytes(); // every single file
    private long maxRequestSize = DataSize.ofGigabytes(1).toBytes(); // total size in every single request
    private int fileSizeThreshold = new Long(DataSize.ofGigabytes(1).toBytes()).intValue(); // cache

    @Override
    public boolean isMultipart(HttpServletRequest request) {

        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String contentType = request.getContentType();
        return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
    }

}
