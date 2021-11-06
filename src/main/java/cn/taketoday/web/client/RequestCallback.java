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

package cn.taketoday.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import cn.taketoday.http.client.ClientHttpRequest;

/**
 * Callback interface for code that operates on a {@link ClientHttpRequest}.
 * Allows manipulating the request headers, and write to the request body.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for
 * application code. There several available factory methods:
 * <ul>
 * <li>{@link RestTemplate#acceptHeaderRequestCallback(Class)}
 * <li>{@link RestTemplate#httpEntityCallback(Object)}
 * <li>{@link RestTemplate#httpEntityCallback(Object, Type)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @see RestTemplate#execute
 * @since 4.0
 */
@FunctionalInterface
public interface RequestCallback {

  /**
   * Gets called by {@link RestTemplate#execute} with an opened {@code ClientHttpRequest}.
   * Does not need to care about closing the request or about handling errors:
   * this will all be handled by the {@code RestTemplate}.
   *
   * @param request the active HTTP request
   * @throws IOException in case of I/O errors
   */
  void doWithRequest(ClientHttpRequest request) throws IOException;

}
