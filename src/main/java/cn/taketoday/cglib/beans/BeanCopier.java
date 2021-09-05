/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.Converter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.KeyFactory;
import cn.taketoday.cglib.core.Local;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.core.Constant.SOURCE_FILE;
import static cn.taketoday.core.Constant.TYPE_OBJECT;

/**
 * @author Chris Nokleberg
 */
@SuppressWarnings("all")
abstract public class BeanCopier {

  private static final Type CONVERTER = TypeUtils.parseType(Converter.class);
  private static final Type BEAN_COPIER = TypeUtils.parseType(BeanCopier.class);

  private static final BeanCopierKey KEY_FACTORY = (BeanCopierKey) KeyFactory.create(BeanCopierKey.class);

  private static final Signature COPY = new Signature("copy", Type.VOID_TYPE, //
                                                      Type.array(TYPE_OBJECT, TYPE_OBJECT, CONVERTER));

  private static final Signature CONVERT = TypeUtils.parseSignature("Object convert(Object, Class, Object)");

  interface BeanCopierKey {
    public Object newInstance(String source, String target, boolean useConverter);
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
      return CglibReflectUtils.getProtectionDomain(source);
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

      Map names = new HashMap();
      for (int i = 0; i < getters.length; i++) {
        names.put(getters[i].getName(), getters[i]);
      }
      Local targetLocal = e.make_local();
      Local sourceLocal = e.make_local();
      if (useConverter) {
        e.load_arg(1);
        e.checkcast(targetType);
        e.store_local(targetLocal);
        e.load_arg(0);
        e.checkcast(sourceType);
        e.store_local(sourceLocal);
      }
      else {
        e.load_arg(1);
        e.checkcast(targetType);
        e.load_arg(0);
        e.checkcast(sourceType);
      }
      for (int i = 0; i < setters.length; i++) {
        PropertyDescriptor setter = setters[i];
        PropertyDescriptor getter = (PropertyDescriptor) names.get(setter.getName());
        if (getter != null) {
          MethodInfo read = CglibReflectUtils.getMethodInfo(getter.getReadMethod());
          MethodInfo write = CglibReflectUtils.getMethodInfo(setter.getWriteMethod());
          if (useConverter) {
            Type setterType = write.getSignature().getArgumentTypes()[0];
            e.load_local(targetLocal);
            e.load_arg(2);
            e.load_local(sourceLocal);
            e.invoke(read);
            e.box(read.getSignature().getReturnType());
            EmitUtils.loadClass(e, setterType);
            e.push(write.getSignature().getName());
            e.invoke_interface(CONVERTER, CONVERT);
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
      e.return_value();
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
