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
package cn.taketoday.asm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link AsmTest}.
 *
 * @author Eric Bruneton
 */
public class AsmTestTest extends AsmTest {

  @Test
  public void testPrecompiledClass_allMethods() {
    assertEquals("jdk3.AllInstructions", PrecompiledClass.JDK3_ALL_INSTRUCTIONS.getName());
    assertEquals("jdk8/AllInstructions", PrecompiledClass.JDK8_ALL_INSTRUCTIONS.getInternalName());
    assertEquals("module-info", PrecompiledClass.JDK9_MODULE.getInternalName());
//    assertFalse(PrecompiledClass.JDK3_ALL_INSTRUCTIONS.isMoreRecentThan(Api.ASM4));
//    assertTrue(PrecompiledClass.JDK8_ALL_INSTRUCTIONS.isMoreRecentThan(Api.ASM4));
//    assertFalse(PrecompiledClass.JDK8_ALL_INSTRUCTIONS.isMoreRecentThan(Api.ASM5));
//    assertTrue(PrecompiledClass.JDK9_MODULE.isMoreRecentThan(Api.ASM5));
//    assertFalse(PrecompiledClass.JDK9_MODULE.isMoreRecentThan(Api.ASM6));
//    assertTrue(PrecompiledClass.JDK11_ALL_INSTRUCTIONS.isMoreRecentThan(Api.ASM6));
//    assertFalse(PrecompiledClass.JDK11_ALL_INSTRUCTIONS.isMoreRecentThan(Api.ASM7));
    assertNotNull(PrecompiledClass.JDK11_ALL_INSTRUCTIONS.getBytes());
    assertEquals("jdk11.AllInstructions", PrecompiledClass.JDK11_ALL_INSTRUCTIONS.toString());
//    assertTrue(PrecompiledClass.JDK14_ALL_STRUCTURES_RECORD.isMoreRecentThan(Api.ASM7));
//    assertFalse(PrecompiledClass.JDK14_ALL_STRUCTURES_RECORD.isMoreRecentThan(Api.ASM8));
//    assertTrue(PrecompiledClass.JDK14_ALL_STRUCTURES_EMPTY_RECORD.isMoreRecentThan(Api.ASM7));
//    assertFalse(PrecompiledClass.JDK14_ALL_STRUCTURES_EMPTY_RECORD.isMoreRecentThan(Api.ASM8));
//    assertTrue(PrecompiledClass.JDK15_ALL_STRUCTURES.isMoreRecentThan(Api.ASM8));
    assertFalse(PrecompiledClass.JDK15_ALL_STRUCTURES.isMoreRecentThan(Api.ASM9));
  }

  @Test
  public void testInvalidClass_allMethods() {
    InvalidClass invalidBytecodeOffset = InvalidClass.INVALID_BYTECODE_OFFSET;

    assertNotNull(invalidBytecodeOffset.getBytes());
    assertEquals("invalid.InvalidBytecodeOffset", invalidBytecodeOffset.toString());
  }

  @Test
  public void testGetAllClassesAndAllApis() {
    List<Arguments> allArguments = allClassesAndAllApis().collect(Collectors.toList());

    assertEquals(
            new HashSet<Object>(Arrays.asList(PrecompiledClass.values())),
            allArguments.stream().map(arg -> arg.get()[0]).collect(Collectors.toSet()));
    assertEquals(
            new HashSet<Object>(Arrays.asList(Api.values())),
            allArguments.stream().map(arg -> arg.get()[1]).collect(Collectors.toSet()));
  }

  @Test
  public void testGetAllClassesAndLatestApi() {
    List<Arguments> allArguments = allClassesAndLatestApi().collect(Collectors.toList());

    assertEquals(
            new HashSet<Object>(Arrays.asList(PrecompiledClass.values())),
            allArguments.stream().map(arg -> arg.get()[0]).collect(Collectors.toSet()));
    assertEquals(
            new HashSet<Object>(Collections.singletonList(Api.ASM9)),
            allArguments.stream().map(arg -> arg.get()[1]).collect(Collectors.toSet()));
  }
}
