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

package cn.taketoday.framework.reactive;

import java.util.function.Supplier;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.support.AbstractMultipartRequest;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:39
 */
public class NettyMultipartRequest extends AbstractMultipartRequest {

  private final RequestContext context;

  final Supplier<InterfaceHttpPostRequestDecoder> decoderSupplier;

  public NettyMultipartRequest(RequestContext context, Supplier<InterfaceHttpPostRequestDecoder> decoderSupplier) {
    this.context = context;
    this.decoderSupplier = decoderSupplier;
  }

  @Nullable
  @Override
  public String getMultipartContentType(String paramOrFileName) {
    return null;
  }

  @Override
  public HttpMethod getRequestMethod() {
    return context.getMethod();
  }

  @Override
  public HttpHeaders getRequestHeaders() {
    return context.requestHeaders();
  }

  @Nullable
  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {

    return null;
  }

  @Override
  protected void initializeMultipart() {
    super.initializeMultipart();
  }

  protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
    DefaultMultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.fromLinkedHashMap();
    for (InterfaceHttpData data : decoderSupplier.get().getBodyHttpDatas()) {
      if (data instanceof FileUpload) {
        String name = data.getName();
        multipartFiles.add(name, new FileUploadMultipartFile((FileUpload) data));
      }
    }
    return multipartFiles;
  }
}
