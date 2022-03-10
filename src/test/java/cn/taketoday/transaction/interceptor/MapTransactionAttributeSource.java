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

package cn.taketoday.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Inherits fallback behavior from AbstractFallbackTransactionAttributeSource.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class MapTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource {

  private final Map<Object, TransactionAttribute> attributeMap = new HashMap<>();

  public void register(Class<?> clazz, TransactionAttribute txAttr) {
    this.attributeMap.put(clazz, txAttr);
  }

  public void register(Method method, TransactionAttribute txAttr) {
    this.attributeMap.put(method, txAttr);
  }

  @Override
  protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
    return this.attributeMap.get(clazz);
  }

  @Override
  protected TransactionAttribute findTransactionAttribute(Method method) {
    return this.attributeMap.get(method);
  }

}
