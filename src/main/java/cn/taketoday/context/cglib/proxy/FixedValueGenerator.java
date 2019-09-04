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
package cn.taketoday.context.cglib.proxy;

import java.util.List;

import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;

/**
 * 
 * @author TODAY <br>
 *         2019-09-03 19:15
 */
class FixedValueGenerator implements CallbackGenerator {

    public static final FixedValueGenerator INSTANCE = new FixedValueGenerator();

    private static final Type FIXED_VALUE = TypeUtils.parseType(FixedValue.class);
    private static final Signature LOAD_OBJECT = TypeUtils.parseSignature("Object loadObject()");

    public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {

        for (final MethodInfo method : methods) {

            final CodeEmitter e = context.beginMethod(ce, method);
            context.emitCallback(e, context.getIndex(method));

            e.invoke_interface(FIXED_VALUE, LOAD_OBJECT);
            e.unbox_or_zero(e.getReturnType());
            e.return_value();
            e.end_method();
        }
    }

    public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) {

    }
}
