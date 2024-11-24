/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.json;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers;
import com.fasterxml.jackson.databind.ser.std.TokenBufferSerializer;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:40
 */
class JacksonRuntimeHintsTests {

  @Test
  void shouldRegisterSerializerConstructors() {
    ReflectionHints hints = registerHints();
    Stream
            .of(StdJdkSerializers.AtomicBooleanSerializer.class, StdJdkSerializers.AtomicIntegerSerializer.class, StdJdkSerializers.AtomicLongSerializer.class,
                    FileSerializer.class, ClassSerializer.class, TokenBufferSerializer.class)
            .forEach((serializer) -> {
              TypeHint typeHint = hints.getTypeHint(serializer);
              assertThat(typeHint).withFailMessage(() -> "No hints found for serializer " + serializer).isNotNull();
              Set<MemberCategory> memberCategories = typeHint.getMemberCategories();
              assertThat(memberCategories).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
            });
  }

  private ReflectionHints registerHints() {
    RuntimeHints hints = new RuntimeHints();
    new JacksonRuntimeHints().registerHints(hints, getClass().getClassLoader());
    return hints.reflection();
  }

}