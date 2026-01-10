/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;

/**
 * @author TODAY 2021/9/25 15:58
 */
public class SimpleMethodInfo extends MethodInfo {

  private final int access;

  private final ClassInfo classInfo;

  private final MethodSignature sig;

  private final Type @Nullable [] exceptionTypes;

  public SimpleMethodInfo(ClassInfo classInfo, int access, MethodSignature sig, Type @Nullable [] exceptionTypes) {
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
  public Type @Nullable [] getExceptionTypes() {
    return exceptionTypes;
  }

}
