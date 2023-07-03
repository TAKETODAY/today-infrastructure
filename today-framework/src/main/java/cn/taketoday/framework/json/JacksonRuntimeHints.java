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
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicBooleanSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicIntegerSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicLongSerializer;
import com.fasterxml.jackson.databind.ser.std.TokenBufferSerializer;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for Jackson.
 *
 * @author Moritz Halbritter
 */
class JacksonRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (!ClassUtils.isPresent("com.fasterxml.jackson.databind.ser.BasicSerializerFactory", classLoader)) {
      return;
    }
    registerSerializers(hints.reflection());
  }

  private void registerSerializers(ReflectionHints hints) {
    hints.registerTypes(TypeReference.listOf(AtomicBooleanSerializer.class, AtomicIntegerSerializer.class,
                    AtomicLongSerializer.class, FileSerializer.class, ClassSerializer.class, TokenBufferSerializer.class),
            TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
  }

}
