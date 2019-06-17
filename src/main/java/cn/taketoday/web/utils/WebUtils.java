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

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.WebMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * 
 * @author TODAY <br>
 *         2019-03-15 19:53
 * @since 2.3.7
 */
public abstract class WebUtils {

    public static SerializerFeature[] SERIALIZE_FEATURES = { //
            SerializerFeature.WriteMapNullValue, //
            SerializerFeature.WriteNullListAsEmpty, //
            SerializerFeature.DisableCircularReferenceDetect//
    };

    /**
     * Get {@link ServletContext}
     * 
     * @return ServletContext
     */
    public final static ServletContext getServletContext() {
        return WebApplicationLoader.getWebApplicationContext().getServletContext();
    }

    /**
     * Get {@link WebApplicationContext}
     * 
     * @return WebApplicationContext
     */
    public final static WebApplicationContext getWebApplicationContext() {
        return WebApplicationLoader.getWebApplicationContext();
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
     * Download file to client.
     *
     * @param request
     *            current request
     * @param response
     *            current response
     * @param download
     *            file to download
     * @param downloadFileBuf
     *            download buff
     * @since 2.1.x
     */
    public final static void downloadFile(HttpServletRequest request, //
            HttpServletResponse response, File download, int downloadFileBuf) throws IOException //
    {
        response.setContentLengthLong(download.length());
        response.setContentType(Constant.APPLICATION_FORCE_DOWNLOAD);

        response.setHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
        response.setHeader(Constant.CONTENT_DISPOSITION, new StringBuilder(Constant.ATTACHMENT_FILE_NAME)//
                .append(StringUtils.encodeUrl(download.getName()))//
                .append(Constant.QUOTATION_MARKS)//
                .toString()//
        );

        try (InputStream in = new FileInputStream(download);
                OutputStream out = response.getOutputStream()) {

            writeToOutputStream(in, out, downloadFileBuf);
        }
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
    public static void writeToOutputStream(InputStream source, OutputStream out, int bufferSize) throws IOException {
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

    // ------------

    public static void resolveException(HttpServletRequest request, final HttpServletResponse response, //
            ServletContext servletContext, ExceptionResolver exceptionResolver, WebMapping webMapping, Throwable exception)
            throws ServletException //
    {
        try {
            exception = ExceptionUtils.unwrapThrowable(exception);
            exceptionResolver.resolveException(request, response, exception, webMapping);
            servletContext.log("Catch Throwable: [" + exception + "] With Msg: [" + exception.getMessage() + "]", exception);
        }
        catch (Throwable e) {
            servletContext.log(
                    "Handling of [" + exception.getClass().getName() + "]  resulted in Exception: [" + e.getClass().getName() + "]", e);
            throw new ServletException(e);
        }
    }

    /**
     * Resolve String type
     *
     * @param request
     *            current request
     * @param response
     *            current response
     * @param result
     *            String value
     * @since 2.3.3
     */
    @SuppressWarnings("unchecked")
    public static void resolveView(//
            final HttpServletRequest request, //
            final HttpServletResponse response, //
            final String resource, //
            final String contextPath, //
            final ViewResolver viewResolver, //
            final Map<String, Object> dataModel) throws Throwable //
    {
        if (resource.startsWith(Constant.REDIRECT_URL_PREFIX)) {
            // @since 2.3.7
            final String redirect = resource.substring(Constant.REDIRECT_URL_PREFIX_LENGTH);
            if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
                response.sendRedirect(redirect);
            }
            else {
                response.sendRedirect(contextPath + redirect);
            }
            return;
        }
        if (dataModel != null) {
            dataModel.forEach(request::setAttribute);
        }
        {
            final HttpSession session = request.getSession();
            final Object attribute = session.getAttribute(Constant.KEY_REDIRECT_MODEL);
            if (attribute instanceof Map) {
                ((Map<String, Object>) attribute).forEach(request::setAttribute);
                session.removeAttribute(Constant.KEY_REDIRECT_MODEL);
            }
        }

        viewResolver.resolveView(resource, request, response);
    }

    /**
     * 
     * @param request
     * @param response
     * @param resource
     * @param contextPath
     * @param viewResolver
     * @throws Throwable
     */
    public static void resolveView(HttpServletRequest request, HttpServletResponse response,
            String resource, String contextPath, ViewResolver viewResolver) throws Throwable //
    {
        resolveView(request, response, resource, contextPath, viewResolver, null);
    }

    /**
     * @param request
     *            current request
     * @param response
     *            current response
     * @param result
     *            result instance
     * @param viewResolver
     * @throws Throwable
     */
    public static void resolveObject(//
            final HttpServletRequest request, //
            final HttpServletResponse response, //
            final Object result, //
            final ViewResolver viewResolver, //
            final int downloadFileBuf) throws Throwable //
    {
        if (result instanceof String) {
            WebUtils.resolveView(request, response, (String) result, request.getContextPath(), viewResolver);
            return;
        }
        else if (result instanceof StringBuilder || result instanceof StringBuffer) {
            response.getWriter().print(result.toString());
            return;
        }
        else if (result instanceof RenderedImage) {
            resolveImage(response, (RenderedImage) result);
            return;
        }
        else if (result instanceof File) {
            WebUtils.downloadFile(request, response, (File) result, downloadFileBuf);
            return;
        }
        resolveJsonView(response, result);
    }

    /**
     * Resolve json view
     * 
     * @param response
     *            current response
     * @param view
     *            view instance
     * @throws IOException
     */
    public static void resolveJsonView(final HttpServletResponse response, final Object view) throws IOException {
        response.setContentType(Constant.CONTENT_TYPE_JSON);
        JSON.writeJSONString(response.getWriter(), view, SERIALIZE_FEATURES);
    }

    /**
     * Resolve image
     * 
     * @param response
     *            current response
     * @param image
     *            image instance
     * @throws IOException
     * @since 2.3.3
     */
    public static void resolveImage(final HttpServletResponse response, final RenderedImage image) throws IOException {
        // need set content type
        ImageIO.write(image, Constant.IMAGE_PNG, response.getOutputStream());
    }
}
