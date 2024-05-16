/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.server.support;

import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.support.AbstractMultipartRequest;
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

  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    List<InterfaceHttpData> bodyHttpDatas = context.requestDecoder().getBodyHttpDatas(paramOrFileName);
    if (bodyHttpDatas != null) {
      HttpHeaders headers = HttpHeaders.forWritable();
      for (InterfaceHttpData bodyHttpData : bodyHttpDatas) {
        if (bodyHttpData instanceof FileUpload httpData) {
          String contentType = httpData.getContentType();
          headers.set(HttpHeaders.CONTENT_TYPE, contentType);
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
