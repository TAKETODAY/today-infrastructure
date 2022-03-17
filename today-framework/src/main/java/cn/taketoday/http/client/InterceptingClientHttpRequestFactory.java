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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpRequestFactory} wrapper with support for
 * {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @see ClientHttpRequestFactory
 * @see ClientHttpRequestInterceptor
 * @since 4.0
 */
public class InterceptingClientHttpRequestFactory extends ClientHttpRequestFactoryWrapper {

  private final List<ClientHttpRequestInterceptor> interceptors;

  /**
   * Create a new instance of the {@code InterceptingClientHttpRequestFactory} with the given parameters.
   *
   * @param requestFactory the request factory to wrap
   * @param interceptors the interceptors that are to be applied (can be {@code null})
   */
  public InterceptingClientHttpRequestFactory(
          ClientHttpRequestFactory requestFactory,
          @Nullable List<ClientHttpRequestInterceptor> interceptors) {

    super(requestFactory);
    this.interceptors = interceptors != null ? interceptors : Collections.emptyList();
  }

  @Override
  protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
    return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
  }

}
