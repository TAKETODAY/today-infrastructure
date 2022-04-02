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
package cn.taketoday.retry.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.util.SerializationUtils;

public class SerializedMapRetryContextCache implements RetryContextCache {

  private static final int DEFAULT_CAPACITY = 4096;

  private final Map<Object, byte[]> map = Collections.synchronizedMap(new HashMap<>());

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public RetryContext get(Object key) {
    byte[] bytes = map.get(key);
    return (RetryContext) SerializationUtils.deserialize(bytes);
  }

  @Override
  public void put(Object key, RetryContext context) {
    if (map.size() >= DEFAULT_CAPACITY) {
      throw new RetryCacheCapacityExceededException("Retry cache capacity "
              + "limit breached. Do you need to re-consider the implementation of the key generator, "
              + "or the equals and hashCode of the items that failed?");
    }
    byte[] serialized = SerializationUtils.serialize(context);
    map.put(key, serialized);
  }

  @Override
  public void remove(Object key) {
    map.remove(key);
  }

}
