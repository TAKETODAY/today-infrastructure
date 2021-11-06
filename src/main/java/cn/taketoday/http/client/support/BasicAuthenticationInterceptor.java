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

package cn.taketoday.http.client.support;

import java.io.IOException;
import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.client.ClientHttpRequestExecution;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpRequestInterceptor} to apply a given HTTP Basic Authentication
 * username/password pair, unless a custom {@code Authorization} header has
 * already been set.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see HttpHeaders#setBasicAuth
 * @see HttpHeaders#AUTHORIZATION
 * @since 4.0
 */
public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

  private final String encodedCredentials;

  /**
   * Create a new interceptor which adds Basic Authentication for the
   * given username and password.
   *
   * @param username the username to use
   * @param password the password to use
   * @see HttpHeaders#setBasicAuth(String, String)
   * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
   */
  public BasicAuthenticationInterceptor(String username, String password) {
    this(username, password, null);
  }

  /**
   * Create a new interceptor which adds Basic Authentication for the
   * given username and password, encoded using the specified charset.
   *
   * @param username the username to use
   * @param password the password to use
   * @param charset the charset to use
   * @see HttpHeaders#setBasicAuth(String, String, Charset)
   * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
   */
  public BasicAuthenticationInterceptor(String username, String password, @Nullable Charset charset) {
    this.encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
  }

  @Override
  public ClientHttpResponse intercept(
          HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    HttpHeaders headers = request.getHeaders();
    if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
      headers.setBasicAuth(this.encodedCredentials);
    }
    return execution.execute(request, body);
  }

}
