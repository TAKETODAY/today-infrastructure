/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
