/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode;

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
