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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

import infra.http.HttpInputMessage;
import infra.http.HttpMessage;
import infra.http.MediaType;
import infra.lang.Constant;
import infra.util.FileCopyUtils;

/**
 * Internal methods shared between types in this package.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class RestClientUtils {

  public static byte[] getBody(HttpInputMessage message) {
    try {
      return FileCopyUtils.copyToByteArray(message.getBody());
    }
    catch (IOException ignore) {
    }
    return Constant.EMPTY_BYTES;
  }

  @Nullable
  public static Charset getCharset(HttpMessage response) {
    MediaType contentType = response.getContentType();
    return contentType != null ? contentType.getCharset() : null;
  }
}
