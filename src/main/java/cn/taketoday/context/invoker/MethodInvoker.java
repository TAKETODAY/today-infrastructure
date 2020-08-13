/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.invoker;

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.cglib.core.ReflectUtils.getMethodInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.DefaultGeneratorStrategy;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY <br>
 *         2019-10-18 22:35
 */
public abstract class MethodInvoker implements Invoker {

    @Override
    public abstract Object invoke(Object obj, Object[] args);

    /**
     * Create a {@link MethodInvoker}
     *
     * @param method
     *            Target method to invoke
     * @return {@link MethodInvoker} sub object
     */
    public static MethodInvoker create(Method method) {
        return new MethodInvokerGenerator(method).create();
    }

    /**
     * Create a {@link MethodInvoker}
     *
     * @param beanClass
     *            Bean Class
     * @param name
     *            Target method to invoke
     * @param parameters
     *            Target parameters classes
     * @throws NoSuchMethodException
     *             Thrown when a particular method cannot be found.
     *
     * @return {@link MethodInvoker} sub object
     */
    public static MethodInvoker create(final Class<?> beanClass,
                                       final String name, final Class<?>... parameters) throws NoSuchMethodException {

        final Method targetMethod = beanClass.getDeclaredMethod(name, parameters);

        return new MethodInvokerGenerator(targetMethod, beanClass).create();
    }

    public static MethodInvoker generateConstructor(final Class<?> beanClass, final Class<?>... parameters) throws NoSuchMethodException {
        return create(beanClass, "<init>", parameters);
    }

    // MethodInvoker object generator
    // --------------------------------------------------------------

    public static class MethodInvokerGenerator implements ClassGenerator {

        private String className;
        private final Class<?> targetClass;
        private final Method targetMethod;

        private static final String superType = "Lcn/taketoday/context/invoker/MethodInvoker;";
        private static final String[] interfaces = { "Lcn/taketoday/context/invoker/Invoker;" };

        private static final MethodInfo invokeInfo;

        static {
            try {
                invokeInfo = getMethodInfo(MethodInvoker.class.getDeclaredMethod("invoke", Object.class, Object[].class));
            }
            catch (NoSuchMethodException | SecurityException e) {
                throw new ContextException(e);
            }
        }

        public MethodInvokerGenerator(Method method) {
            this.targetMethod = method;
            this.targetClass = method.getDeclaringClass();
        }

        public MethodInvokerGenerator(Method method, Class<?> targetClass) {
            this.targetMethod = method;
            this.targetClass = targetClass;
        }

        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(targetClass);
        }

        public MethodInvoker create() {
            final ClassLoader classLoader = targetClass.getClassLoader();
            try {

                final Class<?> loadClass = classLoader.loadClass(getClassName());
                return (MethodInvoker) ClassUtils.newInstance(loadClass);
            }
            catch (ClassNotFoundException e) {
                return ClassUtils.newInstance(generateClass(classLoader));
            }
        }

        protected Class<MethodInvoker> generateClass(final ClassLoader classLoader) {

            try {

                if (classLoader == null) {
                    throw new IllegalStateException("ClassLoader is null while trying to define class " + getClassName()
                            + ". It seems that the loader has been expired from a weak reference somehow.");
                }

                final byte[] b = DefaultGeneratorStrategy.INSTANCE.generate(this);
                return ReflectUtils.defineClass(getClassName(), b, classLoader, getProtectionDomain());
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Exception e) {
                throw new CodeGenerationException(e);
            }
        }

        @Override
        public void generateClass(ClassVisitor v) throws NoSuchMethodException {

            final Method targetMethod = this.targetMethod;

            final ClassEmitter ce = new ClassEmitter(v);
            ce.beginClass(ACC_PUBLIC | ACC_FINAL, getClassName().replace('.', '/'), superType, interfaces);

            EmitUtils.nullConstructor(ce);

            final int a_load = Constant.ALOAD;

            final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, invokeInfo, ACC_PUBLIC | ACC_FINAL);
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                codeEmitter.visitVarInsn(a_load, 1);
                codeEmitter.checkcast(Type.getType(targetClass));
                // codeEmitter.dup();
            }

            if (targetMethod.getParameterCount() != 0) {
                final Class<?>[] parameterTypes = targetMethod.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    codeEmitter.visitVarInsn(a_load, 2);
                    codeEmitter.aaload(i);

                    Class<?> parameterClass = parameterTypes[i];
                    final Type parameterType = Type.getType(parameterClass);
                    if (parameterClass.isPrimitive()) {
                        final Type boxedType = TypeUtils.getBoxedType(parameterType); // java.lang.Long ...

                        codeEmitter.checkcast(boxedType);
                        final String name = parameterClass.getName() + "Value";
                        final String descriptor = "()" + parameterType.getDescriptor();

                        codeEmitter.visitMethodInsn(Constant.INVOKEVIRTUAL, boxedType.getInternalName(), name, descriptor, false);
                    }
                    else {
                        codeEmitter.checkcast(parameterType);
                    }
                }
            }

            final MethodInfo methodInfo = getMethodInfo(targetMethod);
            codeEmitter.invoke(methodInfo);

            codeEmitter.box(Type.getType(targetMethod.getReturnType()));

            codeEmitter.return_value();
            codeEmitter.end_method();

            ce.endClass();
        }

        protected String getClassName() {
            if (className == null) {
                StringBuilder builder = new StringBuilder(targetClass.getName());
                builder.append('$').append(targetMethod.getName());

                if (targetMethod.getParameterCount() != 0) {

                    for (final Class<?> parameterType : targetMethod.getParameterTypes()) {
                        builder.append('$');
                        if (parameterType.isArray()) {
                            builder.append("A$");
                            final String simpleName = parameterType.getSimpleName();
                            builder.append(simpleName.substring(0, simpleName.length() - 2));
                        }
                        else {
                            builder.append(parameterType.getSimpleName());
                        }
                    }
                }
                this.className = builder.toString();
            }
            return className;
        }
    }

}
