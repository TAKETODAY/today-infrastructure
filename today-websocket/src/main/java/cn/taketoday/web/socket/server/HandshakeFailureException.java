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

package cn.taketoday.web.socket.server;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Thrown when handshake processing failed to complete due to an internal, unrecoverable
 * error. This implies a server error (HTTP status code 500) as opposed to a failure in
 * the handshake negotiation.
 *
 * <p>By contrast, when handshake negotiation fails, the response status code will be 200
 * and the response headers and body will have been updated to reflect the cause for the
 * failure. A {@link HandshakeHandler} implementation will have protected methods to
 * customize updates to the response in those cases.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HandshakeFailureException extends NestedRuntimeException {

  public HandshakeFailureException(String message) {
    super(message);
  }

  public HandshakeFailureException(String message, Throwable cause) {
    super(message, cause);
  }

}
