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
package cn.taketoday.core.bytecode.transform;

/**
 * @author TODAY <br>
 * 2019-09-01 21:47
 */
public class ClassFilterTransformer extends AbstractClassFilterTransformer {

  private final ClassFilter filter;

  public ClassFilterTransformer(ClassFilter filter, ClassTransformer pass) {
    super(pass);
    this.filter = filter;
  }

  protected boolean accept(int version, int access, String name, String signature, String superName, String[] interfaces) {
    return filter.accept(name.replace('/', '.'));
  }
}
