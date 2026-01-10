/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import infra.http.MediaType;
import infra.lang.Assert;
import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;

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
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(RenderedImage.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof RenderedImage;
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws IOException {
    if (returnValue instanceof RenderedImage renderedImage) {
      write(renderedImage, context);
    }
  }

  public void write(RenderedImage image, RequestContext context) throws IOException {
    context.setContentType(contentType);
    ImageIO.write(image, formatName, context.getOutputStream());
  }

  public void setFormatName(String formatName) {
    Assert.notNull(formatName, "formatName is required");
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
    Assert.notNull(contentType, "contentType is required");
    this.contentType = contentType;
  }

  /**
   * set the response content-type
   *
   * @param contentType response content-type
   */
  public void setContentType(MediaType contentType) {
    Assert.notNull(contentType, "contentType is required");
    this.contentType = contentType.toString();
  }

  public String getContentType() {
    return contentType;
  }

}
