/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import infra.bytecode.ClassVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.EmitUtils;
import infra.bytecode.core.MethodInfo;
import infra.lang.Assert;
import infra.logging.LoggerFactory;
import infra.reflect.GeneratorSupport;
import infra.util.ReflectionUtils;

/**
 * {@link BeanInstantiator} bytecode Generator
 *
 * @author TODAY 2020/9/11 16:51
 */
class BeanInstantiatorGenerator extends GeneratorSupport<ConstructorAccessor> implements ClassGenerator {

  private static final String superType = "Linfra/beans/support/ConstructorAccessor;";

  private final Constructor<?> targetConstructor;

  public BeanInstantiatorGenerator(Constructor<?> constructor) {
    this(constructor, constructor.getDeclaringClass());
  }

  public BeanInstantiatorGenerator(Constructor<?> constructor, Class<?> targetClass) {
    super(targetClass);
    Assert.notNull(constructor, "constructor is required");
    this.targetConstructor = constructor;
  }

  @Override
  protected int getArgsIndex() {
    return 1;
  }

  /**
   * @since 4.0
   */
  @Override
  protected void generateConstructor(ClassEmitter ce) {
    MethodSignature signature = MethodSignature.forConstructor(Type.forClass(Constructor.class));
    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, signature);
    e.loadThis();
    e.loadArg(0);
    e.super_invoke_constructor(signature);
    e.returnValue();
    e.end_method();
  }

  /**
   * Fast call bean's {@link java.lang.reflect.Constructor Constructor}
   */
  @Override
  public void generateClass(ClassVisitor visitor) {
    ClassEmitter classEmitter = beginClass(visitor);
//    Method constructor = Method.fromConstructor(targetConstructor);
//    GeneratorAdapter generator = new GeneratorAdapter(ACC_PUBLIC | ACC_FINAL, constructor, null, null, visitor);
//    generator.loadThis();

    MethodInfo newInstanceInfo = MethodInfo.from(
            ReflectionUtils.getMethod(ConstructorAccessor.class, "doInstantiate", Object[].class));

    CodeEmitter codeEmitter = EmitUtils.beginMethod(
            classEmitter, newInstanceInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

    Type type = Type.forClass(targetClass);
    codeEmitter.newInstance(type);
    codeEmitter.dup();

    prepareParameters(codeEmitter, this.targetConstructor);

    MethodSignature signature = MethodSignature.from(this.targetConstructor);
    codeEmitter.invokeConstructor(type, signature);

    codeEmitter.returnValue();
    codeEmitter.end_method();
    classEmitter.endClass();
  }

  /**
   * @throws NoSuchMethodException handle in fallback {@link #fallback(Exception)}
   * @since 4.0
   */
  @Override
  protected ConstructorAccessor newInstance(Class<ConstructorAccessor> accessorClass) throws NoSuchMethodException {
    Constructor<ConstructorAccessor> constructor = accessorClass.getDeclaredConstructor(Constructor.class);
    return ReflectionUtils.invokeConstructor(constructor, new Object[] { this.targetConstructor });
  }

  @Override
  protected ConstructorAccessor fallback(Exception exception) {
    LoggerFactory.getLogger(BeanInstantiatorGenerator.class)
            .warn("Cannot access a Constructor: [{}], using fallback instance", targetConstructor, exception);
    return super.fallback(exception);
  }

  @Override
  protected ConstructorAccessor fallbackInstance() {
    return BeanInstantiator.forReflective(targetConstructor);
  }

  @Override
  protected boolean cannotAccess() {
    return Modifier.isPrivate(targetClass.getModifiers())
            || Modifier.isPrivate(targetConstructor.getModifiers());
  }

  @Override
  protected ClassGenerator getClassGenerator() {
    return this;
  }

  @Override
  protected Object cacheKey() {
    return targetConstructor;
  }

  @Override
  protected void appendClassName(StringBuilder builder) {
    builder.append('$').append("class"); // 使用 'class' 代替<init>
    buildClassNameSuffix(builder, targetConstructor);
  }

  @Override
  public String getSuperType() {
    return superType;
  }
}
