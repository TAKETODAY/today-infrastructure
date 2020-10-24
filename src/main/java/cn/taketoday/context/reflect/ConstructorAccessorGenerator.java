/*
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

package cn.taketoday.context.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.Assert;

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.cglib.core.CglibReflectUtils.getMethodInfo;

/**
 * @author TODAY
 * @date 2020/9/11 16:51
 */
public class ConstructorAccessorGenerator extends GeneratorSupport<ConstructorAccessor> implements ClassGenerator {

  private final Constructor<?> targetConstructor;
  private static final String[] interfaces = { "Lcn/taketoday/context/reflect/ConstructorAccessor;" };
  private static final MethodInfo newInstanceInfo;

  static {
    try {
      newInstanceInfo = getMethodInfo(ConstructorAccessor.class.getDeclaredMethod("newInstance", Object[].class));
    }
    catch (NoSuchMethodException | SecurityException e) {
      throw new ContextException(e);
    }
  }

  public ConstructorAccessorGenerator(Constructor<?> constructor) {
    this(constructor, constructor.getDeclaringClass());
  }

  public ConstructorAccessorGenerator(Constructor<?> constructor, Class<?> targetClass) {
    super(targetClass);
    Assert.notNull(constructor, "constructor must not be null");
    this.targetConstructor = constructor;
  }

  @Override
  protected int getArgsIndex() {
    return 1;
  }

  @Override
  public void generateClass(ClassVisitor v) {
    final ClassEmitter classEmitter = beginClass(v);

    final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, newInstanceInfo, ACC_PUBLIC | ACC_FINAL);
    codeEmitter.new_instance(Type.getType(targetClass));
    codeEmitter.dup();

    prepareParameters(codeEmitter, this.targetConstructor);

    final Type type = Type.getType(targetClass);
    Signature signature = new Signature(this.targetConstructor);
    codeEmitter.invoke_constructor(type, signature);

    codeEmitter.return_value();
    codeEmitter.end_method();
    classEmitter.endClass();
  }

  @Override
  protected ConstructorAccessor privateInstance() {
    return new ConstructorConstructorAccessor(targetConstructor);
  }

  @Override
  protected boolean isPrivate() {
    return Modifier.isPrivate(targetClass.getModifiers())
      || Modifier.isPrivate(targetConstructor.getModifiers());
  }

  @Override
  protected ClassGenerator getClassGenerator() {
    return this;
  }

  @Override
  protected String createClassName() {
    StringBuilder builder = new StringBuilder(targetClass.getName());
    builder.append('$').append("class"); // 使用 'class' 代替<init>
    buildClassNameSuffix(builder, targetConstructor);
    return builder.toString();
  }

  @Override
  public String[] getInterfaces() {
    return interfaces;
  }

}
