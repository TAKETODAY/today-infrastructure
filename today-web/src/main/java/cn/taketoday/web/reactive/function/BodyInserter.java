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

import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * A combination of functions that can populate a {@link ReactiveHttpOutputMessage} body.
 *
 * @param <T> the type of data to insert
 * @param <M> the type of {@link ReactiveHttpOutputMessage} this inserter can be applied to
 * @author Arjen Poutsma
 * @see BodyInserters
 * @since 4.0
 */
@FunctionalInterface
public interface BodyInserter<T, M extends ReactiveHttpOutputMessage> {

  /**
   * Insert into the given output message.
   *
   * @param outputMessage the response to insert into
   * @param context the context to use
   * @return a {@code Mono} that indicates completion or error
   */
  Mono<Void> insert(M outputMessage, Context context);

  /**
   * Defines the context used during the insertion.
   */
  interface Context {

    /**
     * Return the {@link HttpMessageWriter HttpMessageWriters} to be used for response body conversion.
     *
     * @return the stream of message writers
     */
    List<HttpMessageWriter<?>> messageWriters();

    /**
     * Optionally return the {@link ServerHttpRequest}, if present.
     */
    Optional<ServerHttpRequest> serverRequest();

    /**
     * Return the map of hints to use for response body conversion.
     */
    Map<String, Object> hints();
  }

}
