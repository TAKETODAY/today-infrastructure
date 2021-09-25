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
package cn.taketoday.core.bytecode.commons;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MethodSignature}.
 *
 * @author Eric Bruneton
 */
public class MethodSignatureTest {

  @Test
  public void testConstructor_fromDescriptor() {
    MethodSignature method = new MethodSignature("name", "(I)J");

    assertEquals("name", method.getName());
    assertEquals("(I)J", method.getDescriptor());
    assertEquals(Type.LONG_TYPE, method.getReturnType());
    assertArrayEquals(new Type[] { Type.INT_TYPE }, method.getArgumentTypes());
    assertEquals("name(I)J", method.toString());
  }

  @Test
  public void testConstructor_fromTypes() {
    MethodSignature method = new MethodSignature(Type.LONG_TYPE, "name", Type.INT_TYPE);

    assertEquals("name", method.getName());
    assertEquals("(I)J", method.getDescriptor());
    assertEquals(Type.LONG_TYPE, method.getReturnType());
    assertArrayEquals(new Type[] { Type.INT_TYPE }, method.getArgumentTypes());
    assertEquals("name(I)J", method.toString());
  }

  @Test
  public void testGetMethod_fromMethodObject() throws ReflectiveOperationException {
    MethodSignature method = MethodSignature.from(Object.class.getMethod("equals", Object.class));

    assertEquals("equals", method.getName());
    assertEquals("(Ljava/lang/Object;)Z", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromConstructorObject() throws ReflectiveOperationException {
    MethodSignature method = MethodSignature.from(Object.class.getConstructor());

    assertEquals("<init>", method.getName());
    assertEquals("()V", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromDescriptor() {
    MethodSignature method =
            MethodSignature.from(
                    "boolean name(byte, char, short, int, float, long, double, pkg.Class, pkg.Class[])");

    assertEquals("name", method.getName());
    assertEquals("(BCSIFJDLpkg/Class;[Lpkg/Class;)Z", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromInvalidDescriptor() {
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("name()"));
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("void name"));
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("void name(]"));
  }

  @Test
  public void testGetMethod_withDefaultPackage() {
    MethodSignature withoutDefaultPackage =
            MethodSignature.from("void name(Object)", /* defaultPackage= */ false);
    MethodSignature withDefaultPackage = MethodSignature.from("void name(Object)", /* defaultPackage= */ true);

    assertEquals("(Ljava/lang/Object;)V", withoutDefaultPackage.getDescriptor());
    assertEquals("(LObject;)V", withDefaultPackage.getDescriptor());
  }

  @Test
  public void testEquals() {
    MethodSignature nullMethod = null;

    boolean equalsNull = new MethodSignature("name", "()V").equals(nullMethod);
    boolean equalsMethodWithDifferentName =
            new MethodSignature("name", "()V").equals(new MethodSignature("other", "()V"));
    boolean equalsMethodWithDifferentDescriptor =
            new MethodSignature("name", "()V").equals(new MethodSignature("name", "(I)J"));
    boolean equalsSame = new MethodSignature("name", "()V").equals(MethodSignature.from("void name()"));

    assertFalse(equalsNull);
    assertFalse(equalsMethodWithDifferentName);
    assertFalse(equalsMethodWithDifferentDescriptor);
    assertTrue(equalsSame);
  }

  @Test
  public void testHashCode() {
    assertNotEquals(0, new MethodSignature("name", "()V").hashCode());
  }

  @Test
  public void forConstructor() {
    MethodSignature cstruct_object = MethodSignature.forConstructor("Object");

    MethodSignature withoutDefaultPackage =
            MethodSignature.from("void <init>(Object)", /* defaultPackage= */ false);

    assertEquals("(Ljava/lang/Object;)V", withoutDefaultPackage.getDescriptor());

    assertThat(cstruct_object.getDescriptor())
            .isEqualTo(withoutDefaultPackage.getDescriptor());

    // <init>(Object,Object,Class)
    MethodSignature cstruct_objects = MethodSignature.forConstructor("Object, Object ,Class ");
    MethodSignature signature =
            MethodSignature.from("void <init>(Object,Object,Class)");

    assertThat(cstruct_objects).isEqualTo(signature);
  }

}
