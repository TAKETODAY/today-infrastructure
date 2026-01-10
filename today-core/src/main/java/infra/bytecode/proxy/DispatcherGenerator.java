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
