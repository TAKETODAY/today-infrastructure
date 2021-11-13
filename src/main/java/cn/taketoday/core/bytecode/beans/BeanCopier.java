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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashMap;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.Local;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.CglibReflectUtils;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.Converter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.KeyFactory;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.core.bytecode.Opcodes.ACC_PUBLIC;
import static cn.taketoday.core.bytecode.Opcodes.JAVA_VERSION;
import static cn.taketoday.lang.Constant.SOURCE_FILE;

/**
 * @author Chris Nokleberg
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class BeanCopier {

  private static final Type CONVERTER = Type.fromClass(Converter.class);
  private static final Type BEAN_COPIER = Type.fromClass(BeanCopier.class);

  private static final BeanCopierKey KEY_FACTORY = KeyFactory.create(BeanCopierKey.class);

  private static final MethodSignature COPY = new MethodSignature(
          Type.VOID_TYPE, "copy", Type.TYPE_OBJECT, Type.TYPE_OBJECT, CONVERTER);

  private static final MethodSignature CONVERT = MethodSignature.from("Object convert(Object, Class, Object)");

  interface BeanCopierKey {
    Object newInstance(String source, String target, boolean useConverter);
  }

  public static BeanCopier create(Class source, Class target, boolean useConverter) {
    return new Generator(source, target, useConverter).create();
  }

  abstract public void copy(Object from, Object to, Converter converter);

  public static class Generator extends AbstractClassGenerator {

    private final Class source;
    private final Class target;
    private final boolean useConverter;

    public Generator(Class source, Class target, boolean useConverter) {
      super(BeanCopier.class);

      if (!Modifier.isPublic(source.getModifiers())) {
        setNamePrefix(source.getName());
      }

      if (!Modifier.isPublic(target.getModifiers())) {
        setNamePrefix(target.getName());
      }
      setNeighbor(source);

      this.source = source;
      this.target = target;
      this.useConverter = useConverter;
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
      return source.getClassLoader();
    }

    @Override
    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(source);
    }

    public BeanCopier create() {
      return (BeanCopier) super.create(KEY_FACTORY.newInstance(source.getName(), target.getName(), useConverter));
    }

    @Override
    public void generateClass(ClassVisitor v) {
      Type sourceType = Type.fromClass(source);
      Type targetType = Type.fromClass(target);
      ClassEmitter ce = new ClassEmitter(v);

      ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(), BEAN_COPIER, null, SOURCE_FILE);

      EmitUtils.nullConstructor(ce);
      CodeEmitter e = ce.beginMethod(ACC_PUBLIC, COPY);
      PropertyDescriptor[] getters = CglibReflectUtils.getBeanGetters(source);
      PropertyDescriptor[] setters = CglibReflectUtils.getBeanSetters(target);

      HashMap<String, PropertyDescriptor> names = new HashMap();
      for (final PropertyDescriptor propertyDescriptor : getters) {
        names.put(propertyDescriptor.getName(), propertyDescriptor);
      }
      Local targetLocal = e.newLocal();
      Local sourceLocal = e.newLocal();
      if (useConverter) {
        e.loadArg(1);
        e.checkCast(targetType);
        e.storeLocal(targetLocal);
        e.loadArg(0);
        e.checkCast(sourceType);
        e.storeLocal(sourceLocal);
      }
      else {
        e.loadArg(1);
        e.checkCast(targetType);
        e.loadArg(0);
        e.checkCast(sourceType);
      }

      for (PropertyDescriptor setter : setters) {
        PropertyDescriptor getter = names.get(setter.getName());
        if (getter != null) {
          MethodInfo read = MethodInfo.from(getter.getReadMethod());
          MethodInfo write = MethodInfo.from(setter.getWriteMethod());
          if (useConverter) {
            Type setterType = write.getSignature().getArgumentTypes()[0];
            e.loadLocal(targetLocal);
            e.loadArg(2);
            e.loadLocal(sourceLocal);
            e.invoke(read);
            e.box(read.getSignature().getReturnType());
            EmitUtils.loadClass(e, setterType);
            e.push(write.getSignature().getName());
            e.invokeInterface(CONVERTER, CONVERT);
            e.unbox_or_zero(setterType);
            e.invoke(write);
          }
          else if (compatible(getter, setter)) {
            e.dup2();
            e.invoke(read);
            e.invoke(write);
          }
        }
      }
      e.returnValue();
      e.end_method();
      ce.endClass();
    }

    private static boolean compatible(PropertyDescriptor getter, PropertyDescriptor setter) {
      // TODO: allow automatic widening conversions?
      return setter.getPropertyType().isAssignableFrom(getter.getPropertyType());
    }

    @Override
    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type);
    }

    @Override
    protected Object nextInstance(Object instance) {
      return instance;
    }
  }
}
