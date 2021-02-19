/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.AbstractClassGenerator;
import cn.taketoday.context.cglib.core.CglibReflectUtils;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.KeyFactory;
import cn.taketoday.context.cglib.core.Local;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.reflect.GeneratorSupport;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.Constant.SOURCE_FILE;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.JAVA_VERSION;

/**
 * @author TODAY 2021/2/12 17:35
 */
public class StandardProxyCreator {
  private static final Logger log = LoggerFactory.getLogger(StandardProxyCreator.class);

  public static Object createProxy(AdvisedSupport config, ClassLoader classLoader, BeanFactory beanFactory) {
    if (log.isDebugEnabled()) {
      log.debug("Creating Standard Proxy {}", config.getTargetSource());
    }

    final StandardProxyGenerator proxyGenerator = new StandardProxyGenerator(beanFactory, config);

    proxyGenerator.setTargetSource(config.getTargetSource());
    proxyGenerator.setClassLoader(classLoader);

    return proxyGenerator.create();
  }

  // Aop standard proxy object generator
  // --------------------------------------------------------------

  private static final StandardProxyCreator.AopKey KEY_FACTORY
          = KeyFactory.create(StandardProxyCreator.AopKey.class, KeyFactory.CLASS_BY_NAME);

  interface AopKey {
    Object newInstance(Class<?> superClass);
  }

  static class Bean {

    void test() {

    }

    void none() {
    }

    void noneStatic() {

    }

    int testReturn() {
      return 100;
    }
  }

  public static class Bean$$AopByTODAY$$b059af0e extends Bean implements StandardProxy {

    private final Bean target;
    private final TargetSource targetSource;
    private static final StandardMethodInvocation.Target testMR2B = InvocationRegistry.getTarget("testMR2B");
    private static final StandardMethodInvocation.Target testReturnlQaU = InvocationRegistry.getTarget("testReturnlQaU");

    public Bean$$AopByTODAY$$b059af0e(Bean target, TargetSource targetSource) {
      this.target = target;
      this.targetSource = targetSource;
    }

    @Override
    void test() {
      try {
//        StandardProxyInvoker.proceed(this, targetSource, testMR2B, Constant.EMPTY_OBJECT_ARRAY);
      }
      catch (Throwable throwable) {
        throwable.printStackTrace();
      }
    }

    @Override
    void none() {
      this.target.none();
    }

    @Override
    void noneStatic() {
      ((Bean) this.targetSource.getTarget()).noneStatic();
    }

    @Override
    int testReturn() {
      Object ret = null;
      try {
//        ret = StandardProxyInvoker.proceed(this, targetSource, testReturnlQaU, Constant.EMPTY_OBJECT_ARRAY);
      }
      catch (Throwable throwable) {
        throwable.printStackTrace();
      }
      return GeneratorSupport.convert((Integer) ret);
    }

  }

  /**
   * 如果是static直接获取bean调用方法，
   */
  public static class StandardProxyGenerator extends AbstractClassGenerator<Object> {

    private static final Signature proceed;
    private static final Signature getTarget;
    private static final Signature dynamicProceed;
    private static final Signature staticExposeProceed;
    private static final Signature dynamicExposeProceed;
    private static final Signature targetSourceGetTarget;

    private static final Type stdProxy = Type.getType(StandardProxy.class);
    private static final Type targetSourceType = Type.getType(TargetSource.class);
    private static final Type advisedSupportType = Type.getType(AdvisedSupport.class);
    private static final Type stdProxyInvoker = Type.getType(StandardProxyInvoker.class);
    private static final Type invocationRegistryType = Type.getType(InvocationRegistry.class);
    private static final Type targetInvocationType = Type.getType(StandardMethodInvocation.Target.class);

    static {
      try {
        proceed = new Signature(StandardProxyInvoker.class.getMethod("proceed",
                                                                     Object.class,
                                                                     StandardMethodInvocation.Target.class,
                                                                     Object[].class));
        dynamicProceed = new Signature(StandardProxyInvoker.class
                                               .getMethod("dynamicProceed",
                                                          TargetSource.class,
                                                          StandardMethodInvocation.Target.class,
                                                          Object[].class));
        dynamicExposeProceed = new Signature(StandardProxyInvoker.class.getMethod("dynamicExposeProceed", Object.class, TargetSource.class,
                                                                                  StandardMethodInvocation.Target.class, Object[].class));
        staticExposeProceed = new Signature(StandardProxyInvoker.class.getMethod("staticExposeProceed",
                                                                                 Object.class,
                                                                                 Object.class,
                                                                                 StandardMethodInvocation.Target.class,
                                                                                 Object[].class));

        targetSourceGetTarget = new Signature(TargetSource.class.getDeclaredMethod("getTarget"));
        getTarget = new Signature(InvocationRegistry.class.getDeclaredMethod("getTarget", String.class));
      }
      catch (NoSuchMethodException e) {
        throw new CodeGenerationException(e);
      }
    }

    private Class<?> targetClass;
    private Class<?>[] parameterTypes;
    private TargetSource targetSource;
    private Constructor<?> targetConstructor;
    private final AdvisedSupport config;
    private final BeanFactory beanFactory;

    public StandardProxyGenerator(BeanFactory beanFactory, AdvisedSupport config) {
      super("Aop");
      this.config = config;
      this.beanFactory = beanFactory;
    }

    public void setTargetClass(Class<?> targetClass) {
      this.targetClass = ClassUtils.getUserClass(targetClass);
    }

    public TargetSource getTargetSource() {
      return targetSource;
    }

    public void setTargetSource(final TargetSource targetSource) {
      this.targetSource = targetSource;
      setTargetClass(targetSource.getTargetClass());
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
      return targetClass.getClassLoader();
    }

    @Override
    protected ProtectionDomain getProtectionDomain() {
      return CglibReflectUtils.getProtectionDomain(targetClass);
    }

    public Object create() {
      setNamePrefix(targetClass.getName());
      Object key = KEY_FACTORY.newInstance(targetClass);
      return super.create(key);
    }

    public Class<?>[] getParameterTypes() {
      if (parameterTypes == null) {
        if (targetConstructor == null) {
          targetConstructor = ClassUtils.getSuitableConstructor(targetClass);
          if (targetConstructor == null) {
            throw new CodeGenerationException("No suitable constructor found in class :[" + targetClass + "]");
          }
        }
        this.parameterTypes = targetConstructor.getParameterTypes();
      }
      return parameterTypes;
    }

    @Override
    protected Object firstInstance(Class<Object> type) {
      final boolean targetSourceStatic = targetSource.isStatic();

      Class<?>[] types = getParameterTypes();
      final int superLength = types.length;
      final Class<?>[] copy = new Class[superLength + (targetSourceStatic ? 3 : 2)];
      System.arraycopy(types, 0, copy, 0, superLength);

      int offset = 1;
      if (targetSourceStatic) {
        copy[superLength] = targetClass;
      }
      else {
        offset = 0;
      }

      copy[superLength + offset] = TargetSource.class;
      copy[superLength + offset + 1] = AdvisedSupport.class;

      Object[] args = createArgs(copy);

      if (targetSourceStatic) {
        args[superLength] = targetSource.getTarget();
      }
      args[superLength + offset] = targetSource;
      args[superLength + offset + 1] = config;

      return CglibReflectUtils.newInstance(type, copy, args);
    }

    private Object[] createArgs(Class<?>[] parameterTypes) {
      final Object[] arg = ContextUtils.resolveParameter(targetConstructor, beanFactory);
      if (arg == null) {
        return new Object[parameterTypes.length];
      }
      Object[] args = new Object[parameterTypes.length];
      System.arraycopy(arg, 0, args, 0, arg.length);
      return args;
    }

    @Override
    protected Object nextInstance(Object instance) {
      return instance;
    }

    @Override
    public void generateClass(ClassVisitor v) {

      final ClassEmitter ce = new ClassEmitter(v);
      final Type targetType = TypeUtils.parseType(targetClass);

      final Type[] interfaces = TypeUtils.add(TypeUtils.getTypes(targetClass.getInterfaces()), stdProxy);
      ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(), targetType, interfaces, SOURCE_FILE);

      final boolean targetSourceStatic = targetSource.isStatic();
      if (targetSourceStatic) {
        ce.declare_field(Constant.ACC_PRIVATE | Constant.ACC_FINAL, "target", targetType, null);
      }
      ce.declare_field(Constant.ACC_PRIVATE | Constant.ACC_FINAL, "config", advisedSupportType, null);
      ce.declare_field(Constant.ACC_PRIVATE | Constant.ACC_FINAL, "targetSource", targetSourceType, null);

      // 父类构造器参数
      constructor(ce, targetType, targetSourceStatic);

      List<String> fields = new ArrayList<>();
      for (Method method : targetClass.getDeclaredMethods()) {

        final int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)
                || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)) { // TODO private
          continue;
        }

        final MethodInterceptor[] interceptors = config.getInterceptors(method, targetClass);
        if (ObjectUtils.isEmpty(interceptors)) {
          if (targetSourceStatic) {
            invokeStaticTarget(ce, targetType, method);
          }
          else {
            invokeTargetFromTargetSource(ce, targetType, method);
          }
          continue;
        }

        final String targetInvField = putTargetInv(method, fields);
        fields.add(targetInvField);

        ce.declare_field(getStaticAccess(), targetInvField, targetInvocationType, null);

        MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method);
        // 当前方法
        final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, modifiers);

        if (config.isExposeProxy()) {
          // 加载代理对象
          codeEmitter.load_this(); // proxy
        }

        codeEmitter.load_this();
        if (targetSourceStatic) {
          // 准备参数
          // Object target, StandardMethodInvocation.Target targetInv, Object[] args
          codeEmitter.getfield("target");
          codeEmitter.getfield(targetInvField);
          prepareArgs(method, codeEmitter);

          if (config.isExposeProxy()) {
            codeEmitter.invoke_static(stdProxyInvoker, staticExposeProceed);
          }
          else {
            codeEmitter.invoke_static(stdProxyInvoker, proceed);
          }
        }
        else {
          //TargetSource targetSource, StandardMethodInvocation.Target targetInv, Object[] args
          codeEmitter.getfield("targetSource");
          codeEmitter.getfield(targetInvField);
          prepareArgs(method, codeEmitter);

          if (config.isExposeProxy()) {
            codeEmitter.invoke_static(stdProxyInvoker, dynamicExposeProceed);
          }
          else {
            codeEmitter.invoke_static(stdProxyInvoker, dynamicProceed);
          }
        }

        Local returnLocal = null;
        if (method.getReturnType() != void.class) {
          returnLocal = codeEmitter.make_local();
          codeEmitter.store_local(returnLocal);
        }

        if (returnLocal != null) {
          codeEmitter.load_local(returnLocal);
          codeEmitter.unbox_or_zero(Type.getType(method.getReturnType()));
        }

        codeEmitter.return_value();
        codeEmitter.end_method();
      }

      if (!fields.isEmpty()) {
        final CodeEmitter staticBlock = ce.begin_static(false); // 静态代码块
        for (final String target : fields) {
          staticBlock.visitLdcInsn(target);
          staticBlock.invoke_static(invocationRegistryType, getTarget);
          staticBlock.putfield(target);
        }
      }

      ce.endClass();
    }

    protected void prepareArgs(Method method, CodeEmitter codeEmitter) {
      if (method.getParameterCount() == 0) {
        codeEmitter.getstatic(Type.getType(Constant.class), "EMPTY_OBJECT_ARRAY", Constant.TYPE_OBJECT_ARRAY);
      }
      else {
        codeEmitter.create_arg_array(); // args
      }
    }

    /**
     * @param method
     *         current method
     * @param fields
     *         Target keys in {@link #targetClass}
     *
     * @return Target key
     */
    protected String putTargetInv(final Method method, final List<String> fields) {
      final String field = method.getName() + StringUtils.getRandomString(4);
      if (fields.contains(field)) {
        return putTargetInv(method, fields);
      }
      final StandardMethodInvocation.Target target = InvocationRegistry.getTarget(field);
      if (target != null) {
        return putTargetInv(method, fields);
      }
      InvocationRegistry.putTarget(field, getTargetMethodInvocation(method));
      return field;
    }

    protected int getStaticAccess() {
      return Constant.PRIVATE_FINAL_STATIC;
    }

    protected StandardMethodInvocation.Target getTargetMethodInvocation(final Method method) {
      final MethodInterceptor[] interceptors = config.getInterceptors(method, targetSource.getTargetClass());
      // OrderUtils.reversedSort(interceptors); //TODO
      return new StandardMethodInvocation.Target(method, interceptors);
    }

    protected void constructor(final ClassEmitter ce, final Type targetType, boolean targetSourceStatic) {
      // 构造器
      Type[] superTypes = TypeUtils.getTypes(getParameterTypes());
      Type[] types = superTypes.clone();
      final int typesLength = types.length;

      // 构建参数,额外的参数添加在最后
      if (targetSourceStatic) {
        // 直接添加对象
        types = TypeUtils.add(types, targetType, true); // 子类构造器参数
      }
      types = TypeUtils.add(types, targetSourceType, advisedSupportType);

      final Signature parseConstructor = TypeUtils.parseConstructor(types);

      final CodeEmitter code = ce.beginMethod(ACC_PUBLIC, parseConstructor);

      code.load_this();
      code.dup();

      // 调用父类构造器
      if (typesLength > 0) {
        code.load_args(0, typesLength);
      }
      code.super_invoke_constructor(TypeUtils.parseConstructor(superTypes));
      // 赋值

      int offset = 1;
      if (targetSourceStatic) {
        code.load_this();

        code.load_arg(typesLength);
        code.putfield("target");
      }
      else {
        offset = 0;
      }

      code.load_this();
      code.load_arg(typesLength + offset);
      code.putfield("targetSource");

      code.load_this();
      code.load_arg(typesLength + offset + 1);
      code.putfield("config");

      code.return_value();
      code.end_method();
    }

    protected void invokeStaticTarget(final ClassEmitter ce, final Type targetType, final Method method) {
      MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method);
      final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, method.getModifiers());

      codeEmitter.load_this();

      codeEmitter.getfield("target");

      codeEmitter.load_args();
      codeEmitter.invoke(methodInfo);
      codeEmitter.return_value();

      codeEmitter.unbox_or_zero(Type.getType(method.getReturnType()));
      codeEmitter.end_method();
    }

    /**
     * <pre>
     *   @Override
     *   void noneStatic() {
     *     ((Bean) this.targetSource.getTarget()).noneStatic();
     *   }
     * </pre>
     */
    protected void invokeTargetFromTargetSource(final ClassEmitter ce, final Type targetType, final Method method) {
      MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method);
      final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, method.getModifiers());

      // this.targetSource.getTarget()

      codeEmitter.load_this();
      codeEmitter.getfield( "targetSource");
      codeEmitter.invoke_interface(targetSourceType, targetSourceGetTarget);

      // cast

      codeEmitter.checkcast(targetType);
      codeEmitter.load_args();
      codeEmitter.invoke(methodInfo);
      codeEmitter.return_value();

      codeEmitter.unbox_or_zero(Type.getType(method.getReturnType()));
      codeEmitter.end_method();
    }

  }

}

