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

package cn.taketoday.core.bytecode.tree;

import java.util.List;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.TypePath;
import cn.taketoday.core.bytecode.TypeReference;

/**
 * A node that represents a type annotation on a local or resource variable.
 *
 * @author Eric Bruneton
 */
public class LocalVariableAnnotationNode extends TypeAnnotationNode {

  /**
   * The fist instructions corresponding to the continuous ranges that make the scope of this local
   * variable (inclusive). Must not be {@literal null}.
   */
  public List<LabelNode> start;

  /**
   * The last instructions corresponding to the continuous ranges that make the scope of this local
   * variable (exclusive). This list must have the same size as the 'start' list. Must not be
   * {@literal null}.
   */
  public List<LabelNode> end;

  /**
   * The local variable's index in each range. This list must have the same size as the 'start'
   * list. Must not be {@literal null}.
   */
  public List<Integer> index;

  /**
   * Constructs a new {@link LocalVariableAnnotationNode}.
   *
   * @param typeRef a reference to the annotated type. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param start the fist instructions corresponding to the continuous ranges that make the scope
   * of this local variable (inclusive).
   * @param end the last instructions corresponding to the continuous ranges that make the scope of
   * this local variable (exclusive). This array must have the same size as the 'start' array.
   * @param index the local variable's index in each range. This array must have the same size as
   * the 'start' array.
   * @param descriptor the class descriptor of the annotation class.
   */
  public LocalVariableAnnotationNode(
          final int typeRef,
          final TypePath typePath,
          final LabelNode[] start,
          final LabelNode[] end,
          final int[] index,
          final String descriptor) {
    super(typeRef, typePath, descriptor);
    this.start = Util.asArrayList(start);
    this.end = Util.asArrayList(end);
    this.index = Util.asArrayList(index);
  }

  /**
   * Makes the given visitor visit this type annotation.
   *
   * @param methodVisitor the visitor that must visit this annotation.
   * @param visible {@literal true} if the annotation is visible at runtime.
   */
  public void accept(final MethodVisitor methodVisitor, final boolean visible) {
    List<LabelNode> end = this.end;
    List<Integer> index = this.index;
    List<LabelNode> start = this.start;

    Label[] startLabels = new Label[start.size()];
    Label[] endLabels = new Label[end.size()];
    int[] indices = new int[index.size()];
    for (int i = 0, n = startLabels.length; i < n; ++i) {
      startLabels[i] = start.get(i).getLabel();
      endLabels[i] = end.get(i).getLabel();
      indices[i] = index.get(i);
    }
    accept(methodVisitor.visitLocalVariableAnnotation(
            typeRef, typePath, startLabels, endLabels, indices, desc, visible));
  }
}
