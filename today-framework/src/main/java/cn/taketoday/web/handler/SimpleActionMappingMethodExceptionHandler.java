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

package cn.taketoday.web.handler;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCapable;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * write 'classpath:error/xxx.png' for RenderedImage
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/3 11:28
 */
@Experimental
public class SimpleActionMappingMethodExceptionHandler extends AbstractActionMappingMethodExceptionHandler {

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context,
          @Nullable HandlerMethod handlerMethod, Throwable ex) throws Exception {
    if (handlerMethod != null) {
      context.setStatus(getErrorStatusValue(ex));

      if (handlerMethod.isReturnTypeAssignableTo(RenderedImage.class)) {
        ClassPathResource pathResource = new ClassPathResource(getErrorStatusValue(ex) + ".png", getClass());
        if (pathResource.exists()) {
          context.setContentType(MediaType.IMAGE_JPEG_VALUE);
          return ImageIO.read(pathResource.getInputStream());
        }
      }
      else if (handlerMethod.isResponseBody()) {
        writeErrorMessage(ex, context);
        return NONE_RETURN_VALUE;
      }
      return null; // next
    }
    return null; // next
  }

  /**
   * Write error message to request context, default is write json
   *
   * @param ex Throwable that occurred in request handler
   * @param context current request context
   */
  protected void writeErrorMessage(Throwable ex, RequestContext context) throws IOException {
    context.setContentType(MediaType.APPLICATION_JSON_VALUE);
    PrintWriter writer = context.getWriter();
    writer.write(buildDefaultErrorMessage(ex));
    writer.flush();
  }

  protected String buildDefaultErrorMessage(Throwable ex) {
    return new StringBuilder()
            .append("{\"message\":\"")
            .append(ex.getMessage())
            .append("\"}")
            .toString();
  }

  /**
   * Get error http status value, if target throwable is {@link HttpStatusCapable}
   * its return from {@link HttpStatusCapable#getStatus()}
   *
   * @param ex Throwable that occurred in request handler
   * @return Http status code
   */
  public int getErrorStatusValue(Throwable ex) {
    if (ex instanceof HttpStatusCapable) { // @since 3.0.1
      HttpStatus httpStatus = ((HttpStatusCapable) ex).getStatus();
      return httpStatus.value();
    }
    return HandlerMethod.getStatusValue(ex);
  }

}
