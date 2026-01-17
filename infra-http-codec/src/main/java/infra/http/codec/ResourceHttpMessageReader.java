/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.codec;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.codec.ResourceDecoder;
import infra.core.io.Resource;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.util.StringUtils;

/**
 * {@code HttpMessageReader} that wraps and delegates to a {@link ResourceDecoder}
 * that extracts the filename from the {@code "Content-Disposition"} header, if
 * available, and passes it as the {@link ResourceDecoder#FILENAME_HINT}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  protected Map<String, Object> getReadHints(ResolvableType actualType,
          ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

    return getReadHints(elementType, request);
  }

}
