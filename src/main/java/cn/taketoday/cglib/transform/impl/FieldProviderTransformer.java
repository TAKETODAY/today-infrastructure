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
package cn.taketoday.cglib.transform.impl;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.asm.commons.TableSwitchGenerator;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.CodeGenerationException;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.ObjectSwitchCallback;
import cn.taketoday.cglib.transform.ClassEmitterTransformer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class FieldProviderTransformer extends ClassEmitterTransformer {

  private static final String FIELD_NAMES = "today$FieldNames";
  private static final String FIELD_TYPES = "today$FieldTypes";

  private static final Type FIELD_PROVIDER = Type.fromClass(FieldProvider.class);
  private static final Type ILLEGAL_ARGUMENT_EXCEPTION = Type.parse("IllegalArgumentException");
  private static final MethodSignature PROVIDER_GET = MethodSignature.from("Object getField(String)");
  private static final MethodSignature PROVIDER_SET = MethodSignature.from("void setField(String, Object)");
  private static final MethodSignature PROVIDER_SET_BY_INDEX = MethodSignature.from("void setField(int, Object)");
  private static final MethodSignature PROVIDER_GET_BY_INDEX = MethodSignature.from("Object getField(int)");
  private static final MethodSignature PROVIDER_GET_TYPES = MethodSignature.from("Class[] getFieldTypes()");
  private static final MethodSignature PROVIDER_GET_NAMES = MethodSignature.from("String[] getFieldNames()");

  private int access;
  private Map fields;

  public void beginClass(int version, int access, String className, Type superType, Type[] interfaces,
                         String sourceFile) {
    if (!Modifier.isAbstract(access)) {
      interfaces = Type.add(interfaces, FIELD_PROVIDER);
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

    super.declare_field(Opcodes.PRIVATE_FINAL_STATIC, FIELD_NAMES, Type.TYPE_STRING_ARRAY, null);
    super.declare_field(Opcodes.PRIVATE_FINAL_STATIC, FIELD_TYPES, Type.TYPE_CLASS_ARRAY, null);

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
    e.putStatic(getClassType(), FIELD_NAMES, Type.TYPE_STRING_ARRAY);

    e.push(names.length);
    e.newArray(Type.TYPE_CLASS);
    e.dup();
    for (int i = 0; i < names.length; i++) {
      e.dup();
      e.push(i);
      Type type = (Type) fields.get(names[i]);
      EmitUtils.loadClass(e, type);
      e.aastore();
    }
    e.putStatic(getClassType(), FIELD_TYPES, Type.TYPE_CLASS_ARRAY);
  }

  private void getNames() {
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_GET_NAMES);
    e.getStatic(getClassType(), FIELD_NAMES, Type.TYPE_STRING_ARRAY);
    e.returnValue();
    e.end_method();
  }

  private void getTypes() {
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_GET_TYPES);
    e.getStatic(getClassType(), FIELD_TYPES, Type.TYPE_CLASS_ARRAY);
    e.returnValue();
    e.end_method();
  }

  private void setByIndex(final String[] names, final int[] indexes) throws Exception {
    final CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_SET_BY_INDEX);
    e.loadThis();
    e.loadArg(1);
    e.loadArg(0);
    e.tableSwitch(indexes, new TableSwitchGenerator() {
      public void generateCase(int key, Label end) {
        Type type = (Type) fields.get(names[key]);
        e.unbox(type);
        e.putField(names[key]);
        e.returnValue();
      }

      public void generateDefault() {
        e.throwException(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
      }
    });
    e.end_method();
  }

  private void getByIndex(final String[] names, final int[] indexes) throws Exception {

    final CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_GET_BY_INDEX);
    e.loadThis();
    e.loadArg(0);
    e.tableSwitch(indexes, new TableSwitchGenerator() {
      public void generateCase(int key, Label end) {
        Type type = (Type) fields.get(names[key]);
        e.getField(names[key]);
        e.box(type);
        e.returnValue();
      }

      public void generateDefault() {
        e.throwException(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field index");
      }
    });
    e.end_method();
  }

  // TODO: if this is used to enhance class files SWITCH_STYLE_TRIE should be used
  // to avoid JVM hashcode implementation incompatibilities
  private void getField(String[] names) throws Exception {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_GET);
    e.loadThis();
    e.loadArg(0);
    EmitUtils.stringSwitch(e, names, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        Type type = (Type) fields.get(key);
        e.getField((String) key);
        e.box(type);
        e.returnValue();
      }

      public void processDefault() {
        e.throwException(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
      }
    });
    e.end_method();
  }

  private void setField(String[] names) throws Exception {
    final CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, PROVIDER_SET);
    e.loadThis();
    e.loadArg(1);
    e.loadArg(0);
    EmitUtils.stringSwitch(e, names, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        Type type = (Type) fields.get(key);
        e.unbox(type);
        e.putField((String) key);
        e.returnValue();
      }

      public void processDefault() {
        e.throwException(ILLEGAL_ARGUMENT_EXCEPTION, "Unknown field name");
      }
    });
    e.end_method();
  }
}
