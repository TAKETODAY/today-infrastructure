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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.utils;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.WebMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-14 16:28
 */
public abstract class ResultUtils {

    public static SerializerFeature[] SERIALIZE_FEATURES = { //
            SerializerFeature.WriteMapNullValue, //
            SerializerFeature.WriteNullListAsEmpty, //
            SerializerFeature.DisableCircularReferenceDetect//
    };

    /**
     * Download file to client.
     *
     * @param request
     *            Current request context
     * @param download
     *            {@link Resource} to download
     * @param bufferSize
     *            Download buffer size
     * @since 2.1.x
     */
    public final static void downloadFile(final RequestContext requestContext, //
            final Resource download, final int bufferSize) throws IOException //
    {
        requestContext.contentLength(download.contentLength());
        requestContext.contentType(Constant.APPLICATION_FORCE_DOWNLOAD);

        requestContext.responseHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
        requestContext.responseHeader(Constant.CONTENT_DISPOSITION, new StringBuilder(Constant.ATTACHMENT_FILE_NAME)//
                .append(StringUtils.encodeUrl(download.getName()))//
                .append(Constant.QUOTATION_MARKS)//
                .toString()//
        );

        try (final InputStream in = download.getInputStream()) {

            WebUtils.writeToOutputStream(in, requestContext.getOutputStream(), bufferSize);
        }
    }

    public static void resolveException(//
            final RequestContext requestContext,
            final ExceptionResolver exceptionResolver, //
            final WebMapping webMapping, Throwable exception) throws ServletException //
    {
        try {
            exceptionResolver.resolveException(requestContext, ExceptionUtils.unwrapThrowable(exception), webMapping);
        }
        catch (Throwable e) {
            throw new ServletException(e);
        }
    }

    public static void resolveRedirect(final String redirect, final RequestContext requestContext) throws IOException {

        if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
            requestContext.redirect(redirect);
        }
        else {
            requestContext.redirect(requestContext.contextPath() + redirect);
        }
    }

    public static void resolveView(//
            final String resource, //
            final ViewResolver viewResolver,
            final RequestContext requestContext) throws Throwable//
    {
        if (resource.startsWith(Constant.REDIRECT_URL_PREFIX)) {
            resolveRedirect(resource.substring(Constant.REDIRECT_URL_PREFIX_LENGTH), requestContext);
        }
        else {

            final RedirectModel redirectModel = requestContext.redirectModel();

            if (redirectModel != null) {
                for (final Entry<String, Object> entry : redirectModel.asMap().entrySet()) {
                    requestContext.attribute(entry.getKey(), entry.getValue());
                }
                requestContext.redirectModel(null);
            }
            viewResolver.resolveView(resource, requestContext);
        }
    }

    /**
     * Resolve image
     * 
     * @param requestContext
     *            Current request context
     * @param image
     *            Image instance
     * @throws IOException
     * @since 2.3.3
     */
    public static void resolveImage(final RequestContext requestContext, final RenderedImage image) throws IOException {
        // need set content type
        ImageIO.write(image, Constant.IMAGE_PNG, requestContext.getOutputStream());
    }

    public static void responseBody(RequestContext requestContext, Object result) throws IOException {

        if (result instanceof CharSequence) {
            requestContext.getWriter().write(((CharSequence) result).toString());
        }
        else {
            requestContext.contentType(Constant.CONTENT_TYPE_JSON);
            JSON.writeJSONString(requestContext.getWriter(), result, SERIALIZE_FEATURES); // TODO message converter
        }
    }
}
