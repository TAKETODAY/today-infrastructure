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
package cn.taketoday.bytecode.core;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY <br>
 * 2019-09-03 15:41
 */
public class VisibilityPredicate implements Predicate<Executable> {

  private final String pkg;
  private final boolean protectedOk;
  private final boolean samePackageOk;

  public VisibilityPredicate(Class<?> source, boolean protectedOk) {
    this.protectedOk = protectedOk;
    // same package is not ok for the bootstrap loaded classes. In all other cases
    // we are
    // generating classes in the same classloader
    this.samePackageOk = source.getClassLoader() != null;
    this.pkg = ClassUtils.getPackageName(source);
  }

  @Override
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
    return samePackageOk && pkg.equals(ClassUtils.getPackageName(member.getDeclaringClass()));
  }
}
