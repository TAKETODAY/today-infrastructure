/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY <br>
 * 2019-07-14 10:47
 */
public abstract class AbstractResultHandler
        extends OrderedSupport implements ResultHandler, RuntimeResultHandler {

  private int downloadFileBuf;
  /** view resolver **/
  private MessageConverter messageConverter;
  /** Template view resolver */
  private TemplateViewResolver templateViewResolver;

  protected AbstractResultHandler() {}

  protected AbstractResultHandler(TemplateViewResolver viewResolver,
                                  MessageConverter messageConverter,
                                  int downloadFileBuf) {
    setTemplateViewResolver(viewResolver);
    setMessageConverter(messageConverter);
    setDownloadFileBufferSize(downloadFileBuf);
  }

  @Override
  public boolean supportsResult(Object result) {
    return false;
  }

  public void handleObject(final RequestContext request, final Object view) throws Throwable {
    if (view != null) {
      if (view instanceof String) {
        handleString((String) view, request);
      }
      else if (view instanceof File) {
        downloadFile(request, ResourceUtils.getResource((File) view));
      }
      else if (view instanceof Resource) {
        downloadFile(request, (Resource) view);
      }
      else if (view instanceof ModelAndView) {
        resolveModelAndView(request, (ModelAndView) view);
      }
      else if (view instanceof RenderedImage) {
        handleImageView((RenderedImage) view, request);
      }
      else {
        obtainMessageConverter().write(request, view);
      }
    }
    else {
      handleNull(request);
    }
  }

  protected void handleNull(RequestContext request) { }

  /**
   * Resolve {@link ModelAndView} return type
   *
   * @since 2.3.3
   */
  public void resolveModelAndView(final RequestContext context, final ModelAndView modelAndView) throws Throwable {
    if (modelAndView != null && modelAndView.hasView()) {
      handleObject(context, modelAndView.getView());
    }
  }

  /**
   * Download file to client.
   *
   * @param download
   *         {@link Resource} to download
   *
   * @since 2.1.x
   */
  public void downloadFile(final RequestContext context, final Resource download) throws IOException {
    WebUtils.downloadFile(context, download, getDownloadFileBufferSize());
  }

  public void handleRedirect(final String redirect, final RequestContext context) throws IOException {

    if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
      context.redirect(redirect);
    }
    else {
      context.redirect(context.contextPath().concat(redirect));
    }
  }

  public void handleString(final String resource, final RequestContext context) throws Throwable {

    if (resource.startsWith(Constant.REDIRECT_URL_PREFIX)) {
      handleRedirect(resource.substring(9), context);
    }
    else if (resource.startsWith(Constant.RESPONSE_BODY_PREFIX)) {
      getMessageConverter().write(context, resource.substring(5));
    }
    else {
      final RedirectModel redirectModel = context.redirectModel();
      if (redirectModel != null) {
        for (final Entry<String, Object> entry : redirectModel.asMap().entrySet()) {
          context.attribute(entry.getKey(), entry.getValue());
        }
        context.applyRedirectModel(null);
      }
      getTemplateViewResolver().resolveView(resource, context);
    }
  }

  /**
   * Resolve image
   *
   * @param context
   *         Current request context
   * @param image
   *         Image instance
   *
   * @throws IOException
   * @since 2.3.3
   */
  public void handleImageView(final RenderedImage image, final RequestContext context) throws IOException {
    // need set content type
    ImageIO.write(image, Constant.IMAGE_PNG, context.getOutputStream());
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

  public MessageConverter obtainMessageConverter() {
    final MessageConverter converter = getMessageConverter();
    Assert.state(converter != null, "No MessageConverter.");
    return converter;
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
