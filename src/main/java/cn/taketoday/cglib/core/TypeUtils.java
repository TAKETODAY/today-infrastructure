/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib.core;

import cn.taketoday.asm.Type;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY <br>
 * 2019-09-03 14:19
 */
public abstract class TypeUtils {

  public static Type[] add(Type[] types, Type extra) {
    return add(types, extra, false);
  }

  public static Type[] add(Type[] types, Type extra, boolean justAdd) {

    if (ObjectUtils.isEmpty(types)) {
      return new Type[] { extra };
    }

    if (!justAdd && CollectionUtils.contains(types, extra)) {
      return types;
    }
    final Type[] copy = new Type[types.length + 1];
    System.arraycopy(types, 0, copy, 0, types.length);
    copy[types.length] = extra;
    return copy;
  }

  public static Type[] add(Type[] t1, Type... t2) {
    if (ObjectUtils.isEmpty(t2)) {
      return t1;
    }
    // TODO: set semantics?
    Type[] all = new Type[t1.length + t2.length];
    System.arraycopy(t1, 0, all, 0, t1.length);
    System.arraycopy(t2, 0, all, t1.length, t2.length);
    return all;
  }

}
