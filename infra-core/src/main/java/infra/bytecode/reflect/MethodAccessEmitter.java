/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import infra.bytecode.ClassVisitor;
import infra.bytecode.Label;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.commons.TableSwitchGenerator;
import infra.bytecode.core.Block;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.DuplicatesPredicate;
import infra.bytecode.core.EmitUtils;
import infra.bytecode.core.MethodInfo;
import infra.bytecode.core.MethodInfoTransformer;
import infra.bytecode.core.ObjectSwitchCallback;
import infra.bytecode.core.VisibilityPredicate;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-08 15:08
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class MethodAccessEmitter extends ClassEmitter {

  public MethodAccessEmitter(ClassVisitor v, String className, Class type) {
    super(v);

    Type base = Type.forClass(type);
    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, Type.forClass(MethodAccess.class), null, Constant.SOURCE_FILE);

    MethodSignature signature = MethodSignature.forConstructor("Class");
    // constructor
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, signature);
    e.loadThis();
    e.loadArgs();
    e.super_invoke_constructor(signature);
    e.returnValue();
    e.end_method();

    VisibilityPredicate vp = new VisibilityPredicate(type, false);
    List<Method> methods = MethodInfo.addAllMethods(type, new ArrayList<>());
    CollectionUtils.filter(methods, vp);
    CollectionUtils.filter(methods, new DuplicatesPredicate());

    Constructor[] declaredConstructors = type.getDeclaredConstructors();
    ArrayList<Constructor> constructors = new ArrayList<>(declaredConstructors.length);
    Collections.addAll(constructors, declaredConstructors);
    CollectionUtils.filter(constructors, vp);

    // getIndex(String)
    emitIndexBySignature(methods);

    // getIndex(String, Class[])
    emitIndexByClassArray(methods);

    // getIndex(Class[])
    e = beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.from("int getIndex(Class[])"));
    e.loadArgs();
    List<MethodInfo> info = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());
    EmitUtils.constructorSwitch(e, info, new GetIndexCallback(e, info));
    e.end_method();

    // invoke(int, Object, Object[])
    e = beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.from("Object invoke(int, Object, Object[])"), invocationTargetException());
    e.loadArg(1);
    e.checkCast(base);
    e.loadArg(0);
    invokeSwitchHelper(e, methods, 2, base);
    e.end_method();

    // newInstance(int, Object[])
    e = beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.from("Object newInstance(int, Object[])"), invocationTargetException());
    e.newInstance(base);
    e.dup();
    e.loadArg(0);
    invokeSwitchHelper(e, constructors, 1, base);
    e.end_method();

    // getMaxIndex()
    e = beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.from("int getMaxIndex()"));
    e.push(methods.size() - 1);
    e.returnValue();
    e.end_method();

    endClass();
  }

  // TODO: support constructor indices ("<init>")
  private void emitIndexBySignature(List<Method> methods) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(Type.INT_TYPE, "getIndex", Type.TYPE_SIGNATURE));
    List<String> signatures = CollectionUtils.transform(methods, obj -> MethodSignature.from(obj).toString());
    e.loadArg(0);
    e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.TO_STRING);
    signatureSwitchHelper(e, signatures);
    e.end_method();
  }

  private static final int TOO_MANY_METHODS = 100; // TODO

  private void emitIndexByClassArray(List<Method> methods) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.from("int getIndex(String, Class[])"));
    if (methods.size() > TOO_MANY_METHODS) {
      // hack for big classes
      List<String> signatures = CollectionUtils.transform(methods, obj -> {
        String s = MethodSignature.from(obj).toString();
        return s.substring(0, s.lastIndexOf(')') + 1);
      });
      e.loadArgs();
      e.invokeStatic(Type.forClass(MethodAccess.class), MethodSignature.from("String getSignatureWithoutReturnType(String, Class[])"));
      signatureSwitchHelper(e, signatures);
    }
    else {
      e.loadArgs();
      List<MethodInfo> info = CollectionUtils.transform(methods, MethodInfoTransformer.getInstance());
      EmitUtils.methodSwitch(e, info, new GetIndexCallback(e, info));
    }
    e.end_method();
  }

  private void signatureSwitchHelper(CodeEmitter e, List<String> signatures) {
    ObjectSwitchCallback callback = new ObjectSwitchCallback() {
      public void processCase(Object key, Label end) {
        // TODO: remove linear indexOf
        e.push(signatures.indexOf(key));
        e.returnValue();
      }

      public void processDefault() {
        e.push(-1);
        e.returnValue();
      }
    };

    String[] strings = StringUtils.toStringArray(signatures);
    EmitUtils.stringSwitch(e, strings, Opcodes.SWITCH_STYLE_HASH, callback);
  }

  private static void invokeSwitchHelper(CodeEmitter e, List members, int arg, Type base) {
    List<MethodInfo> info = CollectionUtils.transform(members, MethodInfoTransformer.getInstance());
    Label illegalArg = e.newLabel();
    Block block = e.begin_block();
    e.tableSwitch(getIntRange(info.size()), new TableSwitchGenerator() {
      public void generateCase(int key, Label end) {
        MethodInfo method = info.get(key);
        Type[] types = method.getSignature().getArgumentTypes();
        for (int i = 0; i < types.length; i++) {
          e.loadArg(arg);
          e.aaload(i);
          e.unbox(types[i]);
        }
        // TODO: change method lookup process so MethodInfo will already reference base
        // instead of superclass when superclass method is inaccessible
        e.invoke(method, base);
        if (!method.isConstructor()) {
          e.box(method.getSignature().getReturnType());
        }
        e.returnValue();
      }

      public void generateDefault() {
        e.goTo(illegalArg);
      }
    });
    block.end();
    EmitUtils.wrapThrowable(block, invocationTargetException());
    e.mark(illegalArg);
    e.throwException(Type.forClass(IllegalArgumentException.class), "Cannot find matching method/constructor");
  }

  private static Type invocationTargetException() {
    return Type.forInternalName("java/lang/reflect/InvocationTargetException");
  }

  private static int[] getIntRange(int length) {
    int[] range = new int[length];
    for (int i = 0; i < length; i++) {
      range[i] = i;
    }
    return range;
  }

  private static final class GetIndexCallback implements ObjectSwitchCallback {
    private final CodeEmitter codeEmitter;
    private final HashMap<Object, Integer> indexes = new HashMap<>();

    public GetIndexCallback(CodeEmitter e, List methods) {
      this.codeEmitter = e;
      int index = 0;
      for (Object object : methods) {
        indexes.put(object, index++);
      }
    }

    @Override
    public void processCase(Object key, Label end) {
      codeEmitter.push(indexes.get(key));
      codeEmitter.returnValue();
    }

    @Override
    public void processDefault() {
      codeEmitter.push(-1);
      codeEmitter.returnValue();
    }
  }

}
