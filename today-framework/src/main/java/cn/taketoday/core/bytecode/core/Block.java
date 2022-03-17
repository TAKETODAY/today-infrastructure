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
package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.bytecode.Label;

public class Block {
  private Label end;
  private final Label start;
  private final CodeEmitter e;

  public Block(CodeEmitter e) {
    this.e = e;
    start = e.mark();
  }

  public CodeEmitter getCodeEmitter() {
    return e;
  }

  public void end() {
    if (end != null) {
      throw new IllegalStateException("end of label already set");
    }
    end = e.mark();
  }

  public Label getStart() {
    return start;
  }

  public Label getEnd() {
    return end;
  }
}
