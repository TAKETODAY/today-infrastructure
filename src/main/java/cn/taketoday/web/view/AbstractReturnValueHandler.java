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

import javax.imageio.ImageIO;

import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * @author TODAY <br>
 * 2019-07-14 10:47
 */
public abstract class AbstractReturnValueHandler
        extends OrderedSupport implements ReturnValueHandler, RuntimeReturnValueHandler {
  public static final String IMAGE_PNG = "png";

  private int downloadFileBuf;
  /** view resolver **/
  private MessageConverter messageConverter;
  /** Template view resolver */
  private TemplateRenderer templateRenderer;
  /** @since 3.0 */
  private RedirectModelManager modelManager;

  @Override
  public boolean supportsReturnValue(Object result) {
    return false;
  }

  @Override
  public abstract void handleReturnValue(RequestContext context, Object handler, Object result)
          throws Throwable;

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
        handleResponseBody(request, view);
      }
    }
    else {
      handleNull(request);
    }
  }

  protected void handleResponseBody(RequestContext request, Object view) throws IOException {
    obtainMessageConverter().write(request, view);
  }

  protected void handleNull(RequestContext request) {
    // noop
  }

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
      context.sendRedirect(redirect);
    }
    else {
      context.sendRedirect(context.getContextPath().concat(redirect));
    }
  }

  /**
   * @see #REDIRECT_URL_PREFIX
   * @see #RESPONSE_BODY_PREFIX
   */
  public void handleString(final String resource, final RequestContext context) throws Throwable {
    if (resource.startsWith(REDIRECT_URL_PREFIX)) {
      // redirect
      handleRedirect(resource.substring(9), context);
    }
    else if (resource.startsWith(RESPONSE_BODY_PREFIX)) {
      // body
      handleResponseBody(context, resource.substring(5));
    }
    else {
      // template view
      final RedirectModelManager modelManager = getModelManager();
      if (modelManager != null) { // @since 3.0.3 checking model manager
        final RedirectModel redirectModel = modelManager.getModel(context);
        if (redirectModel != null) {
          context.setAttributes(redirectModel.asMap());
          modelManager.applyModel(context, null);
        }
      }
      getTemplateViewResolver().render(resource, context);
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
   *         if an error occurs during writing.
   * @since 2.3.3
   */
  public void handleImageView(final RenderedImage image, final RequestContext context) throws IOException {
    // need set content type
    ImageIO.write(image, IMAGE_PNG, context.getOutputStream());
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

  public TemplateRenderer getTemplateViewResolver() {
    return templateRenderer;
  }

  public void setTemplateViewResolver(TemplateRenderer templateRenderer) {
    this.templateRenderer = templateRenderer;
  }

  public void setModelManager(RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  public RedirectModelManager getModelManager() {
    return modelManager;
  }
}
