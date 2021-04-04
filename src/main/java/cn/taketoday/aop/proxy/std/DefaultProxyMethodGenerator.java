/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.proxy.std;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.AdvisedSupport;
import cn.taketoday.aop.proxy.StandardProxyInvoker;
import cn.taketoday.aop.proxy.TargetInvocation;
import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.CglibReflectUtils;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.Local;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY 2021/3/7 20:23
 * @since 3.0
 */
public class DefaultProxyMethodGenerator implements ProxyMethodGenerator {

  private static final Signature proceed;
  private static final Signature dynamicProceed;
  private static final Signature staticExposeProceed;
  private static final Signature dynamicExposeProceed;
  private static final Signature dynamicAdvisedProceed;

  private static final Type stdProxyInvoker = Type.getType(StandardProxyInvoker.class);
  private static final Type targetInvocationType = Type.getType(TargetInvocation.class);

  static {
    try {
      proceed = new Signature(StandardProxyInvoker.class.getMethod("proceed",
                                                                   Object.class,
                                                                   TargetInvocation.class,
                                                                   Object[].class));
      dynamicProceed = new Signature(StandardProxyInvoker.class
                                             .getMethod("dynamicProceed",
                                                        TargetSource.class,
                                                        TargetInvocation.class,
                                                        Object[].class));
      dynamicExposeProceed = new Signature(StandardProxyInvoker.class.getMethod("dynamicExposeProceed",
                                                                                Object.class,
                                                                                TargetSource.class,
                                                                                TargetInvocation.class,
                                                                                Object[].class));
      staticExposeProceed = new Signature(StandardProxyInvoker.class.getMethod("staticExposeProceed",
                                                                               Object.class,
                                                                               Object.class,
                                                                               TargetInvocation.class,
                                                                               Object[].class));

      dynamicAdvisedProceed = new Signature(StandardProxyInvoker.class.getMethod("dynamicAdvisedProceed",
                                                                                 Object.class,
                                                                                 AdvisedSupport.class,
                                                                                 TargetInvocation.class,
                                                                                 Object[].class));
    }
    catch (NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }
  }

  @Override
  public boolean generate(Method method, GeneratorContext context) {
    final List<String> fields = context.getFields();
    final String targetInvField = putTargetInv(method, context, fields);
    context.addField(targetInvField);

    final ClassEmitter classEmitter = context.getClassEmitter();
    classEmitter.declare_field(getStaticAccess(), targetInvField, targetInvocationType, null);

    MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method);

    // current method start
    final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, methodInfo, method.getModifiers());

    // method proxy content
    generateProxyMethod(method, targetInvField, context, codeEmitter);

    // return
    Local returnLocal = null;
    if (method.getReturnType() != void.class) {
      returnLocal = codeEmitter.make_local();
      codeEmitter.store_local(returnLocal);
    }

    if (returnLocal != null) {
      codeEmitter.load_local(returnLocal);
      codeEmitter.unbox_or_zero(Type.getType(method.getReturnType()));
    }

    codeEmitter.return_value();
    codeEmitter.end_method();

    return true;
  }

  void generateProxyMethod(Method method, String targetInvField, GeneratorContext context, CodeEmitter codeEmitter) {
    final AdvisedSupport config = context.getConfig();
    final boolean exposeProxy = config.isExposeProxy();
    final boolean isStatic = config.getTargetSource().isStatic();
    final boolean opaque = config.isOpaque(); //

    if (opaque) {
      // cannot change interceptor chain
      if (exposeProxy) {
        // load proxy object: this
        codeEmitter.load_this();
      }
      codeEmitter.load_this();
      if (isStatic) {
        // Object target, Target targetInv, Object[] args
        codeEmitter.getfield(FIELD_TARGET);
        codeEmitter.getfield(targetInvField);
        prepareArgs(method, codeEmitter);

        if (exposeProxy) {
          codeEmitter.invoke_static(stdProxyInvoker, staticExposeProceed);
        }
        else {
          codeEmitter.invoke_static(stdProxyInvoker, proceed);
        }
      }
      else {
        //TargetSource targetSource, Target targetInv, Object[] args
        codeEmitter.getfield(FIELD_TARGET_SOURCE);
        codeEmitter.getfield(targetInvField);
        prepareArgs(method, codeEmitter);

        if (exposeProxy) {
          codeEmitter.invoke_static(stdProxyInvoker, dynamicExposeProceed);
        }
        else {
          codeEmitter.invoke_static(stdProxyInvoker, dynamicProceed);
        }
      }
    }
    else {
      // ------------------------------
      // dynamic Advised
      // Object proxy, AdvisedSupport advised, TargetInvocation targetInv, Object[] args

      codeEmitter.load_this();
      codeEmitter.load_this();
      codeEmitter.getfield(FIELD_CONFIG);
      codeEmitter.getfield(targetInvField);
      prepareArgs(method, codeEmitter);

      codeEmitter.invoke_static(stdProxyInvoker, dynamicAdvisedProceed);
    }

  }

  /**
   * @param method
   *         current method
   * @param fields
   *         Target keys in {@link GeneratorContext#targetClass}
   *
   * @return Target key
   */
  protected String putTargetInv(final Method method, GeneratorContext context, final List<String> fields) {
    final String field = method.getName() + StringUtils.getRandomString(4);
    if (fields.contains(field)) {
      return putTargetInv(method, context, fields);
    }
    final TargetInvocation target = TargetInvocation.getTarget(field);
    if (target != null) {
      return putTargetInv(method, context, fields);
    }
    TargetInvocation.putTarget(field, getTarget(method, context));
    return field;
  }

  protected int getStaticAccess() {
    return Constant.PRIVATE_FINAL_STATIC;
  }

  protected TargetInvocation getTarget(final Method method, GeneratorContext context) {
    return new TargetInvocation(method, context.getTargetClass(), context.getConfig());
  }

  protected void prepareArgs(Method method, CodeEmitter codeEmitter) {
    if (method.getParameterCount() == 0) {
      EmitUtils.loadEmptyArguments(codeEmitter);
    }
    else {
      codeEmitter.create_arg_array(); // args
    }
  }

}
