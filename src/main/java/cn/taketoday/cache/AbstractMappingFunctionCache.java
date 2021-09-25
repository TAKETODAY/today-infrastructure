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

package cn.taketoday.cache;

import java.util.function.UnaryOperator;

import cn.taketoday.util.ExceptionUtils;

/**
 * @author TODAY 2021/3/8 21:22
 * @since 3.0
 */
public abstract class AbstractMappingFunctionCache extends Cache {

  @Override
  protected final <T> Object computeIfAbsent(Object key, CacheCallback<T> valueLoader) {
    final class MappingFunction implements UnaryOperator<Object> {
      @Override
      public Object apply(Object k) {
        try {
          return compute(k, valueLoader);
        }
        catch (Throwable e) {
          throw ExceptionUtils.sneakyThrow(e);
        }
      }
    }
    return computeIfAbsent(key, new MappingFunction());
  }

  protected abstract Object computeIfAbsent(Object key, UnaryOperator<Object> mappingFunction);

}
