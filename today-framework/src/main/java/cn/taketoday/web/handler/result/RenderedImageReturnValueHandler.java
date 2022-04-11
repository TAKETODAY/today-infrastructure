/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.web.handler.result;

import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * Render an image to HTTP response-body
 * <p>
 * Apply contentType {@link #contentType} to HTTP response-header
 * </p>
 *
 * @author TODAY 2019-07-14 15:15
 * @see RenderedImage
 */
public class RenderedImageReturnValueHandler implements HandlerMethodReturnValueHandler {
  public static final String IMAGE_PNG = "png";

  private String formatName = IMAGE_PNG;

  private String contentType = MediaType.IMAGE_PNG_VALUE;

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    return handlerMethod.isReturnTypeAssignableTo(RenderedImage.class);
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue instanceof RenderedImage;
  }

  @Override
  public void handleReturnValue(
          final RequestContext context, final Object handler, final Object returnValue) throws IOException {
    if (returnValue instanceof RenderedImage) {
      write((RenderedImage) returnValue, context);
    }
  }

  public void write(RenderedImage image, RequestContext context) throws IOException {
    context.setContentType(contentType);
    ImageIO.write(image, formatName, context.getOutputStream());
  }

  public void setFormatName(String formatName) {
    Assert.notNull(formatName, "formatName must not be null");
    this.formatName = formatName;
  }

  public String getFormatName() {
    return formatName;
  }

  /**
   * set the response content-type
   *
   * @param contentType response content-type
   */
  public void setContentType(String contentType) {
    Assert.notNull(contentType, "contentType must not be null");
    this.contentType = contentType;
  }

  /**
   * set the response content-type
   *
   * @param contentType response content-type
   */
  public void setContentType(MediaType contentType) {
    Assert.notNull(contentType, "contentType must not be null");
    this.contentType = contentType.toString();
  }

  public String getContentType() {
    return contentType;
  }

}
