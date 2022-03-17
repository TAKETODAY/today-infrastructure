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

package cn.taketoday.test.web.reactive.server;

import cn.taketoday.lang.Nullable;

/**
 * {@code ExchangeResult} sub-class that exposes the response body fully
 * extracted to a representation of type {@code <T>}.
 *
 * @param <T> the response body type
 * @author Rossen Stoyanchev
 * @see FluxExchangeResult
 * @since 4.0
 */
public class EntityExchangeResult<T> extends ExchangeResult {

  @Nullable
  private final T body;

  EntityExchangeResult(ExchangeResult result, @Nullable T body) {
    super(result);
    this.body = body;
  }

  /**
   * Return the entity extracted from the response body.
   */
  @Nullable
  public T getResponseBody() {
    return this.body;
  }

}
