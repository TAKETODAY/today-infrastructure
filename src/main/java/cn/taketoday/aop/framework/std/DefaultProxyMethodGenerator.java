/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.framework.std;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.AdvisedSupport;
import cn.taketoday.aop.framework.StandardProxyInvoker;
import cn.taketoday.aop.framework.TargetInvocation;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.Local;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.CodeGenerationException;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/3/7 20:23
 * @since 3.0
 */
public class DefaultProxyMethodGenerator implements ProxyMethodGenerator {

  private static final MethodSignature proceed;
  private static final MethodSignature dynamicProceed;
  private static final MethodSignature staticExposeProceed;
  private static final MethodSignature dynamicExposeProceed;
  private static final MethodSignature dynamicAdvisedProceed;

  private static final Type stdProxyInvoker = Type.fromClass(StandardProxyInvoker.class);
  private static final Type targetInvocationType = Type.fromClass(TargetInvocation.class);

  static {
    try {
      Class<StandardProxyInvoker> aClass = StandardProxyInvoker.class;
      proceed = MethodSignature.from(aClass.getMethod("proceed",
              Object.class,
              Object.class,
              TargetInvocation.class,
              Object[].class)
      );
      dynamicProceed = MethodSignature.from(aClass.getMethod("dynamicProceed",
              Object.class,
              TargetSource.class,
              TargetInvocation.class,
              Object[].class)
      );
      dynamicExposeProceed = MethodSignature.from(aClass.getMethod("dynamicExposeProceed",
              Object.class,
              TargetSource.class,
              TargetInvocation.class,
              Object[].class)
      );
      staticExposeProceed = MethodSignature.from(aClass.getMethod("staticExposeProceed",
              Object.class,
              Object.class,
              TargetInvocation.class,
              Object[].class)
      );

      dynamicAdvisedProceed = MethodSignature.from(aClass.getMethod("dynamicAdvisedProceed",
              Object.class,
              AdvisedSupport.class,
              TargetInvocation.class,
              Object[].class)
      );
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

    final int modifiers = excludeAbstractModifiers(method); // fixed @since 3.0.1
    final MethodInfo methodInfo = MethodInfo.from(method, modifiers);
    // current method start
    final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, methodInfo, modifiers);

    // method proxy content
    generateProxyMethod(method, targetInvField, context, codeEmitter);

    // return
    Local returnLocal = null;
    if (method.getReturnType() != void.class) {
      returnLocal = codeEmitter.newLocal();
      codeEmitter.storeLocal(returnLocal);
    }

    if (returnLocal != null) {
      codeEmitter.loadLocal(returnLocal);
      codeEmitter.unbox_or_zero(Type.fromClass(method.getReturnType()));
    }

    codeEmitter.returnValue();
    codeEmitter.end_method();

    return true;
  }

  protected void generateProxyMethod(Method method, String targetInvField, GeneratorContext context, CodeEmitter codeEmitter) {
    final AdvisedSupport config = context.getConfig();
    final boolean exposeProxy = config.isExposeProxy();
    final boolean isStatic = config.getTargetSource().isStatic();
    final boolean opaque = config.isOpaque(); //

    if (opaque) {
      // cannot change interceptor chain
      // load proxy object: this
      codeEmitter.loadThis();
      codeEmitter.loadThis();
      if (isStatic) {
        // Object target, Target targetInv, Object[] args
        codeEmitter.getField(FIELD_TARGET);
        codeEmitter.getField(targetInvField);
        prepareArgs(method, codeEmitter);

        if (exposeProxy) {
          codeEmitter.invokeStatic(stdProxyInvoker, staticExposeProceed);
        }
        else {
          codeEmitter.invokeStatic(stdProxyInvoker, proceed);
        }
      }
      else {
        //TargetSource targetSource, Target targetInv, Object[] args
        codeEmitter.getField(FIELD_TARGET_SOURCE);
        codeEmitter.getField(targetInvField);
        prepareArgs(method, codeEmitter);

        if (exposeProxy) {
          codeEmitter.invokeStatic(stdProxyInvoker, dynamicExposeProceed);
        }
        else {
          codeEmitter.invokeStatic(stdProxyInvoker, dynamicProceed);
        }
      }
    }
    else {
      // ------------------------------
      // dynamic Advised
      // Object proxy, AdvisedSupport advised, TargetInvocation targetInv, Object[] args

      codeEmitter.loadThis();
      codeEmitter.loadThis();
      codeEmitter.getField(FIELD_CONFIG);
      codeEmitter.getField(targetInvField);
      prepareArgs(method, codeEmitter);

      codeEmitter.invokeStatic(stdProxyInvoker, dynamicAdvisedProceed);
    }

  }

  /**
   * @param method current method
   * @param fields Target keys in {@link GeneratorContext#targetClass}
   * @return Target key
   */
  protected String putTargetInv(final Method method, GeneratorContext context, final List<String> fields) {
    final String field = method.getName() + StringUtils.generateRandomString(4);
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
    return Opcodes.PRIVATE_FINAL_STATIC;
  }

  protected TargetInvocation getTarget(final Method method, GeneratorContext context) {
    return new TargetInvocation(method, context.getTargetClass(), context.getConfig());
  }

  protected void prepareArgs(Method method, CodeEmitter codeEmitter) {
    if (method.getParameterCount() == 0) {
      EmitUtils.loadEmptyArguments(codeEmitter);
    }
    else {
      codeEmitter.loadArgArray(); // args
    }
  }

  // static

  /**
   * @since 3.0.2
   */
  static int excludeAbstractModifiers(Method method) {
    final int modifiers = method.getModifiers();
    if (Modifier.isAbstract(modifiers)) {
      return Modifier.PUBLIC;
    }
    return modifiers;
  }

}
