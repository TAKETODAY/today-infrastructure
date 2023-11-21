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

package cn.taketoday.framework.test.context.runner;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;

/**
 * Callback interface used to process an {@link ApplicationContext} with the ability to
 * throw a (checked) exception.
 *
 * @param <C> the application context type
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see AbstractApplicationContextRunner
 * @since 4.0
 */
@FunctionalInterface
public interface ContextConsumer<C extends ApplicationContext> {

  /**
   * Performs this operation on the supplied {@code context}.
   *
   * @param context the application context to consume
   * @throws Throwable any exception that might occur in assertions
   */
  void accept(C context) throws Throwable;

  /**
   * Returns a composed {@code ContextConsumer} that performs, in sequence, this
   * operation followed by the {@code after} operation.
   *
   * @param after the operation to perform after this operation
   * @return a composed {@code ContextConsumer} that performs in sequence this operation
   * followed by the {@code after} operation
   */
  default ContextConsumer<C> andThen(ContextConsumer<? super C> after) {
    Assert.notNull(after, "After is required");
    return (context) -> {
      accept(context);
      after.accept(context);
    };
  }

}
