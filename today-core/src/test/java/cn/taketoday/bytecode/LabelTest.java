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
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link Label}.
 *
 * @author Eric Bruneton
 */
public class LabelTest {

  /** Tests that {@link Label#getOffset()} returns a correct offset after the label is visited. */
  @Test
  public void testGetOffset() {
    MethodVisitor methodVisitor =
            new ClassWriter(0).visitMethod(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    Label label = new Label();

    methodVisitor.visitLabel(label);

    assertEquals(1, label.getOffset());
  }

  /** Tests that {@link Label#getOffset()} throws an exception before the label is visited. */
  @Test
  public void testGetOffset_illegalState() {
    Executable getOffset = () -> new Label().getOffset();

    Exception exception = assertThrows(IllegalStateException.class, getOffset);
    assertEquals("Label offset position has not been resolved yet", exception.getMessage());
  }

  /** Tests that {@link Label#toString()} returns strings starting with "L". */
  @Test
  public void testToString() {
    String string = new Label().toString();

    assertEquals('L', string.charAt(0));
  }
}
