/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

/**
 * @author TODAY <br>
 * 2019-12-28 13:47
 */
public abstract class ResultHandlers {

  private static final LinkedList<ResultHandler> resultHandlers = new LinkedList<>();

  public static void addHandler(ResultHandler... handlers) {
    Assert.notNull(handlers, "handler must not be null");
    Collections.addAll(resultHandlers, handlers);
    OrderUtils.reversedSort(resultHandlers);
  }

  public static void addHandler(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    resultHandlers.addAll(handlers);
    OrderUtils.reversedSort(resultHandlers);
  }

  public static void setHandler(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    resultHandlers.clear();
    resultHandlers.addAll(handlers);
    OrderUtils.reversedSort(resultHandlers);
  }

  public static List<ResultHandler> getHandlers() {
    return resultHandlers;
  }

  public static RuntimeResultHandler[] getRuntimeHandlers() {
    return resultHandlers
            .stream()
            .filter(res -> res instanceof RuntimeResultHandler)
            .toArray(RuntimeResultHandler[]::new);
  }

  public static ResultHandler getHandler(final Object handler) {
    Assert.notNull(handler, "handler must not be null");
    for (final ResultHandler resolver : getHandlers()) {
      if (resolver.supportsHandler(handler)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * Get correspond view resolver, If there isn't a suitable resolver will be
   * throw {@link ConfigurationException}
   *
   * @return A suitable {@link ResultHandler}
   */
  public static ResultHandler obtainHandler(final Object handler) {
    return nonNull(getHandler(handler),
                   () -> "There isn't have a result resolver to resolve : [" + handler + "]");
  }

}
