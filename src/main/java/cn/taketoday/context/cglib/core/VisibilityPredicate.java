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
package cn.taketoday.context.cglib.core;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import cn.taketoday.context.asm.Type;

/**
 * @author TODAY <br>
 * 2019-09-03 15:41
 */
public class VisibilityPredicate implements Predicate<Executable> {

  private String pkg;
  private boolean protectedOk;
  private boolean samePackageOk;

  public VisibilityPredicate(Class<?> source, boolean protectedOk) {
    this.protectedOk = protectedOk;
    // same package is not ok for the bootstrap loaded classes. In all other cases
    // we are
    // generating classes in the same classloader
    this.samePackageOk = source.getClassLoader() != null;
    pkg = TypeUtils.getPackageName(Type.getType(source));
  }

  public boolean test(Executable member) {
    int mod = member.getModifiers();
    if (Modifier.isPrivate(mod)) {
      return false;
    }
    if (Modifier.isPublic(mod) || (Modifier.isProtected(mod) && protectedOk)) {
      // protected is fine if 'protectedOk' is true (for subclasses)
      return true;
    }
    // protected/package private if the member is in the same package as the source
    // class
    // and we are generating into the same classloader.
    return samePackageOk && pkg.equals(TypeUtils.getPackageName(Type.getType(member.getDeclaringClass())));
  }
}
