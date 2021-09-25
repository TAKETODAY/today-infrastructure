/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;

/**
 * @author TODAY 2021/9/25 15:58
 */
public class SimpleMethodInfo extends MethodInfo {

  private final int access;
  private final ClassInfo classInfo;
  private final MethodSignature sig;
  private final Type[] exceptionTypes;

  public SimpleMethodInfo(ClassInfo classInfo, int access, MethodSignature sig, Type[] exceptionTypes) {
    this.sig = sig;
    this.access = access;
    this.classInfo = classInfo;
    this.exceptionTypes = exceptionTypes;
  }

  @Override
  public ClassInfo getClassInfo() {
    return classInfo;
  }

  @Override
  public int getModifiers() {
    return access;
  }

  @Override
  public MethodSignature getSignature() {
    return sig;
  }

  @Override
  public Type[] getExceptionTypes() {
    return exceptionTypes;
  }

}
