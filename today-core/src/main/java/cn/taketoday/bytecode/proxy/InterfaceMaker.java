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
package cn.taketoday.bytecode.proxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Constant;

/**
 * Generates new interfaces at runtime. By passing a generated interface to the
 * Enhancer's list of interfaces to implement, you can make your enhanced
 * classes handle an arbitrary set of method signatures.
 *
 * @author Chris Nokleberg
 * @author TODAY
 */
public class InterfaceMaker extends AbstractClassGenerator<Object> {

  private final HashMap<MethodSignature, Type[]> signatures = new HashMap<>();

  /**
   * Create a new <code>InterfaceMaker</code>. A new <code>InterfaceMaker</code>
   * object should be used for each generated interface, and should not be shared
   * across threads.
   */
  public InterfaceMaker() {
    super(InterfaceMaker.class);
  }

  /**
   * Add a method signature to the interface.
   *
   * @param sig the method signature to add to the interface
   * @param exceptions an array of exception types to declare for the method
   */
  public void add(MethodSignature sig, Type[] exceptions) {
    signatures.put(sig, exceptions);
  }

  /**
   * Add a method signature to the interface. The method modifiers are ignored,
   * since interface methods are by definition abstract and public.
   *
   * @param method the method to add to the interface
   */
  public void add(Method method) {
    add(MethodSignature.from(method), Type.getExceptionTypes(method));
  }

  /**
   * Add all the public methods in the specified class. Methods from superclasses
   * are included, except for methods declared in the base Object class (e.g.
   * <code>getClass</code>, <code>equals</code>, <code>hashCode</code>).
   *
   * @param clazz the class containing the methods to add to the interface
   */
  public void add(Class<?> clazz) {

    for (final Method m : clazz.getMethods()) {
      if (!m.getDeclaringClass().getName().equals("java.lang.Object")) {
        add(m);
      }
    }
  }

  /**
   * Create an interface using the current set of method signatures.
   */
  public Class<?> create() {
    setUseCache(false);
    return (Class<?>) super.create(this);
  }

  @Override
  protected ClassLoader getDefaultClassLoader() {
    return null;
  }

  @Override
  protected Object firstInstance(Class<Object> type) throws Exception {
    return type;
  }

  @Override
  protected Object nextInstance(Object instance) {
    throw new IllegalStateException("InterfaceMaker does not cache");
  }

  @Override
  public void generateClass(ClassVisitor v) throws Exception {
    ClassEmitter ce = new ClassEmitter(v);
    ce.beginClass(//
                  Opcodes.JAVA_VERSION, //
                  Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, //
                  getClassName(), //
                  null, //
                  null, //
                  Constant.SOURCE_FILE//
    );

    final int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;
    for (final Map.Entry<MethodSignature, Type[]> entry : signatures.entrySet()) {
      ce.beginMethod(access, entry.getKey(), entry.getValue()).end_method();
    }

    ce.endClass();
  }

}
