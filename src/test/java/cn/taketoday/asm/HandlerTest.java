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
