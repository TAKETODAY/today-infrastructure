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
package cn.taketoday.core.bytecode.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.core.RejectModifierPredicate;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinEverythingEmitter.java,v 1.3 2004/06/24 21:15:19
 * herbyderby Exp $
 */
class MixinEverythingEmitter extends MixinEmitter {

  public MixinEverythingEmitter(ClassVisitor v, String className, Class<?>[] classes) {
    super(v, className, classes, null);
  }

  @Override
  protected Class<?>[] getInterfaces(Class<?>[] classes) {
    HashSet<Class<?>> list = new HashSet<>();
    for (Class<?> class1 : classes) {
      final Set<Class<?>> allInterfacesForClass = ClassUtils.getAllInterfacesForClassAsSet(class1);
      CollectionUtils.addAll(list, allInterfacesForClass);
    }
    return ClassUtils.toClassArray(list);
  }

  @Override
  protected Method[] getMethods(Class<?> type) {
    ArrayList<Method> methods = new ArrayList<>();

    Collections.addAll(methods, type.getMethods());

    CollectionUtils.filter(methods, new RejectModifierPredicate(Modifier.FINAL | Modifier.STATIC));
    return ReflectionUtils.toMethodArray(methods);
  }
}
