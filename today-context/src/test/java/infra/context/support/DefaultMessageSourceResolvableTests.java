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

package infra.context.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMessageSourceResolvableTests {

  @Test
  void singleCodeConstructor() {
    var resolvable = new DefaultMessageSourceResolvable("test");
    assertThat(resolvable.getCodes()).containsExactly("test");
    assertThat(resolvable.getArguments()).isNull();
    assertThat(resolvable.getDefaultMessage()).isNull();
    assertThat(resolvable.getCode()).isEqualTo("test");
  }

  @Test
  void multipleCodesConstructor() {
    String[] codes = { "code1", "code2" };
    var resolvable = new DefaultMessageSourceResolvable(codes);
    assertThat(resolvable.getCodes()).containsExactly("code1", "code2");
    assertThat(resolvable.getArguments()).isNull();
    assertThat(resolvable.getDefaultMessage()).isNull();
    assertThat(resolvable.getCode()).isEqualTo("code2");
  }

  @Test
  void codesAndDefaultMessageConstructor() {
    String[] codes = { "code1", "code2" };
    var resolvable = new DefaultMessageSourceResolvable(codes, "default");
    assertThat(resolvable.getCodes()).containsExactly("code1", "code2");
    assertThat(resolvable.getArguments()).isNull();
    assertThat(resolvable.getDefaultMessage()).isEqualTo("default");
  }

  @Test
  void codesAndArgumentsConstructor() {
    String[] codes = { "code1" };
    Object[] args = { "arg1", 2 };
    var resolvable = new DefaultMessageSourceResolvable(codes, args);
    assertThat(resolvable.getCodes()).containsExactly("code1");
    assertThat(resolvable.getArguments()).containsExactly("arg1", 2);
    assertThat(resolvable.getDefaultMessage()).isNull();
  }

  @Test
  void fullConstructor() {
    String[] codes = { "code1", "code2" };
    Object[] args = { "arg1", 2 };
    var resolvable = new DefaultMessageSourceResolvable(codes, args, "default");
    assertThat(resolvable.getCodes()).containsExactly("code1", "code2");
    assertThat(resolvable.getArguments()).containsExactly("arg1", 2);
    assertThat(resolvable.getDefaultMessage()).isEqualTo("default");
  }

  @Test
  void copyConstructor() {
    String[] codes = { "code1" };
    Object[] args = { "arg1" };
    var original = new DefaultMessageSourceResolvable(codes, args, "default");
    var copy = new DefaultMessageSourceResolvable(original);

    assertThat(copy.getCodes()).containsExactly("code1");
    assertThat(copy.getArguments()).containsExactly("arg1");
    assertThat(copy.getDefaultMessage()).isEqualTo("default");
  }

  @Test
  void equalsAndHashCode() {
    String[] codes1 = { "code1" };
    Object[] args1 = { "arg1" };
    var resolvable1 = new DefaultMessageSourceResolvable(codes1, args1, "default");
    var resolvable2 = new DefaultMessageSourceResolvable(codes1, args1, "default");
    var different = new DefaultMessageSourceResolvable("different");

    assertThat(resolvable1)
            .isEqualTo(resolvable1)
            .isEqualTo(resolvable2)
            .hasSameHashCodeAs(resolvable2)
            .isNotEqualTo(different)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());

    assertThat(resolvable1.hashCode()).isNotEqualTo(different.hashCode());
  }

  @Test
  void shouldRenderDefaultMessage() {
    var resolvable = new DefaultMessageSourceResolvable("test");
    assertThat(resolvable.shouldRenderDefaultMessage()).isTrue();
  }

  @Test
  void toStringOutput() {
    String[] codes = { "code1", "code2" };
    Object[] args = { "arg1", 2 };
    var resolvable = new DefaultMessageSourceResolvable(codes, args, "default");

    String toString = resolvable.toString();
    assertThat(toString)
            .contains("codes [code1,code2]")
            .contains("arguments [arg1,2]")
            .contains("default message [default]")
            .startsWith(DefaultMessageSourceResolvable.class.getName());
  }

  @Test
  void nullValues() {
    var resolvable = new DefaultMessageSourceResolvable(null, null, null);
    assertThat(resolvable.getCodes()).isNull();
    assertThat(resolvable.getArguments()).isNull();
    assertThat(resolvable.getDefaultMessage()).isNull();
    assertThat(resolvable.getCode()).isNull();
  }

  @Test
  void emptyArrays() {
    var resolvable = new DefaultMessageSourceResolvable(new String[0], new Object[0], "default");
    assertThat(resolvable.getCodes()).isEmpty();
    assertThat(resolvable.getArguments()).isEmpty();
    assertThat(resolvable.getCode()).isNull();
  }

  @Test
  void getCodeWithDifferentArrayLengths() {
    var singleCode = new DefaultMessageSourceResolvable(new String[] { "code1" });
    assertThat(singleCode.getCode()).isEqualTo("code1");

    var multipleCodes = new DefaultMessageSourceResolvable(new String[] { "code1", "code2", "code3" });
    assertThat(multipleCodes.getCode()).isEqualTo("code3");
  }

  @Test
  void specialCharactersInArguments() {
    Object[] specialArgs = { "arg with spaces", "arg,with,commas", "arg{with}braces" };
    var resolvable = new DefaultMessageSourceResolvable(new String[] { "test" }, specialArgs, "default");
    assertThat(resolvable.getArguments())
            .containsExactly("arg with spaces", "arg,with,commas", "arg{with}braces");

    String toString = resolvable.toString();
    assertThat(toString).contains("arguments [arg with spaces,arg,with,commas,arg{with}braces]");
  }

}
