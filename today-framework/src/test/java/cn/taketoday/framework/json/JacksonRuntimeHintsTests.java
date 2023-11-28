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

package cn.taketoday.framework.json;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers;
import com.fasterxml.jackson.databind.ser.std.TokenBufferSerializer;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;

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