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
package cn.taketoday.context.cglib.transform.impl;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Label;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.ObjectSwitchCallback;
import cn.taketoday.context.cglib.core.ProcessSwitchCallback;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.cglib.transform.ClassEmitterTransformer;

@SuppressWarnings("all")
public class FieldProviderTransformer extends ClassEmitterTransformer {

    private static final String FIELD_NAMES = "TODAY$FIELD_NAMES";
    private static final String FIELD_TYPES = "TODAY$FIELD_TYPES";

    private static final Type FIELD_PROVIDER = TypeUtils.parseType(FieldProvider.class);
    private static final Type ILLEGAL_ARGUMENT_EXCEPTION = TypeUtils.parseType("IllegalArgumentException");
    private static final Signature PROVIDER_GET = TypeUtils.parseSignature("Object getField(String)");
    private static final Signature PROVIDER_SET = TypeUtils.parseSignature("void setField(String, Object)");
    private static final Signature PROVIDER_SET_BY_INDEX = TypeUtils.parseSignature("void setField(int, Object)");
    private static final Signature PROVIDER_GET_BY_INDEX = TypeUtils.parseSignature("Object getField(int)");
    private static final Signature PROVIDER_GET_TYPES = TypeUtils.parseSignature("Class[] getFieldTypes()");
    private static final Signature PROVIDER_GET_NAMES = TypeUtils.parseSignature("String[] getFieldNames()");

    private int access;
    private Map fields;

    public void beginClass(int version, int access, String className, Type superType, Type[] interfaces,
                           String sourceFile) {
        if (!Modifier.isAbstract(access)) {
            interfaces = TypeUtils.add(interfaces, FIELD_PROVIDER);
        }
        this.access = access;
        fields = new HashMap();
        super.beginClass(version, access, className, superType, interfaces, sourceFile);
    }

    public void declare_field(int access, String name, Type type, Object value) {
        super.declare_field(access, name, type, value);

        if (!Modifier.isStatic(access)) {
            fields.put(name, type);
        }
    }

    public void endClass() {
        if (!Modifier.isInterface(access)) {
            try {
                generate();
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CodeGenerationException(e);
            }
        }
        super.endClass();
    }

    private void generate() throws Exception {
        final String[] names = (String[]) fields.keySet().toArray(new String[fields.size()]);

        int indexes[] = new int[names.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }

        super.declare_field(Constant.PRIVATE_FINAL_STATIC, FIELD_NAMES, Constant.TYPE_STRING_ARRAY, null);
        super.declare_field(Constant.PRIVATE_FINAL_STATIC, FIELD_TYPES, Constant.TYPE_CLASS_ARRAY, null);

        // use separate methods here because each process switch inner class needs a
        // final CodeEmitter
        initFieldProvider(names);
        getNames();
        getTypes();
        getField(names);
        setField(names);
        setByIndex(names, indexes);
        getByIndex(names, indexes);
    }

    private void initFieldProvider(String[] names) {
        CodeEmitter e = getStaticHook();
        EmitUtils.pushObject(e, names);
        e.putstatic(getClassType(), FIELD_NAMES, Constant.TYPE_STRING_ARRAY);

        e.push(names.length);
        e.newarray(Constant.TYPE_CLASS);
        e.dup();
        for (int i = 0; i < names.length; i++) {
            e.dup();
            e.push(i);
            Type type = (Type) fields.get(names[i]);
            EmitUtils.loadClass(e, type);
            e.aastore();
        }
        e.putstatic(getClassType(), FIELD_TYPES, Constant.TYPE_CLASS_ARRAY);
    }

    private void getNames() {
        CodeEmitter e = super.beginMethod(Constant.ACC_PUBLIC, PROVIDER_GET_NAMES);
        e.getstatic(getClassType(), FIELD_NAMES, Constant.TYPE_STRING_ARRAY);
        e.return_value();
        e.end_method();
    }

    private void getTypes() {
        CodeEmitter e = super.beginMethod(Constant.ACC_PUBLIC, PROVIDER_GET_TYPES);
        e.getstatic(getClassType(), FIELD_TYPES, Constant.TYPE_CLASS_ARRAY);
        e.return_value();
        e.end_method();
    }

    private void setByIndex(final String[] names, final int[] indexes) throws Exception {
        final CodeEmitter e = super.beginMethod(Constant.ACC_PUBLIC, PROVIDER_SET_BY_INDEX);
        e.load_this();
        e.load_arg(1);
        e.load_arg(0);
        e.process_switch(indexes, new ProcessSwitchCallback() {
            public void processCase(int key, Label end) throws Exception {
                Type type = (Type) fields.get(names[key]);
                e.unbox(type);
                e.putfield(names[key]);
                e.return_value();
            }

            public void processDefault() throws Exception {
                e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
            }
        });
        e.end_method();
    }

    private void getByIndex(final String[] names, final int[] indexes) throws Exception {

        final CodeEmitter e = super.beginMethod(Constant.ACC_PUBLIC, PROVIDER_GET_BY_INDEX);
        e.load_this();
        e.load_arg(0);
        e.process_switch(indexes, new ProcessSwitchCallback() {
            public void processCase(int key, Label end) throws Exception {
                Type type = (Type) fields.get(names[key]);
                e.getfield(names[key]);
                e.box(type);
                e.return_value();
            }

            public void processDefault() throws Exception {
                e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
            }
        });
        e.end_method();
    }

    // TODO: if this is used to enhance class files SWITCH_STYLE_TRIE should be used
    // to avoid JVM hashcode implementation incompatibilities
    private void getField(String[] names) throws Exception {
        final CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, PROVIDER_GET);
        e.load_this();
        e.load_arg(0);
        EmitUtils.stringSwitch(e, names, Constant.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                Type type = (Type) fields.get(key);
                e.getfield((String) key);
                e.box(type);
                e.return_value();
            }

            public void processDefault() {
                e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
            }
        });
        e.end_method();
    }

    private void setField(String[] names) throws Exception {
        final CodeEmitter e = beginMethod(Constant.ACC_PUBLIC, PROVIDER_SET);
        e.load_this();
        e.load_arg(1);
        e.load_arg(0);
        EmitUtils.stringSwitch(e, names, Constant.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                Type type = (Type) fields.get(key);
                e.unbox(type);
                e.putfield((String) key);
                e.return_value();
            }

            public void processDefault() {
                e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
            }
        });
        e.end_method();
    }
}
