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
package cn.taketoday.web.view;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.WebMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-14 10:47
 */
public abstract class AbstractViewResolver implements ViewResolver {

    private int downloadFileBuf;
    /** view resolver **/
    private MessageConverter messageConverter;
    /** Template view resolver */
    private TemplateViewResolver templateViewResolver;

    public AbstractViewResolver() {

    }

    public AbstractViewResolver(TemplateViewResolver viewResolver, MessageConverter messageConverter, int downloadFileBuf) {
        this.setTemplateViewResolver(viewResolver);
        this.setDownloadFileBufferSize(downloadFileBuf);
        this.setMessageConverter(messageConverter);
    }

    public void resolveObject(final RequestContext requestContext, final Object view) throws Throwable {

        if (view instanceof String) {
            resolveView((String) view, getTemplateViewResolver(), requestContext);
        }
        else if (view instanceof File) {
            downloadFile(requestContext, ResourceUtils.getResource((File) view), getDownloadFileBufferSize());
        }
        else if (view instanceof Resource) {
            downloadFile(requestContext, (Resource) view, getDownloadFileBufferSize());
        }
        else if (view instanceof ModelAndView) {
            resolveModelAndView(requestContext, (ModelAndView) view);
        }
        else if (view instanceof RenderedImage) {
            resolveImage(requestContext, (RenderedImage) view);
        }
        else {
            getMessageConverter().write(requestContext, view);
        }
    }

    /**
     * Resolve {@link ModelAndView} return type
     * 
     * @since 2.3.3
     */
    public void resolveModelAndView(final RequestContext requestContext, final ModelAndView modelAndView) throws Throwable {
        if (modelAndView.hasView()) {
            resolveObject(requestContext, modelAndView.getView());
        }
    }

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
    public final static void downloadFile(final RequestContext context,
                                          final Resource download, final int bufferSize) throws IOException //
    {
        context.contentLength(download.contentLength());
        context.contentType(Constant.APPLICATION_FORCE_DOWNLOAD);

        context.responseHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
        context.responseHeader(Constant.CONTENT_DISPOSITION, new StringBuilder(Constant.ATTACHMENT_FILE_NAME)//
                .append(StringUtils.encodeUrl(download.getName()))//
                .append(Constant.QUOTATION_MARKS)//
                .toString()//
        );

        try (final InputStream in = download.getInputStream()) {

            WebUtils.writeToOutputStream(in, context.getOutputStream(), bufferSize);
        }
    }

    public static void resolveException(final RequestContext context,
                                        final ExceptionResolver resolver, //
                                        final WebMapping webMapping, final Throwable exception) throws Throwable //
    {
        resolver.resolveException(context, ExceptionUtils.unwrapThrowable(exception), webMapping);
    }

    public static void resolveRedirect(final String redirect, final RequestContext context) throws IOException {

        if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
            context.redirect(redirect);
        }
        else {
            context.redirect(context.contextPath() + redirect);
        }
    }

    public static void resolveView(final String resource, //
                                   final TemplateViewResolver viewResolver,
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

    public int getDownloadFileBufferSize() {
        return downloadFileBuf;
    }

    public void setDownloadFileBufferSize(int downloadFileBuf) {
        this.downloadFileBuf = downloadFileBuf;
    }

    public MessageConverter getMessageConverter() {
        return messageConverter;
    }

    public void setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    public TemplateViewResolver getTemplateViewResolver() {
        return templateViewResolver;
    }

    public void setTemplateViewResolver(TemplateViewResolver templateViewResolver) {
        this.templateViewResolver = templateViewResolver;
    }

}
