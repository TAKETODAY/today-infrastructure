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
package cn.taketoday.core.bytecode.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import cn.taketoday.lang.Constant;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.CglibReflectUtils;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author Chris Nokleberg
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ImmutableBean {

  private static final Type ILLEGAL_STATE_EXCEPTION = Type.fromInternalName("java/lang/IllegalStateException");
  private static final MethodSignature CSTRUCT_OBJECT = MethodSignature.forConstructor("Object");
  private static final Class[] OBJECT_CLASSES = { Object.class };
  private static final String FIELD_NAME = "today$RWbean";

  public static Object create(Object bean) {
    Generator gen = new Generator();
    gen.setBean(bean);
    return gen.create();
  }

  public static class Generator extends AbstractClassGenerator {
    private Object bean;
    private Class target;

    public Generator() {
      super(ImmutableBean.class);
    }

    public void setBean(Object bean) {
      this.bean = bean;
      target = bean.getClass();
    }

    protected ClassLoader getDefaultClassLoader() {
      return target.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(target);
    }

    public Object create() {
      String name = target.getName();
      setNamePrefix(name);
      return super.create(name);
    }

    public void generateClass(ClassVisitor v) {
      Type targetType = Type.fromClass(target);
      ClassEmitter ce = new ClassEmitter(v);
      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), targetType, null, Constant.SOURCE_FILE);

      ce.declare_field(Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE, FIELD_NAME, targetType, null);

      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, CSTRUCT_OBJECT);
      e.loadThis();
      e.super_invoke_constructor();
      e.loadThis();
      e.loadArg(0);
      e.checkCast(targetType);
      e.putField(FIELD_NAME);
      e.returnValue();
      e.end_method();

      PropertyDescriptor[] descriptors = CglibReflectUtils.getBeanProperties(target);
      Method[] getters = CglibReflectUtils.getPropertyMethods(descriptors, true, false);
      Method[] setters = CglibReflectUtils.getPropertyMethods(descriptors, false, true);

      for (final Method value : getters) {
        MethodInfo getter = MethodInfo.from(value);
        e = EmitUtils.beginMethod(ce, getter, Opcodes.ACC_PUBLIC);
        e.loadThis();
        e.getField(FIELD_NAME);
        e.invoke(getter);
        e.returnValue();
        e.end_method();
      }

      for (final Method method : setters) {
        MethodInfo setter = MethodInfo.from(method);
        e = EmitUtils.beginMethod(ce, setter, Opcodes.ACC_PUBLIC);
        e.throwException(ILLEGAL_STATE_EXCEPTION, "Bean is immutable");
        e.end_method();
      }

      ce.endClass();
    }

    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type, OBJECT_CLASSES, new Object[] { bean });
    }

    // TODO: optimize
    protected Object nextInstance(Object instance) {
      return firstInstance(instance.getClass());
    }
  }
}
