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

import java.io.IOException;
import java.io.OutputStream;

/**
 * simple HttpOutputMessage
 *
 * @author TODAY 2021/11/6 13:10
 * @since 4.0
 */
@Deprecated
public class SimpleHttpOutputMessage implements HttpOutputMessage {

  private final HttpHeaders headers;
  private final OutputStream body;

  public SimpleHttpOutputMessage(HttpHeaders headers, OutputStream body) {
    this.headers = headers;
    this.body = body;
  }

  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  @Override
  public OutputStream getBody() throws IOException {
    return body;
  }

}
