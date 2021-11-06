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
package cn.taketoday.http;

import org.reactivestreams.Publisher;

import cn.taketoday.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

/**
 * An "reactive" HTTP input message that exposes the input as {@link Publisher}.
 *
 * <p>Typically implemented by an HTTP request on the server-side or a response
 * on the client-side.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface ReactiveHttpInputMessage extends HttpMessage {

  /**
   * Return the body of the message as a {@link Publisher}.
   *
   * @return the body content publisher
   */
  Flux<DataBuffer> getBody();

}
