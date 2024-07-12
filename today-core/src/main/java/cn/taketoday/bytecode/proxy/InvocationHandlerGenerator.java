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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.core.Block;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;

/**
 * @author TODAY <br>
 * 2019-09-03 18:53
 */
final class InvocationHandlerGenerator implements CallbackGenerator {

  public static final InvocationHandlerGenerator INSTANCE = new InvocationHandlerGenerator();

  private static final Type INVOCATION_HANDLER = Type.forClass(InvocationHandler.class);

  private static final Type UNDECLARED_THROWABLE_EXCEPTION = Type.forClass(UndeclaredThrowableException.class);
  private static final Type METHOD = Type.forInternalName("java/lang/reflect/Method");
  private static final MethodSignature INVOKE = MethodSignature.from("Object invoke(Object, java.lang.reflect.Method, Object[])");

  @Override
  public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {

    for (final MethodInfo method : methods) {
      final MethodSignature impl = context.getImplSignature(method);
      ce.declare_field(Opcodes.PRIVATE_FINAL_STATIC, impl.getName(), METHOD, null);

      final CodeEmitter e = context.beginMethod(ce, method);
      final Block handler = e.begin_block();

      context.emitCallback(e, context.getIndex(method));
      e.loadThis();
      e.getField(impl.getName());
      e.loadArgArray();
      e.invokeInterface(INVOCATION_HANDLER, INVOKE);
      e.unbox(method.getSignature().getReturnType());
      e.returnValue();
      handler.end();
      EmitUtils.wrapUndeclaredThrowable(e, handler, method.getExceptionTypes(), UNDECLARED_THROWABLE_EXCEPTION);
      e.end_method();
    }
  }

  @Override
  public void generateStatic(final CodeEmitter e, final Context context, final List<MethodInfo> methods) {

    for (final MethodInfo method : methods) {

      EmitUtils.loadMethod(e, method);
      e.putField(context.getImplSignature(method).getName());
    }
  }
}
