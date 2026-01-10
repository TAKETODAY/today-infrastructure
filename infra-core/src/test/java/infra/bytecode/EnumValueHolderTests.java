// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode;

import org.junit.jupiter.api.Test;

import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 17:23
 */
class EnumValueHolderTests {

  @Test
  void testConstructorAndGetters() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");

    assertThat(holder.getName()).isEqualTo("SOURCE");
    assertThat(holder.getDescriptor()).isEqualTo("Ljava/lang/annotation/RetentionPolicy;");
  }

  @Test
  void testToString() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");
    String result = holder.toString();

    assertThat(result).contains("EnumValueHolder")
            .contains("SOURCE")
            .contains("Ljava/lang/annotation/RetentionPolicy;");
  }

  @Test
  void testGetInternalWithRealEnum() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");
    Object value = holder.read();

    assertThat(value)
            .isInstanceOf(RetentionPolicy.class)
            .isEqualTo(RetentionPolicy.SOURCE);
  }

  @Test
  void testCacheMechanism() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");

    Object firstRead = holder.read();
    Object secondRead = holder.read();

    assertThat(firstRead).isSameAs(secondRead);
  }

  @Test
  void testInvalidEnumValue() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "INVALID_VALUE");

    assertThatThrownBy(holder::read)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("INVALID_VALUE");
  }

  @Test
  void testInvalidEnumClass() {
    EnumValueHolder holder = new EnumValueHolder("Linvalid/enum/Class;", "ANY");

    assertThatThrownBy(holder::read)
            .isInstanceOf(RuntimeException.class);
  }
}
