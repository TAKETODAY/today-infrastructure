/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link Handler}.
 *
 * @author Eric Bruneton
 */
public class HandlerTest {

  @Test
  public void testConstructor() {
    Label startPc = new Label();
    Label endPc = new Label();
    Label handlerPc = new Label();
    int catchType = 123;
    String catchDescriptor = "123";

    Handler handler = new Handler(startPc, endPc, handlerPc, catchType, catchDescriptor);

    assertEquals(startPc, handler.startPc);
    assertEquals(endPc, handler.endPc);
    assertEquals(handlerPc, handler.handlerPc);
    assertEquals(catchType, handler.catchType);
    assertEquals(catchDescriptor, handler.catchTypeDescriptor);
  }

  @Test
  public void testCopyConstructor() {
    Label startPc1 = new Label();
    Label endPc1 = new Label();
    Label handlerPc = new Label();
    int catchType = 123;
    String catchDescriptor = "123";
    Handler handler1 = new Handler(startPc1, endPc1, handlerPc, catchType, catchDescriptor);
    Label startPc2 = new Label();
    Label endPc2 = new Label();

    Handler handler2 = new Handler(handler1, startPc2, endPc2);

    assertEquals(startPc2, handler2.startPc);
    assertEquals(endPc2, handler2.endPc);
    assertEquals(handlerPc, handler2.handlerPc);
    assertEquals(catchType, handler2.catchType);
    assertEquals(catchDescriptor, handler2.catchTypeDescriptor);
  }

  @Test
  public void testRemoveRange_removeAllOrNothing() {
    Handler handler = newHandler(10, 20);

    assertEquals(null, Handler.removeRange(null, newLabel(0), newLabel(10)));
    assertEquals(handler, Handler.removeRange(handler, newLabel(0), newLabel(10)));
    assertEquals(handler, Handler.removeRange(handler, newLabel(20), newLabel(30)));
    assertEquals(handler, Handler.removeRange(handler, newLabel(20), null));
    assertEquals(null, Handler.removeRange(handler, newLabel(0), newLabel(30)));
  }

  @Test
  public void testRemoveRange_removeStart() {
    Handler handler = Handler.removeRange(newHandler(10, 20), newLabel(0), newLabel(15));

    assertEquals(15, handler.startPc.bytecodeOffset);
    assertEquals(20, handler.endPc.bytecodeOffset);
    assertEquals(null, handler.nextHandler);
  }

  @Test
  public void testRemoveRange_removeEnd() {
    Handler handler = Handler.removeRange(newHandler(10, 20), newLabel(15), newLabel(30));

    assertEquals(10, handler.startPc.bytecodeOffset);
    assertEquals(15, handler.endPc.bytecodeOffset);
    assertEquals(null, handler.nextHandler);
  }

  @Test
  public void testRemoveRange_removeMiddle() {
    Handler handler = Handler.removeRange(newHandler(10, 20), newLabel(13), newLabel(17));

    assertEquals(10, handler.startPc.bytecodeOffset);
    assertEquals(13, handler.endPc.bytecodeOffset);
    assertEquals(17, handler.nextHandler.startPc.bytecodeOffset);
    assertEquals(20, handler.nextHandler.endPc.bytecodeOffset);
    assertEquals(null, handler.nextHandler.nextHandler);
  }

  @Test
  public void testGetExceptionTableLength() {
    Handler handler = newHandler(10, 20);

    assertEquals(0, Handler.getExceptionTableLength(null));
    assertEquals(1, Handler.getExceptionTableLength(handler));
  }

  @Test
  public void testGetExceptionTableSize() {
    Handler handlerList = Handler.removeRange(newHandler(10, 20), newLabel(13), newLabel(17));

    assertEquals(2, Handler.getExceptionTableSize(null));
    assertEquals(18, Handler.getExceptionTableSize(handlerList));
  }

  @Test
  public void testPutExceptionTable() {
    Handler handlerList = Handler.removeRange(newHandler(10, 20), newLabel(13), newLabel(17));
    ByteVector byteVector = new ByteVector();

    Handler.putExceptionTable(handlerList, byteVector);

    assertEquals(18, byteVector.length);
  }

  private static Handler newHandler(final int startPc, final int endPc) {
    return new Handler(newLabel(startPc), newLabel(endPc), newLabel(0), 0, "");
  }

  private static Label newLabel(final int bytecodeOffset) {
    Label label = new Label();
    label.bytecodeOffset = bytecodeOffset;
    return label;
  }
}
