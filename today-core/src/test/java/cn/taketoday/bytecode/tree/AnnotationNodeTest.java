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
package cn.taketoday.bytecode.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link AnnotationNode}.
 *
 * @author Eric Bruneton
 */
public class AnnotationNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    AnnotationNode annotationNode = new AnnotationNode("LI;");

    assertEquals("LI;", annotationNode.desc);
  }

  @Test
  public void testVisit() {
    AnnotationNode annotationNode = new AnnotationNode("LI;");

    annotationNode.visit("bytes", new byte[] { 0, 1 });
    annotationNode.visit("booleans", new boolean[] { false, true });
    annotationNode.visit("shorts", new short[] { 0, 1 });
    annotationNode.visit("chars", new char[] { '0', '1' });
    annotationNode.visit("ints", new int[] { 0, 1 });
    annotationNode.visit("longs", new long[] { 0L, 1L });
    annotationNode.visit("floats", new float[] { 0.0f, 1.0f });
    annotationNode.visit("doubles", new double[] { 0.0, 1.0 });
    annotationNode.visit("string", "value");
    annotationNode.visitAnnotation("annotation", "Lpkg/Annotation;");

    assertEquals("bytes", annotationNode.values.get(0));
    final Object actual = annotationNode.values.get(1);
    final List<Byte> expected = Arrays.asList(new Byte[] { 0, 1 });
    assertEquals(expected, actual);
    assertEquals("booleans", annotationNode.values.get(2));
    assertEquals(Arrays.asList(new Boolean[] { false, true }), annotationNode.values.get(3));
    assertEquals("shorts", annotationNode.values.get(4));
    assertEquals(Arrays.asList(new Short[] { 0, 1 }), annotationNode.values.get(5));
    assertEquals("chars", annotationNode.values.get(6));
    assertEquals(Arrays.asList(new Character[] { '0', '1' }), annotationNode.values.get(7));
    assertEquals("ints", annotationNode.values.get(8));
    assertEquals(Arrays.asList(new Integer[] { 0, 1 }), annotationNode.values.get(9));
    assertEquals("longs", annotationNode.values.get(10));
    assertEquals(Arrays.asList(new Long[] { 0L, 1L }), annotationNode.values.get(11));
    assertEquals("floats", annotationNode.values.get(12));
    assertEquals(Arrays.asList(new Float[] { 0.0f, 1.0f }), annotationNode.values.get(13));
    assertEquals("doubles", annotationNode.values.get(14));
    assertEquals(Arrays.asList(new Double[] { 0.0, 1.0 }), annotationNode.values.get(15));
    assertEquals("string", annotationNode.values.get(16));
    assertEquals("value", annotationNode.values.get(17));
    assertEquals("annotation", annotationNode.values.get(18));
    assertEquals("Lpkg/Annotation;", ((AnnotationNode) annotationNode.values.get(19)).desc);
  }

  @Test
  public void testAnnotationNode_accept_skipNestedAnnotations() {
    AnnotationNode annotationNode = new AnnotationNode("LI;");
    annotationNode.visit("bytes", new byte[] { 0, 1 });
    annotationNode.visitAnnotation("annotation", "Lpkg/Annotation;");
    AnnotationNode dstAnnotationNode = new AnnotationNode("LJ;");
    AnnotationVisitor skipNestedAnnotationsVisitor =
            new AnnotationVisitor(dstAnnotationNode) {

              @Override
              public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
                return null;
              }

              @Override
              public AnnotationVisitor visitArray(final String name) {
                return null;
              }
            };

    annotationNode.accept(skipNestedAnnotationsVisitor);

    assertNull(dstAnnotationNode.values);
  }
}
