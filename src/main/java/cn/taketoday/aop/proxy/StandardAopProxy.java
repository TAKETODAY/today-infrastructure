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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.std.DefaultProxyMethodGenerator;
import cn.taketoday.aop.proxy.std.GeneratorContext;
import cn.taketoday.aop.proxy.std.NoneProxyMethodGenerator;
import cn.taketoday.aop.proxy.std.ProxyMethodGenerator;
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
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;

import static cn.taketoday.context.Constant.AOP_SOURCE_FILE;
import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.JAVA_VERSION;

/**
 * Bytecode-based {@link AopProxy} implementation for the AOP framework.
 * <p>
 * Use CGLIB {@link AbstractClassGenerator} to generate a sub-class
 * </p>
 *
 * @author TODAY 2021/2/12 17:30
 * @since 3.0
 */
public class StandardAopProxy extends AbstractSubclassesAopProxy implements AopProxy {
  private static final Logger log = LoggerFactory.getLogger(StandardAopProxy.class);

  public StandardAopProxy(AdvisedSupport config) {
    super(config);
  }

  @Override
  public Object getProxy(ClassLoader classLoader, Function<Constructor<?>, Object[]> argsFunction) {
    if (log.isDebugEnabled()) {
      log.debug("Creating standard proxy: {}", config.getTargetSource());
    }
    return super.getProxy(classLoader, argsFunction);
  }

  @Override
  protected Object getProxyInternal(Class<?> proxySuperClass,
                                    ClassLoader classLoader,
                                    Function<Constructor<?>, Object[]> argsFunction) {

    final StandardProxyGenerator proxyGenerator = new StandardProxyGenerator(config, proxySuperClass, argsFunction);

    proxyGenerator.setClassLoader(classLoader);

    return proxyGenerator.create();
  }

  // Aop standard proxy object generator
  // --------------------------------------------------------------

  private static final AopKey KEY_FACTORY = KeyFactory.create(AopKey.class, KeyFactory.CLASS_BY_NAME);

  interface AopKey {
    Object newInstance(Class<?> superClass);
  }

  static class StandardProxyGenerator extends AbstractClassGenerator<Object> {

    static final int field_access = Constant.ACC_PRIVATE | Constant.ACC_FINAL;

    private static final Signature getTarget;

    private static final Type targetSourceType = Type.getType(TargetSource.class);
    private static final Type advisedSupportType = Type.getType(AdvisedSupport.class);
    private static final Type targetInvocationType = Type.getType(TargetInvocation.class);

    static {
      try {
        getTarget = new Signature(TargetInvocation.class.getDeclaredMethod("getTarget", String.class));
      }
      catch (NoSuchMethodException e) {
        throw new CodeGenerationException(e);
      }
    }

    /** super class's constructor' params */
    private Class<?>[] parameterTypes;
    private final AdvisedSupport config;
    private final TargetSource targetSource;
    /** class to be proxy-ed */
    private final Class<?> targetClass;
    /** super class's constructor */
    private Constructor<?> constructor;

    final Function<Constructor<?>, Object[]> constructorArgsFunction;

    public StandardProxyGenerator(AdvisedSupport config,
                                  Class<?> proxySuperClass,
                                  Function<Constructor<?>, Object[]> constructorArgsFunction) {
      super("Aop");
      this.config = config;
      this.targetClass = proxySuperClass;
      this.targetSource = config.getTargetSource();
      this.constructorArgsFunction = constructorArgsFunction;
    }

    public TargetSource getTargetSource() {
      return targetSource;
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
      setUseCache(false);
      setNamePrefix(targetClass.getName());
      Object key = KEY_FACTORY.newInstance(targetClass);
      return super.create(key);
    }

    public Class<?>[] getParameterTypes() {
      if (parameterTypes == null) {
        if (constructor == null) {
          constructor = ClassUtils.getSuitableConstructor(targetClass);
          if (constructor == null) {
            throw new CodeGenerationException("No suitable constructor found in class: [" + targetClass + "]");
          }
        }
        this.parameterTypes = constructor.getParameterTypes();
      }
      return parameterTypes;
    }

    @Override
    protected Object firstInstance(Class<Object> type) {
      final boolean targetSourceStatic = targetSource.isStatic();

      Class<?>[] types = getParameterTypes();
      final int superLength = types.length;
      // proxy constructor args types
      final Class<?>[] argTypes = new Class[superLength + (targetSourceStatic ? 3 : 2)];
      System.arraycopy(types, 0, argTypes, 0, superLength);

      int offset = 0;
      if (targetSourceStatic) {
        argTypes[superLength] = targetClass;
        offset = 1;
      }

      argTypes[superLength + offset] = TargetSource.class;
      argTypes[superLength + offset + 1] = AdvisedSupport.class;

      // proxy constructor arguments
      Object[] args = createArgs(argTypes);

      if (targetSourceStatic) {
        args[superLength] = targetSource.getTarget();
      }
      args[superLength + offset] = targetSource;
      args[superLength + offset + 1] = config;

      return CglibReflectUtils.newInstance(type, argTypes, args);
    }

    Object[] createArgs(Class<?>[] proxyConstructorArgTypes) {
      final Object[] ret = new Object[proxyConstructorArgTypes.length];
      if (constructorArgsFunction != null) {
        final Object[] args = constructorArgsFunction.apply(constructor);
        if (args != null) {
          System.arraycopy(args, 0, ret, 0, args.length);
        }
      }
      return ret;
    }

    @Override
    protected Object nextInstance(Object instance) {
      return instance;
    }

    static List<ProxyMethodGenerator> methodGenerators = new ArrayList<>();

    static {
      methodGenerators.add(new NoneProxyMethodGenerator());
      methodGenerators.add(new DefaultProxyMethodGenerator());
    }

    @Override
    public void generateClass(ClassVisitor v) {

      final ClassEmitter ce = new ClassEmitter(v);
      final Type targetType = TypeUtils.parseType(targetClass);
      final Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(config);
      final Type[] interfaces = TypeUtils.getTypes(proxiedInterfaces);

      ce.beginClass(JAVA_VERSION, ACC_PUBLIC | ACC_FINAL, getClassName(), targetType, interfaces, AOP_SOURCE_FILE);

      final boolean targetSourceStatic = targetSource.isStatic();
      if (targetSourceStatic) {
        ce.declare_field(field_access, ProxyMethodGenerator.FIELD_TARGET, targetType, null);
      }
      ce.declare_field(field_access, ProxyMethodGenerator.FIELD_CONFIG, advisedSupportType, null);
      ce.declare_field(field_access, ProxyMethodGenerator.FIELD_TARGET_SOURCE, targetSourceType, null);

      // generate constructor
      generateConstructor(ce, targetType, targetSourceStatic);

      final GeneratorContext context = new GeneratorContext(targetType, config, ce, targetClass);

      for (Method method : targetClass.getDeclaredMethods()) {
//      for (Method method : ReflectionUtils.getUniqueDeclaredMethods(targetClass)) {
        if (shouldGenerate(method)) {
          for (final ProxyMethodGenerator methodGenerator : methodGenerators) {
            if (methodGenerator.generate(method, context)) {
              break;
            }
          }
        }
      }

      // Advised
      if (!this.config.isOpaque()) {
        for (final Method method : Advised.class.getMethods()) {
          generateAdvisedMethod(ce, method);
        }
      }

      generateStaticBlock(ce, context);

      ce.endClass();
    }

    void generateStaticBlock(ClassEmitter ce, GeneratorContext context) {
      final List<String> fields = context.getFields();
      // static block
      if (!fields.isEmpty()) {
        final CodeEmitter staticBlock = ce.begin_static(false); // 静态代码块
        for (final String target : fields) {
          staticBlock.visitLdcInsn(target);
          staticBlock.invoke_static(targetInvocationType, getTarget);
          staticBlock.putfield(target);
        }
      }
    }

    boolean shouldGenerate(Method method) {
      if(method.getName().equals("finalize")) {
        return false;
      }
      final int modifiers = method.getModifiers();
      return !(Modifier.isStatic(modifiers)
              || Modifier.isFinal(modifiers)
              || Modifier.isNative(modifiers)
              || Modifier.isPrivate(modifiers));
    }

    /**
     * <pre class="code">
     *   public AopTest$PrinterBean$$AopByTODAY$$168c2842(PrinterBean var1, TargetSource var2, AdvisedSupport var3) {
     *     this.target = var1;
     *     this.config = var3;
     *     this.targetSource = var2;
     *   }
     * </pre>
     */
    protected void generateConstructor(final ClassEmitter ce, final Type targetType, boolean targetSourceStatic) {
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

      int offset = 0;
      if (targetSourceStatic) {
        code.load_this();
        code.load_arg(typesLength);
        code.putfield(ProxyMethodGenerator.FIELD_TARGET);

        offset = 1;
      }

      code.load_this();
      code.load_arg(typesLength + offset);
      code.putfield(ProxyMethodGenerator.FIELD_TARGET_SOURCE);

      code.load_this();
      code.load_arg(typesLength + offset + 1);
      code.putfield(ProxyMethodGenerator.FIELD_CONFIG);

      code.return_value();
      code.end_method();
    }

    /**
     * <pre class="code">
     *   boolean isProxyTargetClass() {
     *     return this.config.isProxyTargetClass();
     *   }
     * </pre>
     */
    public void generateAdvisedMethod(final ClassEmitter ce, final Method method) {
      MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method, ACC_PUBLIC | ACC_FINAL);
      final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, ACC_PUBLIC | ACC_FINAL);

      codeEmitter.load_this();

      codeEmitter.getfield(ProxyMethodGenerator.FIELD_CONFIG);

      codeEmitter.load_args();
      codeEmitter.invoke(methodInfo);
      codeEmitter.return_value();

      codeEmitter.unbox_or_zero(Type.getType(method.getReturnType()));
      codeEmitter.end_method();
    }

  }

}
