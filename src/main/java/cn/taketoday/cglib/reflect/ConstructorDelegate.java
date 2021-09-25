/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.KeyFactory;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.asm.Type.array;
import static cn.taketoday.core.Constant.SOURCE_FILE;

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
      ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(), CONSTRUCTOR_DELEGATE, array(Type.fromClass(iface)), SOURCE_FILE);

      Type declaring = Type.fromClass(constructor.getDeclaringClass());
      EmitUtils.nullConstructor(ce);
      CodeEmitter e = ce.beginMethod(
              ACC_PUBLIC, MethodSignature.from(newInstance), Type.getExceptionTypes(newInstance));
      e.newInstance(declaring);
      e.dup();
      e.load_args();
      e.invokeConstructor(declaring, MethodSignature.from(constructor));
      e.return_value();
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
