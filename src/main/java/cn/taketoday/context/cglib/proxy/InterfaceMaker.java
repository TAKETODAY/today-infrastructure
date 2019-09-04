/*
 * Copyright 2004 The Apache Software Foundation
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.AbstractClassGenerator;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.Signature;

/**
 * Generates new interfaces at runtime. By passing a generated interface to the
 * Enhancer's list of interfaces to implement, you can make your enhanced
 * classes handle an arbitrary set of method signatures.
 * 
 * @author Chris Nokleberg
 * @author TODAY
 */
public class InterfaceMaker extends AbstractClassGenerator<Object> {

    private static final Source SOURCE = new Source(InterfaceMaker.class.getSimpleName());

    private final Map<Signature, Type[]> signatures = new HashMap<>();

    /**
     * Create a new <code>InterfaceMaker</code>. A new <code>InterfaceMaker</code>
     * object should be used for each generated interface, and should not be shared
     * across threads.
     */
    public InterfaceMaker() {
        super(SOURCE);
    }

    /**
     * Add a method signature to the interface.
     * 
     * @param sig
     *            the method signature to add to the interface
     * @param exceptions
     *            an array of exception types to declare for the method
     */
    public void add(Signature sig, Type[] exceptions) {
        signatures.put(sig, exceptions);
    }

    /**
     * Add a method signature to the interface. The method modifiers are ignored,
     * since interface methods are by definition abstract and public.
     * 
     * @param method
     *            the method to add to the interface
     */
    public void add(Method method) {
        add(ReflectUtils.getSignature(method), ReflectUtils.getExceptionTypes(method));
    }

    /**
     * Add all the public methods in the specified class. Methods from superclasses
     * are included, except for methods declared in the base Object class (e.g.
     * <code>getClass</code>, <code>equals</code>, <code>hashCode</code>).
     * 
     * @param class
     *            the class containing the methods to add to the interface
     */
    public void add(Class<?> clazz) {

        for (final Method m : clazz.getMethods()) {
            if (!m.getDeclaringClass().getName().equals("java.lang.Object")) {
                add(m);
            }
        }
    }

    /**
     * Create an interface using the current set of method signatures.
     */
    public Class<?> create() {
        setUseCache(false);
        return (Class<?>) super.create(this);
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
        return null;
    }

    @Override
    protected Object firstInstance(Class<Object> type) throws Exception {
        return type;
    }

    @Override
    protected Object nextInstance(Object instance) {
        throw new IllegalStateException("InterfaceMaker does not cache");
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {
        ClassEmitter ce = new ClassEmitter(v);
        ce.beginClass(//
                Constant.JAVA_VERSION, //
                Constant.ACC_PUBLIC | Constant.ACC_INTERFACE | Constant.ACC_ABSTRACT, //
                getClassName(), //
                null, //
                null, //
                Constant.SOURCE_FILE//
        );

        final int access = Constant.ACC_PUBLIC | Constant.ACC_ABSTRACT;
        for (final Map.Entry<Signature, Type[]> entry : signatures.entrySet()) {
            ce.beginMethod(access, entry.getKey(), entry.getValue()).end_method();
        }

        ce.endClass();
    }

}
