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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * download file, serialize {@link Resource} or {@link File} to client
 *
 * @author TODAY 2019-07-14 11:18
 * @see Resource
 * @see File
 */
public class ResourceReturnValueHandler extends OrderedSupport implements ReturnValueHandler {
  private final int downloadFileBuf;

  public ResourceReturnValueHandler(int downloadFileBuf) {
    this.downloadFileBuf = downloadFileBuf;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return handler instanceof HandlerMethod && supportsHandlerMethod((HandlerMethod) handler);
  }

  private boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    return handlerMethod.isReturnTypeAssignableTo(Resource.class)
            || handlerMethod.isReturnTypeAssignableTo(File.class);
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue instanceof Resource
            || returnValue instanceof File;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    if (returnValue instanceof File) {
      downloadFile(ResourceUtils.getResource((File) returnValue), context);
    }
    else if (returnValue instanceof Resource) {
      downloadFile((Resource) returnValue, context);
    }
  }

  /**
   * Download file to client.
   *
   * @param resource {@link Resource} to download
   * @since 2.1.x
   */
  public void downloadFile(Resource resource, RequestContext context) throws IOException {
    downloadFile(context, resource, downloadFileBuf);
  }

  /**
   * Download file to client.
   *
   * @param file {@link Resource} to download
   * @since 2.1.x
   */
  public void downloadFile(File file, RequestContext context) throws IOException {
    downloadFile(context, ResourceUtils.getResource(file), downloadFileBuf);
  }

  /**
   * Download file to client.
   *
   * @param context Current request context
   * @param download {@link Resource} to download
   * @param bufferSize Download buffer size
   * @since 2.1.x
   */
  public static void downloadFile(
          RequestContext context, Resource download, int bufferSize) throws IOException //
  {
    context.setContentLength(download.contentLength());
    context.setContentType(HttpHeaders.APPLICATION_FORCE_DOWNLOAD);
    HttpHeaders httpHeaders = context.responseHeaders();

    httpHeaders.set(HttpHeaders.CONTENT_TRANSFER_ENCODING, HttpHeaders.BINARY);
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
            new StringBuilder(HttpHeaders.ATTACHMENT_FILE_NAME)
                    .append(URLEncoder.encode(download.getName(), StandardCharsets.UTF_8))
                    .append(Constant.QUOTATION_MARKS)
                    .toString()
    );

    try (InputStream in = download.getInputStream()) {
      StreamUtils.copy(in, context.getOutputStream(), bufferSize);
    }
  }
}
