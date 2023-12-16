/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.testfixture.beans.factory.aot;

import cn.taketoday.aot.generate.ValueCodeGenerator;
import cn.taketoday.aot.generate.ValueCodeGenerator.Delegate;
import cn.taketoday.javapoet.CodeBlock;

/**
 * A custom value with its code generator {@link Delegate} implementation.
 *
 * @author Stephane Nicoll
 */
public record CustomPropertyValue(String value) {

  public static class ValueCodeGeneratorDelegate implements Delegate {
    @Override
    public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
      if (value instanceof CustomPropertyValue data) {
        return CodeBlock.of("new $T($S)", CustomPropertyValue.class, data.value);
      }
      return null;
    }
  }

}
