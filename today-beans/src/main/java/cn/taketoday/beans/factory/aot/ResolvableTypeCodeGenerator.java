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

package cn.taketoday.beans.factory.aot;

import java.util.Arrays;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.util.ClassUtils;

/**
 * Internal code generator used to support {@link ResolvableType}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
final class ResolvableTypeCodeGenerator {

  private ResolvableTypeCodeGenerator() {
  }

  public static CodeBlock generateCode(ResolvableType resolvableType) {
    return generateCode(resolvableType, false);
  }

  private static CodeBlock generateCode(ResolvableType resolvableType, boolean allowClassResult) {
    if (ResolvableType.NONE.equals(resolvableType)) {
      return CodeBlock.of("$T.NONE", ResolvableType.class);
    }
    Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
    if (resolvableType.hasGenerics() && !resolvableType.hasUnresolvableGenerics()) {
      return generateCodeWithGenerics(resolvableType, type);
    }
    if (allowClassResult) {
      return CodeBlock.of("$T.class", type);
    }
    return CodeBlock.of("$T.forClass($T.class)", ResolvableType.class, type);
  }

  private static CodeBlock generateCodeWithGenerics(ResolvableType target, Class<?> type) {
    ResolvableType[] generics = target.getGenerics();
    boolean hasNoNestedGenerics = Arrays.stream(generics).noneMatch(ResolvableType::hasGenerics);
    CodeBlock.Builder code = CodeBlock.builder();
    code.add("$T.forClassWithGenerics($T.class", ResolvableType.class, type);
    for (ResolvableType generic : generics) {
      code.add(", $L", generateCode(generic, hasNoNestedGenerics));
    }
    code.add(")");
    return code.build();
  }

}
