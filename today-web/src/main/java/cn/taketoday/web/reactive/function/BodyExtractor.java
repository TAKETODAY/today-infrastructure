/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.reactive.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.server.reactive.ServerHttpResponse;

/**
 * A function that can extract data from a {@link ReactiveHttpInputMessage} body.
 *
 * @param <T> the type of data to extract
 * @param <M> the type of {@link ReactiveHttpInputMessage} this extractor can be applied to
 * @author Arjen Poutsma
 * @see BodyExtractors
 * @since 4.0
 */
@FunctionalInterface
public interface BodyExtractor<T, M extends ReactiveHttpInputMessage> {

  /**
   * Extract from the given input message.
   *
   * @param inputMessage the request to extract from
   * @param context the configuration to use
   * @return the extracted data
   */
  T extract(M inputMessage, Context context);

  /**
   * Defines the context used during the extraction.
   */
  interface Context {

    /**
     * Return the {@link HttpMessageReader HttpMessageReaders} to be used for body extraction.
     *
     * @return the stream of message readers
     */
    List<HttpMessageReader<?>> messageReaders();

    /**
     * Optionally return the {@link ServerHttpResponse}, if present.
     */
    Optional<ServerHttpResponse> serverResponse();

    /**
     * Return the map of hints to use to customize body extraction.
     */
    Map<String, Object> hints();
  }

}
