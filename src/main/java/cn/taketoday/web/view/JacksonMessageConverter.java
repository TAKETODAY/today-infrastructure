/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.JacksonObjectNotationProcessor;
import cn.taketoday.web.handler.MethodParameter;

/**
 * support {@link JsonNode} {@link Collection}, POJO, Array
 *
 * @author TODAY 2021/3/10 11:36
 * @since 3.0
 */
public class JacksonMessageConverter extends MessageConverter {

  final JacksonObjectNotationProcessor notationProcessor;

  public JacksonMessageConverter() {
    this(new JacksonObjectNotationProcessor(new ObjectMapper()));
  }

  public JacksonMessageConverter(ObjectMapper mapper) {
    this(new JacksonObjectNotationProcessor(mapper));
  }

  public JacksonMessageConverter(JacksonObjectNotationProcessor notationProcessor) {
    this.notationProcessor = notationProcessor;
  }

  @Override
  protected void writeInternal(RequestContext context, Object noneNullMessage) throws IOException {
    notationProcessor.write(context.getOutputStream(), noneNullMessage);
  }

  @Override
  public Object read(RequestContext context, MethodParameter parameter) throws IOException {
    return notationProcessor.read(context.getInputStream(), parameter.getGenericDescriptor());
  }

}
