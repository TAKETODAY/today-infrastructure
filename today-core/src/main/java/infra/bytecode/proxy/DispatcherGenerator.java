/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode.proxy;

import java.lang.reflect.Modifier;
import java.util.List;

import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.MethodInfo;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2018-11-08 15:09
 */
final class DispatcherGenerator implements CallbackGenerator {

  public static final DispatcherGenerator INSTANCE = new DispatcherGenerator(false);

  public static final DispatcherGenerator PROXY_REF_INSTANCE = new DispatcherGenerator(true);

  private final boolean proxyRef;

  private DispatcherGenerator(boolean proxyRef) {
    this.proxyRef = proxyRef;
  }

  @Override
  public void generate(ClassEmitter ce, Context context, List<MethodInfo> methods) {
    for (final MethodInfo method : methods) {
      if (!Modifier.isProtected(method.getModifiers())) {
        final CodeEmitter e = context.beginMethod(ce, method);
        context.emitCallback(e, context.getIndex(method));
        if (proxyRef) {
          e.loadThis();
          e.invokeInterface(Type.forClass(ProxyRefDispatcher.class), MethodSignature.from("Object loadObject(Object)"));
        }
        else {
          e.invokeInterface(Type.forClass(Dispatcher.class), MethodSignature.from("Object loadObject()"));
        }
        e.checkCast(method.getClassInfo().getType());
        e.loadArgs();
        e.invoke(method);
        e.returnValue();
        e.end_method();
      }
    }
  }

  @Override
  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) throws Exception {

  }

}
