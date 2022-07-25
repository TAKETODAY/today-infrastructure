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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.CglibReflectUtils;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.KeyFactory;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.lang.Constant.SOURCE_FILE;

/**
 * @author Chris Nokleberg
 * @version $Id: ConstructorDelegate.java,v 1.20 2006/03/05 02:43:19 herbyderby
 * Exp $
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
abstract public class ConstructorDelegate {

  private static final ConstructorKey KEY_FACTORY = KeyFactory.create(ConstructorKey.class, KeyFactory.CLASS_BY_NAME);

  interface ConstructorKey {
    Object newInstance(String declaring, String iface);
  }

  protected ConstructorDelegate() { }

  public static <T> T create(Class targetClass, Class<T> iface) {
    Generator gen = new Generator();
    gen.setTargetClass(targetClass);
    gen.setInterface(iface);
    return (T) gen.create();
  }

  public static class Generator extends AbstractClassGenerator {
    private static final Type CONSTRUCTOR_DELEGATE = Type.fromClass(ConstructorDelegate.class);

    private Class iface;
    private Class targetClass;

    public Generator() {
      super(ConstructorDelegate.class);
    }

    public void setInterface(Class iface) {
      this.iface = iface;
    }

    public void setTargetClass(Class targetClass) {
      this.targetClass = targetClass;
      setNeighbor(targetClass);
    }

    public ConstructorDelegate create() {
      setNamePrefix(targetClass.getName());
      Object key = KEY_FACTORY.newInstance(iface.getName(), targetClass.getName());
      return (ConstructorDelegate) super.create(key);
    }

    protected ClassLoader getDefaultClassLoader() {
      return targetClass.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(targetClass);
    }

    public void generateClass(ClassVisitor v) {
      setNamePrefix(targetClass.getName());

      final Method newInstance = CglibReflectUtils.findNewInstance(iface);
      if (!newInstance.getReturnType().isAssignableFrom(targetClass)) {
        throw new IllegalArgumentException("incompatible return type");
      }
      final Constructor constructor;
      try {
        constructor = targetClass.getDeclaredConstructor(newInstance.getParameterTypes());
      }
      catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("interface does not match any known constructor");
      }

      ClassEmitter ce = new ClassEmitter(v);
      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), CONSTRUCTOR_DELEGATE, Type.array(Type.fromClass(iface)), SOURCE_FILE);

      Type declaring = Type.fromClass(constructor.getDeclaringClass());
      EmitUtils.nullConstructor(ce);
      CodeEmitter e = ce.beginMethod(
              Opcodes.ACC_PUBLIC, MethodSignature.from(newInstance), Type.getExceptionTypes(newInstance));
      e.newInstance(declaring);
      e.dup();
      e.loadArgs();
      e.invokeConstructor(declaring, MethodSignature.from(constructor));
      e.returnValue();
      e.end_method();
      ce.endClass();
    }

    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type);
    }

    protected Object nextInstance(Object instance) {
      return instance;
    }
  }
}
