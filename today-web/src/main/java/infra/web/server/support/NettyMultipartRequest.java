/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import java.util.List;

import infra.http.HttpHeaders;
import infra.lang.Nullable;
import infra.util.MultiValueMap;
import infra.web.bind.NotMultipartRequestException;
import infra.web.multipart.MaxUploadSizeExceededException;
import infra.web.multipart.Multipart;
import infra.web.multipart.support.AbstractMultipartRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:39
 */
final class NettyMultipartRequest extends AbstractMultipartRequest {

  private final NettyRequestContext context;

  public NettyMultipartRequest(NettyRequestContext context) {
    this.context = context;
  }

  @Nullable
  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    List<InterfaceHttpData> bodyHttpList = context.requestDecoder().getBodyHttpDatas(paramOrFileName);
    if (bodyHttpList != null) {
      HttpHeaders headers = HttpHeaders.forWritable();
      for (InterfaceHttpData bodyHttpData : bodyHttpList) {
        if (bodyHttpData instanceof FileUpload httpData) {
          String contentType = httpData.getContentType();
          headers.setOrRemove(HttpHeaders.CONTENT_TYPE, contentType);
          break;
        }
      }
      return headers;
    }
    return null;
  }

  @Override
  protected MultiValueMap<String, Multipart> parseRequest() {
    var map = MultiValueMap.<String, Multipart>forLinkedHashMap();
    try {
      for (InterfaceHttpData data : context.requestDecoder().getBodyHttpDatas()) {
        if (data instanceof FileUpload fileUpload) {
          map.add(data.getName(), new NettyMultipartFile(fileUpload));
        }
        else if (data instanceof Attribute attribute) {
          NettyFormData nettyFormData = new NettyFormData(attribute);
          map.add(attribute.getName(), nettyFormData);
        }
      }
      return map;
    }
    catch (HttpPostRequestDecoder.TooLongFormFieldException e) {
      throw new MaxUploadSizeExceededException(-1, e);
    }
    catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e) {
      throw new NotMultipartRequestException("Not enough data", e);
    }
  }
}
