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

package cn.taketoday.beans.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.ClassGenerator;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.core.Assert;
import cn.taketoday.core.reflect.GeneratorSupport;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.core.bytecode.Opcodes.ACC_FINAL;
import static cn.taketoday.core.bytecode.Opcodes.ACC_PUBLIC;

/**
 * @author TODAY 2020/9/11 16:51
 */
public class BeanInstantiatorGenerator
        extends GeneratorSupport<ConstructorAccessor> implements ClassGenerator {

  private static final String superType = "Lcn/taketoday/beans/support/ConstructorAccessor;";
  private static final MethodInfo newInstanceInfo = MethodInfo.from(
          ReflectionUtils.findMethod(ConstructorAccessor.class, "doInstantiate", Object[].class));

  private final Constructor<?> targetConstructor;

  public BeanInstantiatorGenerator(Constructor<?> constructor) {
    this(constructor, constructor.getDeclaringClass());
  }

  public BeanInstantiatorGenerator(Constructor<?> constructor, Class<?> targetClass) {
    super(targetClass);
    Assert.notNull(constructor, "constructor must not be null");
    this.targetConstructor = constructor;
  }

  @Override
  protected int getArgsIndex() {
    return 1;
  }

  /**
   * Fast call bean's {@link java.lang.reflect.Constructor Constructor}
   */
  @Override
  public void generateClass(ClassVisitor visitor) {
    final ClassEmitter classEmitter = beginClass(visitor);
//    final Method constructor = Method.fromConstructor(targetConstructor);
//    GeneratorAdapter generator = new GeneratorAdapter(ACC_PUBLIC | ACC_FINAL, constructor, null, null, visitor);
//    generator.loadThis();

    final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, newInstanceInfo, ACC_PUBLIC | ACC_FINAL);

    final Type type = Type.fromClass(targetClass);
    codeEmitter.newInstance(type);
    codeEmitter.dup();

    prepareParameters(codeEmitter, this.targetConstructor);

    MethodSignature signature = MethodSignature.from(this.targetConstructor);
    codeEmitter.invokeConstructor(type, signature);

    codeEmitter.returnValue();
    codeEmitter.end_method();
    classEmitter.endClass();
  }

  @Override
  protected ConstructorAccessor fallback(Exception exception) {
    LoggerFactory.getLogger(BeanInstantiatorGenerator.class)
            .warn("Cannot access a Constructor: [{}], using fallback instance", targetConstructor, exception);
    return super.fallback(exception);
  }

  @Override
  protected ConstructorAccessor fallbackInstance() {
    return BeanInstantiator.fromReflective(targetConstructor);
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
