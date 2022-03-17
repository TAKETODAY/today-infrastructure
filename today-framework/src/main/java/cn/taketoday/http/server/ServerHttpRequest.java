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

package cn.taketoday.http.server;

import java.net.InetSocketAddress;
import java.security.Principal;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.lang.Nullable;

/**
 * Represents a server-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

  /**
   * Return a {@link Principal} instance containing the name of the
   * authenticated user.
   * <p>If the user has not been authenticated, the method returns <code>null</code>.
   */
  @Nullable
  Principal getPrincipal();

  /**
   * Return the address on which the request was received.
   */
  InetSocketAddress getLocalAddress();

  /**
   * Return the address of the remote client.
   */
  InetSocketAddress getRemoteAddress();

  /**
   * Return a control that allows putting the request in asynchronous mode so the
   * response remains open until closed explicitly from the current or another thread.
   */
  ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response);

}
