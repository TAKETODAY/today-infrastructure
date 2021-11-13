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
package cn.taketoday.core.bytecode.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.core.bytecode.core.Customizer;
import cn.taketoday.core.bytecode.core.KeyFactoryCustomizer;

/**
 * @author TODAY <br>
 * 2019-10-17 20:45
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CustomizerRegistry {

  private final Class<?>[] customizerTypes;
  private final HashMap<Class<?>, List<KeyFactoryCustomizer>> customizers = new HashMap<>();

  public CustomizerRegistry(Class... customizerTypes) {
    this.customizerTypes = customizerTypes;
  }

  public void add(KeyFactoryCustomizer customizer) {
    Class<? extends KeyFactoryCustomizer> klass = customizer.getClass();
    for (Class<?> type : customizerTypes) {
      if (type.isAssignableFrom(klass)) {
        List<KeyFactoryCustomizer> list = customizers.get(type);
        if (list == null) {
          customizers.put(type, list = new ArrayList<>());
        }
        list.add(customizer);
      }
    }
  }

  public <T> List<T> get(Class<T> klass) {
    List<KeyFactoryCustomizer> list = customizers.get(klass);
    return list == null ? Collections.emptyList() : (List<T>) list;
  }

  public static CustomizerRegistry singleton(Customizer customizer) {
    CustomizerRegistry registry = new CustomizerRegistry(Customizer.class);
    registry.add(customizer);
    return registry;
  }
}
