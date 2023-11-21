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

package cn.taketoday.util.xml;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import cn.taketoday.lang.Assert;

/**
 * Contains common behavior relating to {@link Transformer Transformers}
 * and the {@code javax.xml.transform} package in general.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class TransformerUtils {

  /**
   * The indent amount of characters if {@link #enableIndenting indenting is enabled}.
   * <p>Defaults to "2".
   */
  public static final int DEFAULT_INDENT_AMOUNT = 2;

  /**
   * Enable indenting for the supplied {@link Transformer}.
   * <p>If the underlying XSLT engine is Xalan, then the special output key {@code indent-amount}
   * will be also be set to a value of {@link #DEFAULT_INDENT_AMOUNT} characters.
   *
   * @param transformer the target transformer
   * @see Transformer#setOutputProperty(String, String)
   * @see OutputKeys#INDENT
   */
  public static void enableIndenting(Transformer transformer) {
    enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
  }

  /**
   * Enable indenting for the supplied {@link Transformer}.
   * <p>If the underlying XSLT engine is Xalan, then the special output key {@code indent-amount}
   * will be also be set to a value of {@link #DEFAULT_INDENT_AMOUNT} characters.
   *
   * @param transformer the target transformer
   * @param indentAmount the size of the indent (2 characters, 3 characters, etc)
   * @see Transformer#setOutputProperty(String, String)
   * @see OutputKeys#INDENT
   */
  public static void enableIndenting(Transformer transformer, int indentAmount) {
    Assert.notNull(transformer, "Transformer is required");
    if (indentAmount < 0) {
      throw new IllegalArgumentException("Invalid indent amount (must not be less than zero): " + indentAmount);
    }
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    try {
      // Xalan-specific, but this is the most common XSLT engine in any case
      transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", String.valueOf(indentAmount));
    }
    catch (IllegalArgumentException ignored) {
    }
  }

  /**
   * Disable indenting for the supplied {@link Transformer}.
   *
   * @param transformer the target transformer
   * @see OutputKeys#INDENT
   */
  public static void disableIndenting(Transformer transformer) {
    Assert.notNull(transformer, "Transformer is required");
    transformer.setOutputProperty(OutputKeys.INDENT, "no");
  }

}
