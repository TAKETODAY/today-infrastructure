/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web;

import java.util.Objects;

import cn.taketoday.lang.Assert;

/**
 * Provides a convenient implementation of the RequestContext
 * that can be subclassed by developers wishing to adapt the request to web.
 * This class implements the Wrapper or Decorator pattern.
 * Methods default to calling through to the wrapped request object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 13:53
 */
public class RequestContextDecorator extends DecoratingRequestContext {

  protected final RequestContext delegate;

  public RequestContextDecorator(RequestContext delegate) {
    Assert.notNull(delegate, "RequestContext delegate is required");
    this.delegate = delegate;
  }

  @Override
  public RequestContext getDelegate() {
    return delegate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RequestContextDecorator that))
      return false;
    return Objects.equals(delegate, that.delegate);
  }

}
