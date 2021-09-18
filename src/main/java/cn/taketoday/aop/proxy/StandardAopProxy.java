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

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.std.DefaultProxyMethodGenerator;
import cn.taketoday.aop.proxy.std.GeneratorContext;
import cn.taketoday.aop.proxy.std.NoneProxyMethodGenerator;
import cn.taketoday.aop.proxy.std.ProxyMethodGenerator;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.CodeGenerationException;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.KeyFactory;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.core.Constant;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

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
  protected Object getProxyInternal(
          Class<?> proxySuperClass, ClassLoader classLoader) {
    if (log.isDebugEnabled()) {
      log.debug("Creating standard proxy: {}", config.getTargetSource());
    }
    final StandardProxyGenerator proxyGenerator = new StandardProxyGenerator(config, proxySuperClass);

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

    static final int field_access = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;

    private static final MethodSignature getTarget;

    private static final Type targetSourceType = Type.fromClass(TargetSource.class);
    private static final Type advisedSupportType = Type.fromClass(AdvisedSupport.class);
    private static final Type targetInvocationType = Type.fromClass(TargetInvocation.class);

    static {
      try {
        getTarget = MethodSignature.from(TargetInvocation.class.getDeclaredMethod("getTarget", String.class));
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

    public StandardProxyGenerator(AdvisedSupport config,
                                  Class<?> proxySuperClass) {
      super("Aop");
      this.config = config;
      this.targetClass = proxySuperClass;
      this.targetSource = config.getTargetSource();
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
          constructor = BeanUtils.getConstructor(targetClass);
          if (constructor == null) {
            throw new CodeGenerationException("No suitable constructor found in class: [" + targetClass + "]");
          }
        }
        this.parameterTypes = constructor.getParameterTypes();
      }
      return parameterTypes;
    }

    @Override
    protected Object firstInstance(Class<Object> type) throws Exception {
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

      return ReflectionUtils.newInstance(type, argTypes, args);
    }

    Object[] createArgs(Class<?>[] proxyConstructorArgTypes) {
      // TODO 调试 构造器问题
      final Object[] ret = new Object[proxyConstructorArgTypes.length];
//      if (constructorArgsFunction != null) {
//        final Object[] args = constructorArgsFunction.apply(constructor);
//        if (args != null) {
//          System.arraycopy(args, 0, ret, 0, args.length);
//        }
//      }
      return ret;
    }

    @Override
    protected Object nextInstance(Object instance) {
      return instance;
    }

    static List<ProxyMethodGenerator> methodGenerators = new ArrayList<>(2);

    static {
      methodGenerators.add(new NoneProxyMethodGenerator());
      methodGenerators.add(new DefaultProxyMethodGenerator());
    }

    @Override
    public void generateClass(ClassVisitor v) {

      final ClassEmitter ce = new ClassEmitter(v);
      final Type targetType = Type.fromClass(targetClass);
      final Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(config);
      final Type[] interfaces = TypeUtils.getTypes(proxiedInterfaces);

      ce.beginClass(Opcodes.JAVA_VERSION,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                    getClassName(), targetType, interfaces, Constant.AOP_SOURCE_FILE);

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

    protected void generateStaticBlock(ClassEmitter ce, GeneratorContext context) {
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

    protected boolean shouldGenerate(Method method) {
      if (method.getName().equals("finalize")) {
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

      final MethodSignature parseConstructor = MethodSignature.forConstructor(types);

      final CodeEmitter code = ce.beginMethod(Opcodes.ACC_PUBLIC, parseConstructor);

      code.load_this();
      code.dup();

      // 调用父类构造器
      if (typesLength > 0) {
        code.load_args(0, typesLength);
      }
      code.super_invoke_constructor(MethodSignature.forConstructor(superTypes));
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
      MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
      final CodeEmitter codeEmitter = EmitUtils.beginMethod(ce, methodInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

      codeEmitter.load_this();

      codeEmitter.getfield(ProxyMethodGenerator.FIELD_CONFIG);

      codeEmitter.load_args();
      codeEmitter.invoke(methodInfo);
      codeEmitter.return_value();

      codeEmitter.unbox_or_zero(Type.fromClass(method.getReturnType()));
      codeEmitter.end_method();
    }

  }

}
