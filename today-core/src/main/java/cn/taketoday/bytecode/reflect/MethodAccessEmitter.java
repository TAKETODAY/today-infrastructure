/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.bytecode.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.commons.TableSwitchGenerator;
import cn.taketoday.bytecode.core.Block;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.DuplicatesPredicate;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.core.MethodInfoTransformer;
import cn.taketoday.bytecode.core.ObjectSwitchCallback;
import cn.taketoday.bytecode.core.VisibilityPredicate;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-08 15:08
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class MethodAccessEmitter extends ClassEmitter {

  static final MethodSignature CSTRUCT_CLASS = MethodSignature.forConstructor("Class");
  static final MethodSignature METHOD_GET_INDEX = MethodSignature.from("int getIndex(String, Class[])");
  static final MethodSignature SIGNATURE_GET_INDEX = new MethodSignature(Type.INT_TYPE, "getIndex", Type.TYPE_SIGNATURE);
  static final MethodSignature CONSTRUCTOR_GET_INDEX = MethodSignature.from("int getIndex(Class[])");
  static final MethodSignature INVOKE = MethodSignature.from("Object invoke(int, Object, Object[])");
  static final MethodSignature NEW_INSTANCE = MethodSignature.from("Object newInstance(int, Object[])");
  static final MethodSignature GET_MAX_INDEX = MethodSignature.from("int getMaxIndex()");

  private static final Type FAST_CLASS = Type.fromClass(MethodAccess.class);
  private static final Type INVOCATION_TARGET_EXCEPTION =
          Type.fromInternalName("java/lang/reflect/InvocationTargetException");

  public MethodAccessEmitter(ClassVisitor v, String className, Class type) {
    super(v);

    Type base = Type.fromClass(type);
    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, FAST_CLASS, null, Constant.SOURCE_FILE);

    // constructor
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, CSTRUCT_CLASS);
    e.loadThis();
    e.loadArgs();
    e.super_invoke_constructor(CSTRUCT_CLASS);
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
    e = beginMethod(Opcodes.ACC_PUBLIC, CONSTRUCTOR_GET_INDEX);
    e.loadArgs();
    List<MethodInfo> info = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());
    EmitUtils.constructorSwitch(e, info, new GetIndexCallback(e, info));
    e.end_method();

    // invoke(int, Object, Object[])
    e = beginMethod(Opcodes.ACC_PUBLIC, INVOKE, INVOCATION_TARGET_EXCEPTION);
    e.loadArg(1);
    e.checkCast(base);
    e.loadArg(0);
    invokeSwitchHelper(e, methods, 2, base);
    e.end_method();

    // newInstance(int, Object[])
    e = beginMethod(Opcodes.ACC_PUBLIC, NEW_INSTANCE, INVOCATION_TARGET_EXCEPTION);
    e.newInstance(base);
    e.dup();
    e.loadArg(0);
    invokeSwitchHelper(e, constructors, 1, base);
    e.end_method();

    // getMaxIndex()
    e = beginMethod(Opcodes.ACC_PUBLIC, GET_MAX_INDEX);
    e.push(methods.size() - 1);
    e.returnValue();
    e.end_method();

    endClass();
  }

  // TODO: support constructor indices ("<init>")
  private void emitIndexBySignature(List<Method> methods) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, SIGNATURE_GET_INDEX);
    List<String> signatures = CollectionUtils.transform(methods, obj -> MethodSignature.from(obj).toString());
    e.loadArg(0);
    e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.TO_STRING);
    signatureSwitchHelper(e, signatures);
    e.end_method();
  }

  private static final int TOO_MANY_METHODS = 100; // TODO

  private void emitIndexByClassArray(List<Method> methods) {
    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, METHOD_GET_INDEX);
    if (methods.size() > TOO_MANY_METHODS) {
      // hack for big classes
      List<String> signatures = CollectionUtils.transform(methods, obj -> {
        String s = MethodSignature.from(obj).toString();
        return s.substring(0, s.lastIndexOf(')') + 1);
      });
      e.loadArgs();
      e.invokeStatic(FAST_CLASS, MethodSignature.from("String getSignatureWithoutReturnType(String, Class[])"));
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
    EmitUtils.wrapThrowable(block, INVOCATION_TARGET_EXCEPTION);
    e.mark(illegalArg);
    e.throwException(Type.fromClass(IllegalArgumentException.class), "Cannot find matching method/constructor");
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

  private static int[] getIntRange(int length) {
    int[] range = new int[length];
    for (int i = 0; i < length; i++) {
      range[i] = i;
    }
    return range;
  }
}
