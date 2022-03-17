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

package cn.taketoday.expression.spel;

import java.io.PrintStream;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelExpression;

/**
 * Utilities for working with Spring Expressions.
 *
 * @author Andy Clement
 */
public class SpelUtilities {

  /**
   * Output an indented representation of the expression syntax tree to the specified output stream.
   *
   * @param printStream the output stream to print into
   * @param expression the expression to be displayed
   */
  public static void printAbstractSyntaxTree(PrintStream printStream, Expression expression) {
    printStream.println("===> Expression '" + expression.getExpressionString() + "' - AST start");
    printAST(printStream, ((SpelExpression) expression).getAST(), "");
    printStream.println("===> Expression '" + expression.getExpressionString() + "' - AST end");
  }

  /*
   * Helper method for printing the AST with indentation
   */
  private static void printAST(PrintStream out, SpelNode t, String indent) {
    if (t != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(indent).append(t.getClass().getSimpleName());
      sb.append("  value:").append(t.toStringAST());
      sb.append(t.getChildCount() < 2 ? "" : "  #children:" + t.getChildCount());
      out.println(sb);
      for (int i = 0; i < t.getChildCount(); i++) {
        printAST(out, t.getChild(i), indent + "  ");
      }
    }
  }

}
