/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on the Java {@link HttpClient}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 19:57
 */
class JdkClientHttpResponse implements ClientHttpResponse {

  private final HttpResponse<InputStream> response;

  private final HttpHeaders headers;

  private final InputStream body;

  public JdkClientHttpResponse(HttpResponse<InputStream> response) {
    this.response = response;
    this.headers = adaptHeaders(response);
    InputStream inputStream = response.body();
    this.body = (inputStream != null) ? inputStream : InputStream.nullInputStream();
  }

  private static HttpHeaders adaptHeaders(HttpResponse<?> response) {
    Map<String, List<String>> rawHeaders = response.headers().map();
    Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ENGLISH);
    MultiValueMap<String, String> multiValueMap = MultiValueMap.from(map);
    multiValueMap.putAll(rawHeaders);
    return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.response.statusCode());
  }

  @Override
  public String getStatusText() {
    // HttpResponse does not expose status text
    if (getStatusCode() instanceof HttpStatus status) {
      return status.getReasonPhrase();
    }
    else {
      return "";
    }
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.body;
  }

  @Override
  public void close() {
    try {
      try {
        StreamUtils.drain(this.body);
      }
      finally {
        this.body.close();
      }
    }
    catch (IOException ignored) { }
  }
}
