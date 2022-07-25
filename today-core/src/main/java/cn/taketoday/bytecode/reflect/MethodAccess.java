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
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * @author TODAY 2018-11-08 15:08
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class MethodAccess {

  private final Class type;

  protected MethodAccess(Class type) {
    this.type = type;
  }

  /**
   * Return the index of the matching method. The index may be used later to
   * invoke the method with less overhead. If more than one method matches (i.e.
   * they differ by return type only), one is chosen arbitrarily.
   *
   * @param name the method name
   * @param parameterTypes the parameter array
   * @return the index, or <code>-1</code> if none is found.
   * @see #invoke(int, Object, Object[])
   */
  public abstract int getIndex(String name, Class[] parameterTypes);

  /**
   * Return the index of the matching constructor. The index may be used later to
   * create a new instance with less overhead.
   *
   * @param parameterTypes the parameter array
   * @return the constructor index, or <code>-1</code> if none is found.
   * @see #newInstance(int, Object[])
   */
  public abstract int getIndex(Class[] parameterTypes);

  /**
   * Invoke the method with the specified index.
   *
   * @param index the method index
   * @param obj the object the underlying method is invoked from
   * @param args the arguments used for the method call
   * @throws java.lang.reflect.InvocationTargetException if the underlying method throws an exception
   * @see #getIndex(String, Class[])
   */
  public abstract Object invoke(int index, Object obj, Object[] args) throws InvocationTargetException;

  /**
   * Create a new instance using the specified constructor index and arguments.
   *
   * @param index the constructor index
   * @param args the arguments passed to the constructor
   * @throws java.lang.reflect.InvocationTargetException if the constructor throws an exception
   * @see #getIndex(Class[])
   */
  public abstract Object newInstance(int index, Object[] args) throws InvocationTargetException;

  public abstract int getIndex(MethodSignature sig);

  public int getIndex(Method method) {
    return getIndex(MethodSignature.from(method));
  }

  /**
   * Returns the maximum method index for this class.
   */
  public abstract int getMaxIndex();

  public static class Generator extends AbstractClassGenerator {

    private final Class<?> type;

    public Generator(Class<?> type) {
      super(MethodAccess.class);
      this.type = type;
    }

    public MethodAccess create() {
      setNamePrefix(type.getName());
      return (MethodAccess) super.create(type.getName());
    }

    protected ClassLoader getDefaultClassLoader() {
      return type.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(type);
    }

    public void generateClass(ClassVisitor v) throws Exception {
      new MethodAccessEmitter(v, getClassName(), type);
    }

    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type, new Class[] { Class.class }, new Object[] { this.type });
    }

    protected Object nextInstance(Object instance) {
      return instance;
    }
  }

  public Object invoke(String name, Class[] parameterTypes, Object obj, Object[] args) throws InvocationTargetException {
    return invoke(getIndex(name, parameterTypes), obj, args);
  }

  public Object newInstance() throws InvocationTargetException {
    return newInstance(getIndex(Constant.EMPTY_CLASSES), null);
  }

  public Object newInstance(Class[] parameterTypes, Object[] args) throws InvocationTargetException {
    return newInstance(getIndex(parameterTypes), args);
  }

  public FastMethodAccessor getMethod(Method method) {
    return new FastMethodAccessor(this, method);
  }

  public FastConstructorAccessor getConstructor(Constructor constructor) {
    return new FastConstructorAccessor(this, constructor);
  }

  public FastMethodAccessor getMethod(String name, Class[] parameterTypes) {
    try {
      return getMethod(type.getMethod(name, parameterTypes));
    }
    catch (NoSuchMethodException e) {
      throw new NoSuchMethodError(e.getMessage());
    }
  }

  public FastConstructorAccessor getConstructor(Class[] parameterTypes) {
    try {
      return getConstructor(type.getConstructor(parameterTypes));
    }
    catch (NoSuchMethodException e) {
      throw new NoSuchMethodError(e.getMessage());
    }
  }

  public String getName() {
    return type.getName();
  }

  /**
   * @see Method#getDeclaringClass()
   */
  public Class getDeclaringClass() {
    return type;
  }

  @Override
  public String toString() {
    return type.toString();
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o == this || (o instanceof MethodAccess && type.equals(((MethodAccess) o).type));
  }

  protected static String getSignatureWithoutReturnType(String name, Class[] parameterTypes) {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append('(');
    for (Class parameterType : parameterTypes) {
      sb.append(Type.getDescriptor(parameterType));
    }
    sb.append(')');
    return sb.toString();
  }

  // static factory

  public static MethodAccess from(Class type) {
    return from(type.getClassLoader(), type);
  }

  public static MethodAccess from(ClassLoader loader, Class type) {
    Generator gen = new Generator(type);
    gen.setClassLoader(loader);
    gen.setNeighbor(type);
    return gen.create();
  }

}
