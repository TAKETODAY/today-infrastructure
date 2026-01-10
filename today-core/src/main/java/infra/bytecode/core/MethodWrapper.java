/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author TODAY
 * @since 2019-09-03 12:58
 */
public final class MethodWrapper {

  private MethodWrapper() {

  }

  public static Object create(Method method) {
    return new MethodWrapperKey(method.getName(),
            Arrays.asList(CglibReflectUtils.getNames(method.getParameterTypes())),
            method.getReturnType().getName());
  }

  public static HashSet<Object> createSet(Collection<Method> methods) {
    HashSet<Object> ret = new HashSet<>();

    for (Method method : methods) {
      ret.add(create(method));
    }
    return ret;
  }

  private record MethodWrapperKey(String name, List<String> parameterTypes, String returnType) {
  }

}
