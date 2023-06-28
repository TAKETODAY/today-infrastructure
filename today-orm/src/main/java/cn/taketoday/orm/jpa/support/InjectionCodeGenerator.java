/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.jpa.support;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import cn.taketoday.aot.generate.AccessControl;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;

/**
 * Internal code generator that can inject a value into a field or single-arg
 * method.
 *
 * <p>Generates code in the form:
 * <pre class="code">{@code
 * instance.age = value;
 * }</pre> or <pre class="code">{@code
 * instance.setAge(value);
 * }</pre>
 *
 * <p>Will also generate reflection based injection and register hints if the
 * member is not visible.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class InjectionCodeGenerator {

  private final ClassName targetClassName;

  private final RuntimeHints hints;

  InjectionCodeGenerator(ClassName targetClassName, RuntimeHints hints) {
    Assert.notNull(targetClassName, "ClassName must not be null");
    Assert.notNull(hints, "RuntimeHints must not be null");
    this.targetClassName = targetClassName;
    this.hints = hints;
  }

  CodeBlock generateInjectionCode(Member member, String instanceVariable, CodeBlock resourceToInject) {
    if (member instanceof Field field) {
      return generateFieldInjectionCode(field, instanceVariable, resourceToInject);
    }
    if (member instanceof Method method) {
      return generateMethodInjectionCode(method, instanceVariable, resourceToInject);
    }
    throw new IllegalStateException("Unsupported member type " + member.getClass().getName());
  }

  private CodeBlock generateFieldInjectionCode(Field field, String instanceVariable,
          CodeBlock resourceToInject) {

    CodeBlock.Builder code = CodeBlock.builder();
    AccessControl accessControl = AccessControl.forMember(field);
    if (!accessControl.isAccessibleFrom(this.targetClassName)) {
      this.hints.reflection().registerField(field);
      code.addStatement("$T field = $T.findField($T.class, $S)", Field.class,
              ReflectionUtils.class, field.getDeclaringClass(), field.getName());
      code.addStatement("$T.makeAccessible($L)", ReflectionUtils.class, "field");
      code.addStatement("$T.setField($L, $L, $L)", ReflectionUtils.class,
              "field", instanceVariable, resourceToInject);
    }
    else {
      code.addStatement("$L.$L = $L", instanceVariable, field.getName(), resourceToInject);
    }
    return code.build();
  }

  private CodeBlock generateMethodInjectionCode(Method method, String instanceVariable,
          CodeBlock resourceToInject) {

    Assert.isTrue(method.getParameterCount() == 1,
            () -> "Method '" + method.getName() + "' must declare a single parameter");
    CodeBlock.Builder code = CodeBlock.builder();
    AccessControl accessControl = AccessControl.forMember(method);
    if (!accessControl.isAccessibleFrom(this.targetClassName)) {
      this.hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
      code.addStatement("$T method = $T.findMethod($T.class, $S, $T.class)",
              Method.class, ReflectionUtils.class, method.getDeclaringClass(),
              method.getName(), method.getParameterTypes()[0]);
      code.addStatement("$T.makeAccessible($L)", ReflectionUtils.class, "method");
      code.addStatement("$T.invokeMethod($L, $L, $L)", ReflectionUtils.class,
              "method", instanceVariable, resourceToInject);
    }
    else {
      code.addStatement("$L.$L($L)", instanceVariable, method.getName(), resourceToInject);
    }
    return code.build();
  }

}
