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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;

/**
 * HTTP/1 {@link io.netty.handler.codec.http.HttpHeaders} view over an existing
 * Netty {@link DefaultHeaders} instance.
 * <p>Response headers are stored in a generic {@link DefaultHeaders} implementation,
 * while {@code DefaultHttpResponse} requires the HTTP/1 {@code HttpHeaders} type.
 * This adapter shares the underlying header store instead of copying every entry.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http1Headers extends DefaultHttpHeaders {

  Http1Headers(DefaultHeaders<CharSequence, @Nullable CharSequence, ?> headers) {
    super(headers);
  }

}
