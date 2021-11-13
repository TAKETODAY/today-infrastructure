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
package cn.taketoday.core.bytecode.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.Local;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.Block;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ReflectionUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
class BulkBeanEmitter extends ClassEmitter {

  private static final MethodSignature GET_PROPERTY_VALUES = MethodSignature.from("void getPropertyValues(Object, Object[])");
  private static final MethodSignature SET_PROPERTY_VALUES = MethodSignature.from("void setPropertyValues(Object, Object[])");
  private static final MethodSignature CSTRUCT_EXCEPTION = MethodSignature.forConstructor("Throwable, int");

  private static final Type BULK_BEAN = Type.fromClass(BulkBean.class);
  private static final Type BULK_BEAN_EXCEPTION = Type.fromClass(BulkBeanException.class);

  public BulkBeanEmitter(ClassVisitor v,
                         String className,
                         Class target,
                         String[] getterNames,
                         String[] setterNames,
                         Class[] types
  ) {
    super(v);

    Method[] getters = new Method[getterNames.length];
    Method[] setters = new Method[setterNames.length];
    validate(target, getterNames, setterNames, types, getters, setters);

    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, BULK_BEAN, null, Constant.SOURCE_FILE);
    EmitUtils.nullConstructor(this);
    generateGet(target, getters);
    generateSet(target, setters);
    endClass();
  }

  private void generateGet(final Class target, final Method[] getters) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, GET_PROPERTY_VALUES);
    if (getters.length > 0) {
      e.loadArg(0);
      e.checkCast(Type.fromClass(target));
      Local bean = e.newLocal();
      e.storeLocal(bean);
      for (int i = 0; i < getters.length; i++) {
        if (getters[i] != null) {
          MethodInfo getter = MethodInfo.from(getters[i]);
          e.loadArg(1);
          e.push(i);
          e.loadLocal(bean);
          e.invoke(getter);
          e.box(getter.getSignature().getReturnType());
          e.aastore();
        }
      }
    }
    e.returnValue();
    e.end_method();
  }

  private void generateSet(final Class target, final Method[] setters) {
    // setPropertyValues
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, SET_PROPERTY_VALUES);
    if (setters.length > 0) {
      Local index = e.newLocal(Type.INT_TYPE);
      e.push(0);
      e.storeLocal(index);
      e.loadArg(0);
      e.checkCast(Type.fromClass(target));
      e.loadArg(1);
      Block handler = e.begin_block();
      int lastIndex = 0;
      for (int i = 0; i < setters.length; i++) {
        if (setters[i] != null) {
          MethodInfo setter = MethodInfo.from(setters[i]);
          int diff = i - lastIndex;
          if (diff > 0) {
            e.iinc(index, diff);
            lastIndex = i;
          }
          e.dup2();
          e.aaload(i);
          e.unbox(setter.getSignature().getArgumentTypes()[0]);
          e.invoke(setter);

          // fix by wangzx for setters which has returns, such as chained setter
          switch (setter.getSignature().getReturnType().getSort()) {
            case Type.VOID:
              break;
            case Type.LONG:
            case Type.DOUBLE:
              e.pop2();
              break;
            default:
              e.pop();
              break;
          }
        }
      }
      handler.end();
      e.returnValue();
      e.catchException(handler, Type.TYPE_THROWABLE);
      e.newInstance(BULK_BEAN_EXCEPTION);
      e.dupX1();
      e.swap();
      e.loadLocal(index);
      e.invokeConstructor(BULK_BEAN_EXCEPTION, CSTRUCT_EXCEPTION);
      e.throwException();
    }
    else {
      e.returnValue();
    }
    e.end_method();
  }

  private static void validate(Class target,
                               String[] getters,
                               String[] setters,
                               Class[] types,
                               Method[] getters_out,
                               Method[] setters_out
  ) {
    int i = -1;
    if (setters.length != types.length || getters.length != types.length) {
      throw new BulkBeanException("accessor array length must be equal type array length", i);
    }
    for (i = 0; i < types.length; i++) {
      if (getters[i] != null) {
        Method method = getMethod(target, getters[i], null, i);
        if (method.getReturnType() != types[i]) {
          throw new BulkBeanException("Specified type " + types[i] + " does not match declared type " + method
                  .getReturnType(), i);
        }
        if (Modifier.isPrivate(method.getModifiers())) {
          throw new BulkBeanException("Property is private", i);
        }
        getters_out[i] = method;
      }
      if (setters[i] != null) {
        Method method = getMethod(target, setters[i], new Class[] { types[i] }, i);
        if (Modifier.isPrivate(method.getModifiers())) {
          throw new BulkBeanException("Property is private", i);
        }
        setters_out[i] = method;
      }
    }
  }

  private static Method getMethod(
          final Class<?> type, final String methodName, final Class<?>[] parameterTypes, int index
  ) {
    final Method method = ReflectionUtils.findMethod(type, methodName, parameterTypes);
    if (method == null) {
      throw new BulkBeanException("Cannot find specified property", index);
    }
    return method;
  }
}
