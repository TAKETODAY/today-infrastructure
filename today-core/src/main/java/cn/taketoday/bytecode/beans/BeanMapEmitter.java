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
package cn.taketoday.bytecode.beans;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.CglibReflectUtils;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.core.ObjectSwitchCallback;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Constant;
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
      Iterator<String> it = allProps.keySet().iterator();
      while (it.hasNext()) {
        String name = it.next();
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
    e.loadThis();
    e.loadArg(0);
    e.super_invoke_constructor(CSTRUCT_OBJECT);
    e.returnValue();
    e.end_method();
  }

  private void generateGet(Class type, final Map<String, PropertyDescriptor> getters) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, BEAN_MAP_GET);
    e.loadArg(0);
    e.checkCast(Type.fromClass(type));
    e.loadArg(1);
    e.checkCast(Type.TYPE_STRING);
    EmitUtils.stringSwitch(e, getNames(getters), Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        PropertyDescriptor pd = getters.get(key);
        MethodInfo method = MethodInfo.from(pd.getReadMethod());
        e.invoke(method);
        e.box(method.getSignature().getReturnType());
        e.returnValue();
      }

      public void processDefault() {
        e.aconst_null();
        e.returnValue();
      }
    });
    e.end_method();
  }

  private void generatePut(Class type, final Map<String, PropertyDescriptor> setters) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, BEAN_MAP_PUT);
    e.loadArg(0);
    e.checkCast(Type.fromClass(type));
    e.loadArg(1);
    e.checkCast(Type.TYPE_STRING);
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
        e.loadArg(2); // new value
        MethodInfo write = MethodInfo.from(pd.getWriteMethod());
        e.unbox(write.getSignature().getArgumentTypes()[0]);
        e.invoke(write);
        e.returnValue();
      }

      public void processDefault() {
        // fall-through
      }
    });
    e.aconst_null();
    e.returnValue();
    e.end_method();
  }

  private void generateKeySet(String[] allNames) {
    // static initializer
    declare_field(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "keys", FIXED_KEY_SET, null);

    CodeEmitter e = begin_static();
    e.newInstance(FIXED_KEY_SET);
    e.dup();
    EmitUtils.pushArray(e, allNames);
    e.invokeConstructor(FIXED_KEY_SET, CSTRUCT_STRING_ARRAY);
    e.putField("keys");
    e.returnValue();
    e.end_method();

    // keySet
    e = beginMethod(Opcodes.ACC_PUBLIC, KEY_SET);
    e.loadThis();
    e.getField("keys");
    e.returnValue();
    e.end_method();
  }

  private void generateGetPropertyType(Map<String, PropertyDescriptor> allProps, String[] allNames) {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, GET_PROPERTY_TYPE);
    e.loadArg(0);
    EmitUtils.stringSwitch(e, allNames, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        PropertyDescriptor pd = allProps.get(key);
        EmitUtils.loadClass(e, Type.fromClass(pd.getPropertyType()));
        e.returnValue();
      }

      public void processDefault() {
        e.aconst_null();
        e.returnValue();
      }
    });
    e.end_method();
  }
}
