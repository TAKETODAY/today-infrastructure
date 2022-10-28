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

package cn.taketoday.http.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="https://square.github.io/okhttp/">OkHttp</a> 3.x to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OkHttp3ClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

  private OkHttpClient client;
  private final boolean defaultClient;

  /**
   * Create a factory with a default {@link OkHttpClient} instance.
   */
  public OkHttp3ClientHttpRequestFactory() {
    this.client = new OkHttpClient();
    this.defaultClient = true;
  }

  /**
   * Create a factory with the given {@link OkHttpClient} instance.
   *
   * @param client the client to use
   */
  public OkHttp3ClientHttpRequestFactory(OkHttpClient client) {
    Assert.notNull(client, "OkHttpClient must not be null");
    this.client = client;
    this.defaultClient = false;
  }

  /**
   * Set the underlying read timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setReadTimeout(int readTimeout) {
    this.client = this.client.newBuilder()
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Set the underlying write timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setWriteTimeout(int writeTimeout) {
    this.client = this.client.newBuilder()
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setConnectTimeout(int connectTimeout) {
    this.client = this.client.newBuilder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new OkHttp3ClientHttpRequest(this.client, uri, httpMethod);
  }

  @Override
  public void destroy() throws IOException {
    if (this.defaultClient) {
      // Clean up the client if we created it in the constructor
      Cache cache = this.client.cache();
      if (cache != null) {
        cache.close();
      }
      this.client.dispatcher().executorService().shutdown();
      this.client.connectionPool().evictAll();
    }
  }

  static Request buildRequest(
          HttpHeaders headers, byte[] content, URI uri, HttpMethod method) throws MalformedURLException {
    MediaType contentType = getContentType(headers);
    RequestBody body = getRequestBody(content, method, contentType);

    Request.Builder builder = new Request.Builder()
            .url(uri.toURL())
            .method(method.name(), body);

    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      for (String headerValue : entry.getValue()) {
        builder.addHeader(headerName, headerValue);
      }
    }

    return builder.build();
  }

  @Nullable
  private static RequestBody getRequestBody(
          byte[] content, HttpMethod method, @Nullable MediaType contentType) {
    return content.length > 0 || okhttp3.internal.http.HttpMethod.requiresRequestBody(method.name())
           ? RequestBody.create(content, contentType) : null;
  }

  @Nullable
  private static MediaType getContentType(HttpHeaders headers) {
    String rawContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
    return StringUtils.hasText(rawContentType) ? MediaType.parse(rawContentType) : null;
  }

}
