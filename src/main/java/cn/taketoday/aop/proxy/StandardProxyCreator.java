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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.proxy;

import static cn.taketoday.context.Constant.SOURCE_FILE;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.context.asm.Type.array;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import cn.taketoday.aop.ProxyCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.AbstractClassGenerator;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.KeyFactory;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * @author TODAY <br>
 *         2019-09-07 10:44
 */
public class StandardProxyCreator implements ProxyCreator {

    private static final Logger log = LoggerFactory.getLogger(StandardProxyCreator.class);

    @Override
    public Object createProxy(TargetSource targetSource, BeanFactory beanFactory) {
        if (log.isDebugEnabled()) {
            log.debug("Creating Standard Proxy, target source is: [{}]", targetSource);
        }

        final Class<?> targetClass = targetSource.getTargetClass();
        final StandardProxyGenerator proxyGenerator = new StandardProxyGenerator(beanFactory);
        proxyGenerator.setTarget(targetSource.getTarget());
        proxyGenerator.setTargetClass(targetClass);

        return proxyGenerator.create();
    }

    // Aop standard proxy object generator
    // --------------------------------------------------------------

    private static final AopKey KEY_FACTORY = KeyFactory.create(AopKey.class, KeyFactory.CLASS_BY_NAME);

    interface AopKey {
        Object newInstance(Class<?> superClass);
    }

    public static class StandardProxyGenerator extends AbstractClassGenerator<Object> {

        private static final Source SOURCE = new Source("Aop");

        private Object target;
        private Class<?> targetClass;
        private Class<?>[] parameterTypes;
        private Constructor<?> targetConstructor;
        private final BeanFactory beanFactory;

        public StandardProxyGenerator(BeanFactory beanFactory) {
            super(SOURCE);
            this.beanFactory = beanFactory;
        }

        public void setTarget(Object target) {
            this.target = target;
            setTargetClass(target.getClass());
        }

        public void setTargetClass(Class<?> targetClass) {
            this.targetClass = ClassUtils.getUserClass(targetClass);
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return targetClass.getClassLoader();
        }

        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(targetClass);
        }

        public Object create() {
            setNamePrefix(targetClass.getName());
            Object key = KEY_FACTORY.newInstance(targetClass);
            return super.create(key);
        }

        @Override
        protected Object firstInstance(Class<Object> type) {

            if (ObjectUtils.isEmpty(parameterTypes)) {
                return ReflectUtils.newInstance(type, new Class[] { targetClass }, new Object[] { target });
            }
            Class<?>[] types = this.parameterTypes;
            final Class<?>[] copy = new Class[types.length + 1];
            System.arraycopy(types, 0, copy, 0, types.length);
            copy[types.length] = targetClass;

            final Object[] arg = ContextUtils.resolveParameter(targetConstructor, beanFactory);
            Object[] args = new Object[parameterTypes.length + 1];
            System.arraycopy(arg, 0, args, 0, arg.length);
            args[types.length] = target;

            return ReflectUtils.newInstance(type, copy, args);
        }

        @Override
        protected Object nextInstance(Object instance) {
            return instance;
        }

        @Override
        public void generateClass(ClassVisitor v) throws NoSuchMethodException {

            final ClassEmitter ce = new ClassEmitter(v);

            final Type targetType = TypeUtils.parseType(targetClass);

            ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(), targetType,
                          array(TypeUtils.getTypes(targetClass.getInterfaces())), SOURCE_FILE);

            ce.declare_field(Constant.ACC_PRIVATE | Constant.ACC_FINAL, "target", targetType, null);

            targetConstructor = ClassUtils.obtainConstructor(targetClass);

            // 父类构造器参数
            final Type[] types = TypeUtils.getTypes(parameterTypes = targetConstructor.getParameterTypes());
            {// 构造器

                final Type[] add = TypeUtils.add(types, targetType, true); // 子类构造器参数
                final Signature parseConstructor = TypeUtils.parseConstructor(add);

                final CodeEmitter code = ce.beginMethod(ACC_PUBLIC, parseConstructor);

                code.load_this();
                code.dup();

                final int length = types.length;
                if (length > 0) {
                    code.load_args(0, length);
                }
                code.super_invoke_constructor(TypeUtils.parseConstructor(types));

                code.load_arg(length);
                code.putfield("target");

                code.return_value();
                code.end_method();
            }

            for (Method method : targetClass.getDeclaredMethods()) {

                final int modifiers = method.getModifiers();

                if ((!Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers))
                    || Modifier.isFinal(modifiers)) {
                    continue;
                }

                final MethodInfo methodInfo = ReflectUtils.getMethodInfo(method);

                // TODO static method

                final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, modifiers);

                codeEmitter.load_this();
                codeEmitter.getfield(ce.getClassInfo().getType(), "target", targetType);

                codeEmitter.load_args();
                codeEmitter.invoke(methodInfo);

                codeEmitter.return_value();
                codeEmitter.end_method();
            }

            ce.endClass();
        }

    }
}
