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
package cn.taketoday.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * 
 * @author TODAY <br>
 *         2019-03-15 19:53
 * @since 2.3.7
 */
public abstract class WebUtils {

    private static WebApplicationContext applicationContext;

    /**
     * Get {@link WebApplicationContext}
     * 
     * @return WebApplicationContext
     */
    public final static WebApplicationContext getWebApplicationContext() {
        return applicationContext;
    }

    public static void setWebApplicationContext(WebApplicationContext applicationContext) {
        WebUtils.applicationContext = applicationContext;
    }

    /**
     * @param type
     *            type
     * @param methodParameterName
     *            parameter name
     */
    public final static BadRequestException newBadRequest(String type, MethodParameter parameter, Throwable ex) {
        return newBadRequest(type, parameter.getName(), ex);
    }

    /**
     * @param type
     *            type
     * @param methodParameterName
     *            parameter name
     */
    public final static BadRequestException newBadRequest(String type, String methodParameterName, Throwable ex) {
        StringBuilder msg = new StringBuilder(64);

        if (StringUtils.isNotEmpty(type)) {
            msg.append(type);
        }
        else {
            msg.append("Parameter");
        }

        msg.append(": [").append(methodParameterName).append("] is required and it can't be resolve, bad request.");

        return new BadRequestException(msg.toString(), ex);
    }

    /**
     * Write to {@link OutputStream}
     * 
     * @param source
     *            {@link InputStream}
     * @param out
     *            {@link OutputStream}
     * @param bufferSize
     *            buffer size
     * @throws IOException
     *             if any IO exception occurred
     */
    public static void writeToOutputStream(final InputStream source, //
            final OutputStream out, final int bufferSize) throws IOException //
    {
        final byte[] buff = new byte[bufferSize];
        int len = 0;
        while ((len = source.read(buff)) != -1) {
            out.write(buff, 0, len);
        }
    }

    /**
     * Resolves the content type of the file.
     *
     * @param filename
     *            name of file or path
     * @return file content type
     * @since 2.3.7
     */
    public static String resolveFileContentType(String filename) {
        return URLConnection.getFileNameMap().getContentTypeFor(filename);
    }

    public static String getEtag(String name, long size, long lastModifid) {
        return new StringBuilder()//
                .append(name)//
                .append(Constant.PATH_SEPARATOR)//
                .append(size)//
                .append(Constant.PATH_SEPARATOR)//
                .append(lastModifid).toString();
    }

    // ---
    public static boolean isMultipart(final RequestContext requestContext) {

        if (!"POST".equals(requestContext.method())) {
            return false;
        }
        final String contentType = requestContext.contentType();
        return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
    }

}
