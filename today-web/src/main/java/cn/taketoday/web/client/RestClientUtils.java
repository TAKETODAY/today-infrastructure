/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client;

import java.io.IOException;
import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileCopyUtils;

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
    return new byte[0];
  }

  @Nullable
  public static Charset getCharset(HttpMessage response) {
    HttpHeaders headers = response.getHeaders();
    MediaType contentType = headers.getContentType();
    return contentType != null ? contentType.getCharset() : null;
  }
}
