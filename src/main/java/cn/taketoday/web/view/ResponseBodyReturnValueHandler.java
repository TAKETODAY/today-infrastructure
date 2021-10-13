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
package cn.taketoday.web.view;

import java.io.IOException;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.MessageBodyConverter;
import cn.taketoday.web.RequestContext;

/**
 * serialize return-value(any Object) to HTTP response-body
 *
 * @author TODAY 2019-07-14 01:19
 */
public class ResponseBodyReturnValueHandler extends OrderedSupport implements ReturnValueHandler {
  private final MessageBodyConverter messageBodyConverter;

  public ResponseBodyReturnValueHandler(MessageBodyConverter messageBodyConverter) {
    Assert.notNull(messageBodyConverter, "MessageBodyConverter must not be null");
    this.messageBodyConverter = messageBodyConverter;
    setOrder(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return true;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return true;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    write(context, returnValue);
  }

  public void write(RequestContext context, Object returnValue) throws IOException {
    messageBodyConverter.write(context, returnValue);
  }

}
