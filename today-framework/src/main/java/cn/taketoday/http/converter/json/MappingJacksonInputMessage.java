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

package cn.taketoday.http.converter.json;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.lang.Nullable;

/**
 * {@link HttpInputMessage} that can eventually stores a Jackson view that will be used
 * to deserialize the message.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJacksonInputMessage implements HttpInputMessage {

  private final InputStream body;

  private final HttpHeaders headers;

  @Nullable
  private Class<?> deserializationView;

  public MappingJacksonInputMessage(InputStream body, HttpHeaders headers) {
    this.body = body;
    this.headers = headers;
  }

  public MappingJacksonInputMessage(InputStream body, HttpHeaders headers, Class<?> deserializationView) {
    this(body, headers);
    this.deserializationView = deserializationView;
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.body;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  public void setDeserializationView(@Nullable Class<?> deserializationView) {
    this.deserializationView = deserializationView;
  }

  @Nullable
  public Class<?> getDeserializationView() {
    return this.deserializationView;
  }

}
