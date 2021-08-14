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

import java.io.IOException;

import cn.taketoday.core.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.handler.ObjectNotationProcessor;

/**
 * ObjectNotationProcessor MessageConverter
 *
 * @author TODAY 2021/5/17 16:40
 * @since 3.0.1
 */
public final class ObjectNotationProcessorMessageConverter extends MessageConverter {
  private final ObjectNotationProcessor notationProcessor;

  public ObjectNotationProcessorMessageConverter(ObjectNotationProcessor notationProcessor) {
    Assert.notNull(notationProcessor, "ObjectNotationProcessor must not be null");
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
