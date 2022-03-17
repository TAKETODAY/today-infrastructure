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

package cn.taketoday.web.client.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInitializer;
import cn.taketoday.util.LambdaSafe;

/**
 * {@link ClientHttpRequestFactory} to apply customizations from the
 * {@link RestTemplateBuilder}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class RestTemplateBuilderClientHttpRequestInitializer implements ClientHttpRequestInitializer {

  private final BasicAuthentication basicAuthentication;
  private final Map<String, List<String>> defaultHeaders;
  private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

  RestTemplateBuilderClientHttpRequestInitializer(
          BasicAuthentication basicAuthentication,
          Map<String, List<String>> defaultHeaders,
          Set<RestTemplateRequestCustomizer<?>> requestCustomizers
  ) {
    this.defaultHeaders = defaultHeaders;
    this.basicAuthentication = basicAuthentication;
    this.requestCustomizers = requestCustomizers;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void initialize(ClientHttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    if (this.basicAuthentication != null) {
      this.basicAuthentication.applyTo(headers);
    }
    for (Map.Entry<String, List<String>> entry : defaultHeaders.entrySet()) {
      headers.putIfAbsent(entry.getKey(), entry.getValue());
    }

    LambdaSafe.callbacks(RestTemplateRequestCustomizer.class, requestCustomizers, request)
            .invoke(customizer -> customizer.customize(request));
  }

}
