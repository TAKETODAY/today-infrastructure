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

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PRIVATE;
import static cn.taketoday.context.asm.Opcodes.ACC_SYNCHRONIZED;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Label;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;

/**
 * 
 * @author TODAY <br>
 *         2019-09-03 19:17
 */
class LazyLoaderGenerator implements CallbackGenerator {

    public static final LazyLoaderGenerator INSTANCE = new LazyLoaderGenerator();

    private static final Signature LOAD_OBJECT = TypeUtils.parseSignature("Object loadObject()");
    private static final Type LAZY_LOADER = TypeUtils.parseType(LazyLoader.class);

    public void generate(ClassEmitter ce, Context context, List<MethodInfo> methods) {

        final Set<Integer> indexes = new HashSet<>();

        for (final MethodInfo method : methods) {
            if (Modifier.isProtected(method.getModifiers())) {
                // ignore protected methods
            }
            else {
                int index = context.getIndex(method);
                indexes.add(new Integer(index));
                CodeEmitter e = context.beginMethod(ce, method);
                e.load_this();
                e.dup();
                e.invoke_virtual_this(loadMethod(index));
                e.checkcast(method.getClassInfo().getType());
                e.load_args();
                e.invoke(method);
                e.return_value();
                e.end_method();
            }
        }

        for (final int index : indexes) {

            final String delegate = "TODAY$LAZY_LOADER_" + index;

            ce.declare_field(ACC_PRIVATE, delegate, Constant.TYPE_OBJECT, null);

            CodeEmitter e = ce.beginMethod(ACC_PRIVATE | ACC_SYNCHRONIZED | ACC_FINAL, loadMethod(index));

            e.load_this();
            e.getfield(delegate);
            e.dup();
            Label end = e.make_label();
            e.ifnonnull(end);
            e.pop();
            e.load_this();
            context.emitCallback(e, index);
            e.invoke_interface(LAZY_LOADER, LOAD_OBJECT);
            e.dup_x1();
            e.putfield(delegate);
            e.mark(end);
            e.return_value();
            e.end_method();
        }
    }

    private Signature loadMethod(int index) {
        return new Signature("TODAY$LOAD_PRIVATE_" + index, Constant.TYPE_OBJECT, Constant.TYPES_EMPTY);
    }

    public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) {}
}
