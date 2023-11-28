/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.http.codec;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.codec.ResourceDecoder;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.util.StringUtils;

/**
 * {@code HttpMessageReader} that wraps and delegates to a {@link ResourceDecoder}
 * that extracts the filename from the {@code "Content-Disposition"} header, if
 * available, and passes it as the {@link ResourceDecoder#FILENAME_HINT}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ResourceHttpMessageReader extends DecoderHttpMessageReader<Resource> {

  public ResourceHttpMessageReader() {
    super(new ResourceDecoder());
  }

  public ResourceHttpMessageReader(ResourceDecoder resourceDecoder) {
    super(resourceDecoder);
  }

  @Override
  protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
    String filename = message.getHeaders().getContentDisposition().getFilename();
    return StringUtils.hasText(filename)
           ? Hints.from(ResourceDecoder.FILENAME_HINT, filename)
           : Hints.none();
  }

  @Override
  protected Map<String, Object> getReadHints(
          ResolvableType actualType, ResolvableType elementType,
          ServerHttpRequest request, ServerHttpResponse response) {

    return getReadHints(elementType, request);
  }

}
