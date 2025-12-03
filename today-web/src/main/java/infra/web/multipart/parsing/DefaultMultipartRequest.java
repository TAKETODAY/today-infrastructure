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

package infra.web.multipart.parsing;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.http.HttpHeaders;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.multipart.MaxUploadSizeExceededException;
import infra.web.multipart.NotMultipartRequestException;
import infra.web.multipart.Part;
import infra.web.multipart.support.AbstractMultipartRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:39
 */
final class DefaultMultipartRequest extends AbstractMultipartRequest {

  private final DefaultMultipartParser multipartParser;

  private final RequestContext context;

  public DefaultMultipartRequest(DefaultMultipartParser multipartParser, RequestContext context) {
    this.multipartParser = multipartParser;
    this.context = context;
  }

  @Nullable
  @Override
  public HttpHeaders getMultipartHeaders(String paramOrFileName) {
    List<Part> multipartData = getParts(paramOrFileName);
    if (CollectionUtils.isNotEmpty(multipartData)) {
      return multipartData.get(0).getHeaders();
    }
    return null;
  }

  @Override
  protected MultiValueMap<String, Part> parseRequest() {
    var map = MultiValueMap.<String, Part>forLinkedHashMap();
    try {
      List<Part> parsed = multipartParser.parseRequest(context);
      for (Part part : parsed) {
        map.add(part.getName(), part);
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
