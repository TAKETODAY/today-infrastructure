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

/**
 * @author TODAY 2021/3/8 21:22
 * @since 3.0
 */
abstract class AbstractMappingFunctionCache extends AbstractCache {

  @Override
  protected <T> Object getInternal(Object key, CacheCallback<T> valueLoader) {
    final class MappingFunction implements UnaryOperator<Object> {
      @Override
      public Object apply(Object k) {
        return lookupValue(k, valueLoader);
      }
    }
    return getInternal(key, new MappingFunction());
  }

  protected abstract Object getInternal(Object key, UnaryOperator<Object> mappingFunction);

}
