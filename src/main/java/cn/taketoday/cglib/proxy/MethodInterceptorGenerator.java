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
import cn.taketoday.cglib.core.CglibCollectionUtils;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.ClassInfo;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.Local;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.ObjectSwitchCallback;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.cglib.core.Transformer;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.context.Constant;

import static cn.taketoday.asm.Type.array;
import static cn.taketoday.context.Constant.PRIVATE_FINAL_STATIC;
import static cn.taketoday.context.Constant.SWITCH_STYLE_HASH;

/**
 * @author TODAY <br>
 * 2019-09-03 19:29
 */
final class MethodInterceptorGenerator implements CallbackGenerator {

  public static final MethodInterceptorGenerator INSTANCE = new MethodInterceptorGenerator();

  static final String FIND_PROXY_NAME = "today$FindMethodProxy";

  static final Class<?>[] FIND_PROXY_TYPES = { Signature.class };

  private static final Type METHOD = TypeUtils.parseType(Method.class);
  private static final Type ABSTRACT_METHOD_ERROR = TypeUtils.parseType(AbstractMethodError.class);

  private static final Type REFLECT_UTILS = TypeUtils.parseType(CglibReflectUtils.class);
  private static final Type METHOD_PROXY = TypeUtils.parseType(MethodProxy.class);
  private static final Type METHOD_INTERCEPTOR = TypeUtils.parseType(MethodInterceptor.class);

  private static final Signature GET_DECLARED_METHODS = //
          TypeUtils.parseSignature("java.lang.reflect.Method[] getDeclaredMethods()");

  private static final Signature FIND_METHODS = //
          TypeUtils.parseSignature("java.lang.reflect.Method[] findMethods(String[], java.lang.reflect.Method[])");

  private static final Signature MAKE_PROXY = new Signature("create",
                                                            METHOD_PROXY, //
                                                            array(Constant.TYPE_CLASS,
                                                                  Constant.TYPE_CLASS,
                                                                  Constant.TYPE_STRING,
                                                                  Constant.TYPE_STRING,
                                                                  Constant.TYPE_STRING)//
  );

  private static final Signature INTERCEPT = new Signature("intercept",
                                                           Constant.TYPE_OBJECT,
                                                           array(Constant.TYPE_OBJECT,
                                                                 METHOD,
                                                                 Constant.TYPE_OBJECT_ARRAY,
                                                                 METHOD_PROXY)//
  );

  private static final Signature FIND_PROXY = new Signature(FIND_PROXY_NAME, METHOD_PROXY, array(Constant.TYPE_SIGNATURE));
  private static final Signature TO_STRING = TypeUtils.parseSignature("String toString()");

  private static final Transformer<MethodInfo, ClassInfo> METHOD_TO_CLASS = MethodInfo::getClassInfo;

  private String getMethodField(Signature impl) {
    return impl.getName() + "$Method";
  }

  private String getMethodProxyField(Signature impl) {
    return impl.getName() + "$Proxy";
  }

  @Override
  public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {
    final HashMap<String, String> sigMap = new HashMap<>();

    for (final MethodInfo method : methods) {
      final Signature sig = method.getSignature();
      final Signature impl = context.getImplSignature(method);

      final String methodField = getMethodField(impl);
      final String methodProxyField = getMethodProxyField(impl);

      sigMap.put(sig.toString(), methodProxyField);
      ce.declare_field(PRIVATE_FINAL_STATIC, methodField, METHOD, null);
      ce.declare_field(PRIVATE_FINAL_STATIC, methodProxyField, METHOD_PROXY, null);

      // access method
      CodeEmitter codeEmitter = ce.beginMethod(Opcodes.ACC_FINAL, impl, method.getExceptionTypes());
      superHelper(codeEmitter, method, context);
      codeEmitter.return_value();
      codeEmitter.end_method();

      // around method
      codeEmitter = context.beginMethod(ce, method);
      Label nullInterceptor = codeEmitter.make_label();
      context.emitCallback(codeEmitter, context.getIndex(method));
      codeEmitter.dup();
      codeEmitter.ifnull(nullInterceptor);

      codeEmitter.load_this(); // obj
      codeEmitter.getfield(methodField); // method

      if (sig.getArgumentTypes().length == 0) {
        // empty args
        EmitUtils.loadEmptyArguments(codeEmitter);
      }
      else {
        codeEmitter.create_arg_array(); // args
      }

      codeEmitter.getfield(methodProxyField);  // methodProxy
      codeEmitter.invoke_interface(METHOD_INTERCEPTOR, INTERCEPT);
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
      e.throw_exception(ABSTRACT_METHOD_ERROR, method + " is abstract");
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

    Local thisClass = e.make_local();
    Local declaringClass = e.make_local();
    EmitUtils.loadClassThis(e);
    e.store_local(thisClass);

    final Map<ClassInfo, List<MethodInfo>> methodsByClass
            = CglibCollectionUtils.bucket(methods, METHOD_TO_CLASS);

    for (final Entry<ClassInfo, List<MethodInfo>> entry : methodsByClass.entrySet()) {

      final ClassInfo classInfo = entry.getKey();
      final List<MethodInfo> classMethods = entry.getValue();

      final int size = classMethods.size();
      e.push(2 * size);
      e.newArray(Constant.TYPE_STRING);
      for (int index = 0; index < size; index++) {
        Signature sig = classMethods.get(index).getSignature();
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
      e.invoke_virtual(Constant.TYPE_CLASS, GET_DECLARED_METHODS);
      e.invoke_static(REFLECT_UTILS, FIND_METHODS);

      for (int index = 0; index < size; index++) {
        MethodInfo method = classMethods.get(index);
        Signature sig = method.getSignature();
        Signature impl = context.getImplSignature(method);
        e.dup();
        e.push(index);
        e.array_load(METHOD);
        e.putfield(getMethodField(impl));

        e.load_local(declaringClass);
        e.load_local(thisClass);
        e.push(sig.getDescriptor());
        e.push(sig.getName());
        e.push(impl.getName());
        e.invoke_static(METHOD_PROXY, MAKE_PROXY);
        e.putfield(getMethodProxyField(impl));
      }
      e.pop();
    }
  }

  public void generateFindProxy(final ClassEmitter ce, final Map<String, String> sigMap) {

    final CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, FIND_PROXY);
    e.load_arg(0);
    e.invoke_virtual(Constant.TYPE_OBJECT, TO_STRING);

    final ObjectSwitchCallback callback = new ObjectSwitchCallback() {

      @Override
      public void processCase(final Object key, final Label end) {
        e.getfield(sigMap.get(key));
        e.return_value();
      }

      @Override
      public void processDefault() {
        e.aconst_null();
        e.return_value();
      }
    };

    EmitUtils.stringSwitch(e, sigMap.keySet().toArray(new String[sigMap.size()]), SWITCH_STYLE_HASH, callback);
    e.end_method();
  }
}
