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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.ObjectSwitchCallback;
import cn.taketoday.core.Constant;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-09-04 19:47
 */
@SuppressWarnings({ "rawtypes" })
class BeanMapEmitter extends ClassEmitter {

  private static final Type BEAN_MAP = Type.fromClass(BeanMap.class);
  private static final Type FIXED_KEY_SET = Type.fromClass(FixedKeySet.class);
  private static final MethodSignature CSTRUCT_OBJECT = MethodSignature.forConstructor("Object");
  private static final MethodSignature CSTRUCT_STRING_ARRAY = MethodSignature.forConstructor("String[]");
  private static final MethodSignature BEAN_MAP_GET = MethodSignature.from("Object get(Object, Object)");
  private static final MethodSignature BEAN_MAP_PUT = MethodSignature.from("Object put(Object, Object, Object)");
  private static final MethodSignature KEY_SET = MethodSignature.from("java.util.Set keySet()");
  private static final MethodSignature NEW_INSTANCE = new MethodSignature(BEAN_MAP, "newInstance", Type.TYPE_OBJECT);
  private static final MethodSignature GET_PROPERTY_TYPE = MethodSignature.from("Class getPropertyType(String)");

  public BeanMapEmitter(final ClassVisitor v, final String className, final Class type, final int require) {
    super(v);

    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, BEAN_MAP, null, Constant.SOURCE_FILE);
    EmitUtils.nullConstructor(this);
    EmitUtils.factoryMethod(this, NEW_INSTANCE);
    generateConstructor();

    final Map<String, PropertyDescriptor> getters = toMap(CglibReflectUtils.getBeanGetters(type));
    final Map<String, PropertyDescriptor> setters = toMap(CglibReflectUtils.getBeanSetters(type));
    final HashMap<String, PropertyDescriptor> allProps = new HashMap<>();
    allProps.putAll(getters);
    allProps.putAll(setters);

    if (require != 0) {
      for (final Iterator<String> it = allProps.keySet().iterator(); it.hasNext(); ) {
        final String name = it.next();
        if ((((require & BeanMap.REQUIRE_GETTER) != 0) && !getters.containsKey(name))
                || (((require & BeanMap.REQUIRE_SETTER) != 0) && !setters.containsKey(name))) {

          it.remove();
          getters.remove(name);
          setters.remove(name);
        }
      }
    }
    generateGet(type, getters);
    generatePut(type, setters);

    final String[] allNames = getNames(allProps);
    generateKeySet(allNames);
    generateGetPropertyType(allProps, allNames);
    endClass();
  }

  private Map<String, PropertyDescriptor> toMap(PropertyDescriptor[] props) {
    HashMap<String, PropertyDescriptor> names = new HashMap<>();
    for (final PropertyDescriptor prop : props) {
      names.put(prop.getName(), prop);
    }
    return names;
  }

  private String[] getNames(Map<String, PropertyDescriptor> propertyMap) {
    return StringUtils.toStringArray(propertyMap.keySet());
  }

  private void generateConstructor() {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, CSTRUCT_OBJECT);
    e.load_this();
    e.load_arg(0);
    e.super_invoke_constructor(CSTRUCT_OBJECT);
    e.return_value();
    e.end_method();
  }

  private void generateGet(Class type, final Map<String, PropertyDescriptor> getters) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, BEAN_MAP_GET);
    e.load_arg(0);
    e.checkcast(Type.fromClass(type));
    e.load_arg(1);
    e.checkcast(Type.TYPE_STRING);
    EmitUtils.stringSwitch(e, getNames(getters), Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        PropertyDescriptor pd = getters.get(key);
        MethodInfo method = MethodInfo.from(pd.getReadMethod());
        e.invoke(method);
        e.box(method.getSignature().getReturnType());
        e.return_value();
      }

      public void processDefault() {
        e.aconst_null();
        e.return_value();
      }
    });
    e.end_method();
  }

  private void generatePut(Class type, final Map<String, PropertyDescriptor> setters) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, BEAN_MAP_PUT);
    e.load_arg(0);
    e.checkcast(Type.fromClass(type));
    e.load_arg(1);
    e.checkcast(Type.TYPE_STRING);
    EmitUtils.stringSwitch(e, getNames(setters), Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        PropertyDescriptor pd = setters.get(key);
        if (pd.getReadMethod() == null) {
          e.aconst_null();
        }
        else {
          MethodInfo read = MethodInfo.from(pd.getReadMethod());
          e.dup();
          e.invoke(read);
          e.box(read.getSignature().getReturnType());
        }
        e.swap(); // move old value behind bean
        e.load_arg(2); // new value
        MethodInfo write = MethodInfo.from(pd.getWriteMethod());
        e.unbox(write.getSignature().getArgumentTypes()[0]);
        e.invoke(write);
        e.return_value();
      }

      public void processDefault() {
        // fall-through
      }
    });
    e.aconst_null();
    e.return_value();
    e.end_method();
  }

  private void generateKeySet(String[] allNames) {
    // static initializer
    declare_field(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "keys", FIXED_KEY_SET, null);

    CodeEmitter e = begin_static();
    e.newInstance(FIXED_KEY_SET);
    e.dup();
    EmitUtils.pushArray(e, allNames);
    e.invoke_constructor(FIXED_KEY_SET, CSTRUCT_STRING_ARRAY);
    e.putfield("keys");
    e.return_value();
    e.end_method();

    // keySet
    e = beginMethod(Opcodes.ACC_PUBLIC, KEY_SET);
    e.load_this();
    e.getfield("keys");
    e.return_value();
    e.end_method();
  }

  private void generateGetPropertyType(Map<String, PropertyDescriptor> allProps, String[] allNames) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, GET_PROPERTY_TYPE);
    e.load_arg(0);
    EmitUtils.stringSwitch(e, allNames, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        PropertyDescriptor pd = allProps.get(key);
        EmitUtils.loadClass(e, Type.fromClass(pd.getPropertyType()));
        e.return_value();
      }

      public void processDefault() {
        e.aconst_null();
        e.return_value();
      }
    });
    e.end_method();
  }
}
