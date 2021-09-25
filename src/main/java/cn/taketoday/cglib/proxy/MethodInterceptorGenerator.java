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
package cn.taketoday.cglib.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.ClassInfo;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.asm.commons.Local;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.ObjectSwitchCallback;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-09-03 19:29
 */
final class MethodInterceptorGenerator implements CallbackGenerator {
  public static final MethodInterceptorGenerator INSTANCE = new MethodInterceptorGenerator();

  static final String FIND_PROXY_NAME = "today$FindMethodProxy";

  private static final Type METHOD = Type.fromClass(Method.class);
  private static final Type ABSTRACT_METHOD_ERROR = Type.fromClass(AbstractMethodError.class);

  private static final Type REFLECT_UTILS = Type.fromClass(CglibReflectUtils.class);
  private static final Type METHOD_PROXY = Type.fromClass(MethodProxy.class);
  private static final Type METHOD_INTERCEPTOR = Type.fromClass(MethodInterceptor.class);

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
      codeEmitter.return_value();
      codeEmitter.end_method();

      // around method
      codeEmitter = context.beginMethod(ce, method);
      Label nullInterceptor = codeEmitter.newLabel();
      context.emitCallback(codeEmitter, context.getIndex(method));
      codeEmitter.dup();
      codeEmitter.ifNull(nullInterceptor);

      codeEmitter.load_this(); // obj
      codeEmitter.getField(methodField); // method

      if (sig.getArgumentTypes().length == 0) {
        // empty args
        EmitUtils.loadEmptyArguments(codeEmitter);
      }
      else {
        codeEmitter.create_arg_array(); // args
      }

      codeEmitter.getField(methodProxyField);  // methodProxy
      codeEmitter.invokeInterface(METHOD_INTERCEPTOR, INTERCEPT);
      codeEmitter.unbox_or_zero(sig.getReturnType());
      codeEmitter.return_value();

      codeEmitter.mark(nullInterceptor);
      superHelper(codeEmitter, method, context);
      codeEmitter.return_value();
      codeEmitter.end_method();
    }
    generateFindProxy(ce, sigMap);
  }

  private static void superHelper(CodeEmitter e, MethodInfo method, Context context) {
    if (Modifier.isAbstract(method.getModifiers())) {
      e.throwException(ABSTRACT_METHOD_ERROR, method + " is abstract");
    }
    else {
      e.load_this();
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
    e.store_local(thisClass);

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
      e.store_local(declaringClass);
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

        e.load_local(declaringClass);
        e.load_local(thisClass);
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
    e.load_arg(0);
    e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.TO_STRING);

    final ObjectSwitchCallback callback = new ObjectSwitchCallback() {

      @Override
      public void processCase(final Object key, final Label end) {
        e.getField(sigMap.get(key));
        e.return_value();
      }

      @Override
      public void processDefault() {
        e.aconst_null();
        e.return_value();
      }
    };

    EmitUtils.stringSwitch(
            e, StringUtils.toStringArray(sigMap.keySet()), Opcodes.SWITCH_STYLE_HASH, callback);
    e.end_method();
  }
}
