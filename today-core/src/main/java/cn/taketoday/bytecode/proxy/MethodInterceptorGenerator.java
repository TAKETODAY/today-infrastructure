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

package cn.taketoday.bytecode.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.Local;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.core.CglibReflectUtils;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.ClassInfo;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.core.ObjectSwitchCallback;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-03 19:29
 */
final class MethodInterceptorGenerator implements CallbackGenerator {
  public static final MethodInterceptorGenerator INSTANCE = new MethodInterceptorGenerator();

  static final String FIND_PROXY_NAME = "today$FindMethodProxy";

  private static final Type METHOD = Type.forClass(Method.class);

  private static final Type REFLECT_UTILS = Type.forClass(CglibReflectUtils.class);
  private static final Type METHOD_PROXY = Type.forClass(MethodProxy.class);
  private static final Type METHOD_INTERCEPTOR = Type.forClass(MethodInterceptor.class);

  private static final MethodSignature GET_DECLARED_METHODS = //
          MethodSignature.from("java.lang.reflect.Method[] getDeclaredMethods()");

  private static final MethodSignature FIND_METHODS = //
          MethodSignature.from("java.lang.reflect.Method[] findMethods(String[], java.lang.reflect.Method[])");

  private static final MethodSignature MAKE_PROXY = new MethodSignature(
          METHOD_PROXY,
          "create",
          Type.TYPE_CLASS, Type.TYPE_CLASS, Type.TYPE_STRING, Type.TYPE_STRING, Type.TYPE_STRING
  );

  private static final MethodSignature INTERCEPT = new MethodSignature(
          Type.TYPE_OBJECT,
          "intercept",
          Type.TYPE_OBJECT, METHOD, Type.TYPE_OBJECT_ARRAY, METHOD_PROXY
  );

  private static final MethodSignature FIND_PROXY = new MethodSignature(
          METHOD_PROXY, FIND_PROXY_NAME, Type.TYPE_SIGNATURE);

  private String getMethodField(MethodSignature impl) {
    return impl.getName() + "$Method";
  }

  private String getMethodProxyField(MethodSignature impl) {
    return impl.getName() + "$Proxy";
  }

  @Override
  public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {
    final HashMap<String, String> sigMap = new HashMap<>();

    for (final MethodInfo method : methods) {
      final MethodSignature sig = method.getSignature();
      final MethodSignature impl = context.getImplSignature(method);

      final String methodField = getMethodField(impl);
      final String methodProxyField = getMethodProxyField(impl);

      sigMap.put(sig.toString(), methodProxyField);
      ce.declare_field(Opcodes.PRIVATE_FINAL_STATIC, methodField, METHOD, null);
      ce.declare_field(Opcodes.PRIVATE_FINAL_STATIC, methodProxyField, METHOD_PROXY, null);

      // access method
      CodeEmitter codeEmitter = ce.beginMethod(Opcodes.ACC_FINAL, impl, method.getExceptionTypes());
      superHelper(codeEmitter, method, context);
      codeEmitter.returnValue();
      codeEmitter.end_method();

      // around method
      codeEmitter = context.beginMethod(ce, method);
      Label nullInterceptor = codeEmitter.newLabel();
      context.emitCallback(codeEmitter, context.getIndex(method));
      codeEmitter.dup();
      codeEmitter.ifNull(nullInterceptor);

      codeEmitter.loadThis(); // obj
      codeEmitter.getField(methodField); // method

      if (sig.getArgumentTypes().length == 0) {
        // empty args
        EmitUtils.loadEmptyArguments(codeEmitter);
      }
      else {
        codeEmitter.loadArgArray(); // args
      }

      codeEmitter.getField(methodProxyField);  // methodProxy
      codeEmitter.invokeInterface(METHOD_INTERCEPTOR, INTERCEPT);
      codeEmitter.unbox_or_zero(sig.getReturnType());
      codeEmitter.returnValue();

      codeEmitter.mark(nullInterceptor);
      superHelper(codeEmitter, method, context);
      codeEmitter.returnValue();
      codeEmitter.end_method();
    }
    generateFindProxy(ce, sigMap);
  }

  private static void superHelper(CodeEmitter e, MethodInfo method, Context context) {
    if (Modifier.isAbstract(method.getModifiers())) {
      e.throwException(Type.forClass(AbstractMethodError.class), method + " is abstract");
    }
    else {
      e.loadThis();
      context.emitLoadArgsAndInvoke(e, method);
    }
  }

  // generates
  // ---------------------------------
  // static {
  //        Class thisClass = Class.forName("NameOfThisClass");
  //        Class cls = Class.forName("java.lang.Object");
  //        String[] sigs = new String[] { "toString", "()Ljava/lang/String;" };
  //        Method[] methods = cls.getDeclaredMethods();
  //        methods = ReflectUtils.findMethods(sigs, methods);
  //        METHOD_0 = methods[0];
  //        TODAY$ACCESS_0 = MethodProxy.create(cls, thisClass, "()Ljava/lang/String;", "toString", "TODAY$ACCESS_0");
  // }

  @Override
  public void generateStatic(final CodeEmitter e, final Context context, final List<MethodInfo> methods) throws Exception {

    Local thisClass = e.newLocal();
    Local declaringClass = e.newLocal();
    EmitUtils.loadClassThis(e);
    e.storeLocal(thisClass);

    Map<ClassInfo, List<MethodInfo>> methodsByClass
            = CollectionUtils.buckets(methods, MethodInfo::getClassInfo);

    for (final Entry<ClassInfo, List<MethodInfo>> entry : methodsByClass.entrySet()) {

      final ClassInfo classInfo = entry.getKey();
      final List<MethodInfo> classMethods = entry.getValue();

      final int size = classMethods.size();
      e.push(2 * size);
      e.newArray(Type.TYPE_STRING);
      for (int index = 0; index < size; index++) {
        MethodSignature sig = classMethods.get(index).getSignature();
        e.dup();
        e.push(2 * index);
        e.push(sig.getName());
        e.aastore();
        e.dup();
        e.push(2 * index + 1);
        e.push(sig.getDescriptor());
        e.aastore();
      }

      EmitUtils.loadClass(e, classInfo.getType());
      e.dup();
      e.storeLocal(declaringClass);
      e.invokeVirtual(Type.TYPE_CLASS, GET_DECLARED_METHODS);
      e.invokeStatic(REFLECT_UTILS, FIND_METHODS);

      for (int index = 0; index < size; index++) {
        MethodInfo method = classMethods.get(index);
        MethodSignature sig = method.getSignature();
        MethodSignature impl = context.getImplSignature(method);
        e.dup();
        e.push(index);
        e.arrayLoad(METHOD);
        e.putField(getMethodField(impl));

        e.loadLocal(declaringClass);
        e.loadLocal(thisClass);
        e.push(sig.getDescriptor());
        e.push(sig.getName());
        e.push(impl.getName());
        e.invokeStatic(METHOD_PROXY, MAKE_PROXY);
        e.putField(getMethodProxyField(impl));
      }
      e.pop();
    }
  }

  public void generateFindProxy(final ClassEmitter ce, final Map<String, String> sigMap) {

    final CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, FIND_PROXY);
    e.loadArg(0);
    e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.TO_STRING);

    final ObjectSwitchCallback callback = new ObjectSwitchCallback() {

      @Override
      public void processCase(final Object key, final Label end) {
        e.getField(sigMap.get(key));
        e.returnValue();
      }

      @Override
      public void processDefault() {
        e.aconst_null();
        e.returnValue();
      }
    };

    EmitUtils.stringSwitch(
            e, StringUtils.toStringArray(sigMap.keySet()), Opcodes.SWITCH_STYLE_HASH, callback);
    e.end_method();
  }
}
