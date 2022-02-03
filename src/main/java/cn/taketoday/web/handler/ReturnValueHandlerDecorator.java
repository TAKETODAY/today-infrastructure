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

package cn.taketoday.web.handler;

import java.io.IOException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Decorator Pattern
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/24 15:00
 */
public class ReturnValueHandlerDecorator implements ReturnValueHandler {
  private final ReturnValueHandler delegate;

  public ReturnValueHandlerDecorator(ReturnValueHandler delegate) {
    Assert.notNull(delegate, "ReturnValueHandler delegate is required");
    this.delegate = delegate;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return delegate.supportsHandler(handler);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return delegate.supportsReturnValue(returnValue);
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws IOException {
    delegate.handleReturnValue(context, handler, returnValue);
  }

  public ReturnValueHandler getDelegate() {
    return delegate;
  }

}
