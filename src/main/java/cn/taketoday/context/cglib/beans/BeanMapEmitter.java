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
package cn.taketoday.context.cglib.beans;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Label;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.ObjectSwitchCallback;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;

/**
 * 
 * @author TODAY <br>
 *         2019-09-04 19:47
 */
@SuppressWarnings("all")
class BeanMapEmitter extends ClassEmitter {

    private static final Type BEAN_MAP = TypeUtils.parseType(BeanMap.class);
    private static final Type FIXED_KEY_SET = TypeUtils.parseType(FixedKeySet.class);
    private static final Signature CSTRUCT_OBJECT = TypeUtils.parseConstructor("Object");
    private static final Signature CSTRUCT_STRING_ARRAY = TypeUtils.parseConstructor("String[]");
    private static final Signature BEAN_MAP_GET = TypeUtils.parseSignature("Object get(Object, Object)");
    private static final Signature BEAN_MAP_PUT = TypeUtils.parseSignature("Object put(Object, Object, Object)");
    private static final Signature KEY_SET = TypeUtils.parseSignature("java.util.Set keySet()");
    private static final Signature NEW_INSTANCE = new Signature("newInstance", BEAN_MAP, new Type[] { Constant.TYPE_OBJECT });
    private static final Signature GET_PROPERTY_TYPE = TypeUtils.parseSignature("Class getPropertyType(String)");

    public BeanMapEmitter(ClassVisitor v, String className, Class type, int require) {
        super(v);

        beginClass(Constant.JAVA_VERSION, Constant.ACC_PUBLIC, className, BEAN_MAP, null, Constant.SOURCE_FILE);
        EmitUtils.nullConstructor(this);
        EmitUtils.factoryMethod(this, NEW_INSTANCE);
        generateConstructor();

        Map getters = makePropertyMap(ReflectUtils.getBeanGetters(type));
        Map setters = makePropertyMap(ReflectUtils.getBeanSetters(type));
        Map allProps = new HashMap();
        allProps.putAll(getters);
        allProps.putAll(setters);

        if (require != 0) {
            for (Iterator it = allProps.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                if ((((require & BeanMap.REQUIRE_GETTER) != 0) && !getters.containsKey(
                        name)) || (((require & BeanMap.REQUIRE_SETTER) != 0) && !setters.containsKey(name))) {
                    it.remove();
                    getters.remove(name);
                    setters.remove(name);
                }
            }
        }
        generateGet(type, getters);
        generatePut(type, setters);

        String[] allNames = getNames(allProps);
        generateKeySet(allNames);
        generateGetPropertyType(allProps, allNames);
        endClass();
    }

    private Map makePropertyMap(PropertyDescriptor[] props) {
        Map names = new HashMap();
        for (int i = 0; i < props.length; i++) {
            names.put(((PropertyDescriptor) props[i]).getName(), props[i]);
        }
        return names;
    }

    private String[] getNames(Map propertyMap) {
        return (String[]) propertyMap.keySet().toArray(new String[propertyMap.size()]);
    }

    private void generateConstructor() {
        CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, CSTRUCT_OBJECT);
        e.load_this();
        e.load_arg(0);
        e.super_invoke_constructor(CSTRUCT_OBJECT);
        e.return_value();
        e.end_method();
    }

    private void generateGet(Class type, final Map getters) {
        final CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, BEAN_MAP_GET);
        e.load_arg(0);
        e.checkcast(Type.getType(type));
        e.load_arg(1);
        e.checkcast(Constant.TYPE_STRING);
        EmitUtils.stringSwitch(e, getNames(getters), Constant.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                PropertyDescriptor pd = (PropertyDescriptor) getters.get(key);
                MethodInfo method = ReflectUtils.getMethodInfo(pd.getReadMethod());
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

    private void generatePut(Class type, final Map setters) {
        final CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, BEAN_MAP_PUT);
        e.load_arg(0);
        e.checkcast(Type.getType(type));
        e.load_arg(1);
        e.checkcast(Constant.TYPE_STRING);
        EmitUtils.stringSwitch(e, getNames(setters), Constant.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                PropertyDescriptor pd = (PropertyDescriptor) setters.get(key);
                if (pd.getReadMethod() == null) {
                    e.aconst_null();
                }
                else {
                    MethodInfo read = ReflectUtils.getMethodInfo(pd.getReadMethod());
                    e.dup();
                    e.invoke(read);
                    e.box(read.getSignature().getReturnType());
                }
                e.swap(); // move old value behind bean
                e.load_arg(2); // new value
                MethodInfo write = ReflectUtils.getMethodInfo(pd.getWriteMethod());
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
        declare_field(Constant.ACC_STATIC | Constant.ACC_PRIVATE, "keys", FIXED_KEY_SET, null);

        CodeEmitter e = begin_static();
        e.new_instance(FIXED_KEY_SET);
        e.dup();
        EmitUtils.pushArray(e, allNames);
        e.invoke_constructor(FIXED_KEY_SET, CSTRUCT_STRING_ARRAY);
        e.putfield("keys");
        e.return_value();
        e.end_method();

        // keySet
        e = beginMethod(Constant.ACC_PUBLIC, KEY_SET);
        e.load_this();
        e.getfield("keys");
        e.return_value();
        e.end_method();
    }

    private void generateGetPropertyType(final Map allProps, String[] allNames) {
        final CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, GET_PROPERTY_TYPE);
        e.load_arg(0);
        EmitUtils.stringSwitch(e, allNames, Constant.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                PropertyDescriptor pd = (PropertyDescriptor) allProps.get(key);
                EmitUtils.loadClass(e, Type.getType(pd.getPropertyType()));
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
