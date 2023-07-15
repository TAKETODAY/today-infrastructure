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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel.ast;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.core.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 */
public class FormatHelperTests {

  @Test
  public void formatMethodWithSingleArgumentForMessage() {
    String message = FormatHelper.formatMethodForMessage("foo", Arrays.asList(TypeDescriptor.forObject("a string")));
    assertThat(message).isEqualTo("foo(java.lang.String)");
  }

  @Test
  public void formatMethodWithMultipleArgumentsForMessage() {
    String message = FormatHelper.formatMethodForMessage("foo", Arrays.asList(TypeDescriptor.forObject("a string"), TypeDescriptor.forObject(Integer.valueOf(5))));
    assertThat(message).isEqualTo("foo(java.lang.String,java.lang.Integer)");
  }

}
