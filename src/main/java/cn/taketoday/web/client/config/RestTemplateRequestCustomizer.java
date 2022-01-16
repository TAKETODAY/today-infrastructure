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

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestInitializer;
import cn.taketoday.web.client.RestTemplate;

/**
 * Callback interface that can be used to customize the {@link ClientHttpRequest} sent
 * from a {@link RestTemplate}.
 *
 * @param <T> the {@link ClientHttpRequest} type
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplateBuilder
 * @see ClientHttpRequestInitializer
 * @since 4.0
 */
@FunctionalInterface
public interface RestTemplateRequestCustomizer<T extends ClientHttpRequest> {

  /**
   * Customize the specified {@link ClientHttpRequest}.
   *
   * @param request the request to customize
   */
  void customize(T request);

}
