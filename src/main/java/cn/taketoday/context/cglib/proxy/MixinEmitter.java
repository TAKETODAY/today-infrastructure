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

import static cn.taketoday.context.Constant.SOURCE_FILE;
import static cn.taketoday.context.Constant.TYPE_OBJECT_ARRAY;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.context.asm.Type.array;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.MethodWrapper;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinEmitter.java,v 1.9 2006/08/27 21:04:37 herbyderby Exp $
 */
@SuppressWarnings("all")
class MixinEmitter extends ClassEmitter {

    private static final String FIELD_NAME = "TODAY$DELEGATES";
    private static final Type MIXIN = TypeUtils.parseType(Mixin.class);
    private static final Signature CSTRUCT_OBJECT_ARRAY = TypeUtils.parseConstructor("Object[]");

    private static final Signature NEW_INSTANCE = new Signature("newInstance", MIXIN, array(TYPE_OBJECT_ARRAY));

    public MixinEmitter(ClassVisitor v, String className, Class[] classes, int[] route) {
        super(v);

        beginClass(JAVA_VERSION, ACC_PUBLIC, className, MIXIN, TypeUtils.getTypes(getInterfaces(classes)), SOURCE_FILE);
        EmitUtils.nullConstructor(this);
        EmitUtils.factoryMethod(this, NEW_INSTANCE);

        declare_field(Constant.ACC_PRIVATE, FIELD_NAME, TYPE_OBJECT_ARRAY, null);

        CodeEmitter e = beginMethod(ACC_PUBLIC, CSTRUCT_OBJECT_ARRAY);
        e.load_this();
        e.super_invoke_constructor();
        e.load_this();
        e.load_arg(0);
        e.putfield(FIELD_NAME);
        e.return_value();
        e.end_method();

        Set unique = new HashSet();
        for (int i = 0; i < classes.length; i++) {
            Method[] methods = getMethods(classes[i]);
            for (int j = 0; j < methods.length; j++) {
                if (unique.add(MethodWrapper.create(methods[j]))) {
                    MethodInfo method = ReflectUtils.getMethodInfo(methods[j]);
                    int modifiers = ACC_PUBLIC;
                    if ((method.getModifiers() & Constant.ACC_VARARGS) == Constant.ACC_VARARGS) {
                        modifiers |= Constant.ACC_VARARGS;
                    }
                    e = EmitUtils.beginMethod(this, method, modifiers);
                    e.load_this();
                    e.getfield(FIELD_NAME);
                    e.aaload((route != null) ? route[i] : i);
                    e.checkcast(method.getClassInfo().getType());
                    e.load_args();
                    e.invoke(method);
                    e.return_value();
                    e.end_method();
                }
            }
        }

        endClass();
    }

    protected Class[] getInterfaces(Class[] classes) {
        return classes;
    }

    protected Method[] getMethods(Class type) {
        return type.getMethods();
    }
}
