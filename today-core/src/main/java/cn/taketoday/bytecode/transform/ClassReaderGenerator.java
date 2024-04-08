/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.bytecode.transform;

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class ClassReaderGenerator implements ClassGenerator {

  private final ClassReader r;

  private final Attribute[] attrs;

  private final int flags;

  public ClassReaderGenerator(ClassReader r, @Nullable Attribute[] attrs, int flags) {
    this.r = r;
    this.attrs = (attrs != null) ? attrs : new Attribute[0];
    this.flags = flags;
  }

  @Override
  public void generateClass(ClassVisitor v) {
    r.accept(v, attrs, flags);
  }

}
