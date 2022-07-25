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
package cn.taketoday.bytecode.reflect;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.core.ProcessArrayCallback;
import cn.taketoday.bytecode.commons.Local;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ReflectionUtils;

import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
abstract public class MulticastDelegate implements Cloneable {

  protected Object[] targets = Constant.EMPTY_OBJECTS;

  protected MulticastDelegate() { }

  public List<Object> getTargets() {
    return Arrays.asList(targets);
  }

  abstract public MulticastDelegate add(Object target);

  protected MulticastDelegate addHelper(Object target) {
    MulticastDelegate copy = newInstance();
    final Object[] targets = this.targets;
    copy.targets = new Object[targets.length + 1];
    System.arraycopy(targets, 0, copy.targets, 0, targets.length);
    copy.targets[targets.length] = target;
    return copy;
  }

  public MulticastDelegate remove(Object target) {
    final Object[] targets = this.targets;
    for (int i = targets.length - 1; i >= 0; i--) {
      if (targets[i].equals(target)) {
        MulticastDelegate copy = newInstance();
        copy.targets = new Object[targets.length - 1];
        System.arraycopy(targets, 0, copy.targets, 0, i);
        System.arraycopy(targets, i + 1, copy.targets, i, targets.length - i - 1);
        return copy;
      }
    }
    return this;
  }

  public abstract MulticastDelegate newInstance();

  public static MulticastDelegate create(Class<?> iface) {
    return new Generator().setInterface(iface).create();
  }

  public static class Generator extends AbstractClassGenerator<Object> {

    static final Type MULTICAST_DELEGATE = Type.fromClass(MulticastDelegate.class);
    static final MethodSignature ADD_DELEGATE = new MethodSignature(MULTICAST_DELEGATE, "add", Type.TYPE_OBJECT);
    static final MethodSignature ADD_HELPER = new MethodSignature(MULTICAST_DELEGATE, "addHelper", Type.TYPE_OBJECT);
    static final MethodSignature NEW_INSTANCE = new MethodSignature(MULTICAST_DELEGATE, "newInstance", Type.EMPTY_ARRAY);

    private Class<?> iface;

    public Generator() {
      super(MulticastDelegate.class);
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
      return iface.getClassLoader();
    }

    @Override
    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(iface);
    }

    public Generator setInterface(Class<?> iface) {
      this.iface = iface;
      setNeighbor(iface);
      return this;
    }

    public MulticastDelegate create() {
      setNamePrefix(MulticastDelegate.class.getName());
      return (MulticastDelegate) super.create(iface.getName());
    }

    @Override
    public void generateClass(ClassVisitor cv) {
      final MethodInfo method = MethodInfo.from(ReflectionUtils.findFunctionalInterfaceMethod(iface));

      ClassEmitter ce = new ClassEmitter(cv);

      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), MULTICAST_DELEGATE,
              Type.array(Type.fromClass(iface)), Constant.SOURCE_FILE);

      EmitUtils.nullConstructor(ce);

      // generate proxied method
      emitProxy(ce, method);

      // newInstance
      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, NEW_INSTANCE);
      e.new_instance_this();
      e.dup();
      e.invoke_constructor_this();
      e.returnValue();
      e.end_method();

      // add
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, ADD_DELEGATE);
      e.loadThis();
      e.loadArg(0);
      e.checkCast(Type.fromClass(iface));
      e.invoke_virtual_this(ADD_HELPER);
      e.returnValue();
      e.end_method();

      ce.endClass();
    }

    private void emitProxy(ClassEmitter ce, final MethodInfo method) {
      int modifiers = Opcodes.ACC_PUBLIC;
      if ((method.getModifiers() & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS) {
        modifiers |= Opcodes.ACC_VARARGS;
      }
      final CodeEmitter e = EmitUtils.beginMethod(ce, method, modifiers);
      Type returnType = method.getSignature().getReturnType();
      final boolean returns = returnType != Type.VOID_TYPE;
      Local result = null;
      if (returns) {
        result = e.newLocal(returnType);
        e.zero_or_null(returnType);
        e.storeLocal(result);
      }
      e.loadThis();
      e.super_getfield("targets", Type.TYPE_OBJECT_ARRAY);
      final Local result2 = result;
      EmitUtils.processArray(e, Type.TYPE_OBJECT_ARRAY, new ProcessArrayCallback() {
        public void processElement(Type type) {
          e.checkCast(Type.fromClass(iface));
          e.loadArgs();
          e.invoke(method);
          if (returns) {
            e.storeLocal(result2);
          }
        }
      });
      if (returns) {
        e.loadLocal(result);
      }
      e.returnValue();
      e.end_method();
    }

    @Override
    protected Object firstInstance(Class<Object> type) {
      // make a new instance in case first object is used with a long list of targets
      return ((MulticastDelegate) ReflectionUtils.newInstance(type)).newInstance();
    }

    @Override
    protected Object nextInstance(Object instance) {
      return ((MulticastDelegate) instance).newInstance();
    }
  }
}
