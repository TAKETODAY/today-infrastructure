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
package cn.taketoday.bytecode.util;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.commons.Local;
import cn.taketoday.bytecode.commons.MethodSignature;

import static cn.taketoday.lang.Constant.SOURCE_FILE;

class ParallelSorterEmitter extends ClassEmitter {

  private static final Type PARALLEL_SORTER = Type.fromClass(ParallelSorter.class);
  private static final MethodSignature CSTRUCT_OBJECT_ARRAY = MethodSignature.forConstructor("Object[]");
  private static final MethodSignature NEW_INSTANCE = new MethodSignature(PARALLEL_SORTER, "newInstance", Type.TYPE_OBJECT_ARRAY);
  private static final MethodSignature SWAP = MethodSignature.from("void swap(int, int)");

  public ParallelSorterEmitter(ClassVisitor v, String className, Object[] arrays) {
    super(v);
    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, PARALLEL_SORTER, null, SOURCE_FILE);
    EmitUtils.nullConstructor(this);
    EmitUtils.factoryMethod(this, NEW_INSTANCE);
    generateConstructor(arrays);
    generateSwap(arrays);
    endClass();
  }

  private String getFieldName(int index) {
    return "FIELD_" + index;
  }

  private void generateConstructor(Object[] arrays) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, CSTRUCT_OBJECT_ARRAY);
    e.loadThis();
    e.super_invoke_constructor();
    e.loadThis();
    e.loadArg(0);
    e.super_putfield("a", Type.TYPE_OBJECT_ARRAY);
    for (int i = 0; i < arrays.length; i++) {
      Type type = Type.fromClass(arrays[i].getClass());
      declare_field(Opcodes.ACC_PRIVATE, getFieldName(i), type, null);
      e.loadThis();
      e.loadArg(0);
      e.push(i);
      e.aaload();
      e.checkCast(type);
      e.putField(getFieldName(i));
    }
    e.returnValue();
    e.end_method();
  }

  private void generateSwap(final Object[] arrays) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, SWAP);
    for (int i = 0; i < arrays.length; i++) {
      Type type = Type.fromClass(arrays[i].getClass());
      Type component = type.getComponentType();
      Local T = e.newLocal(type);

      e.loadThis();
      e.getField(getFieldName(i));
      e.storeLocal(T);

      e.loadLocal(T);
      e.loadArg(0);

      e.loadLocal(T);
      e.loadArg(1);
      e.arrayLoad(component);

      e.loadLocal(T);
      e.loadArg(1);

      e.loadLocal(T);
      e.loadArg(0);
      e.arrayLoad(component);

      e.arrayStore(component);
      e.arrayStore(component);
    }
    e.returnValue();
    e.end_method();
  }
}
