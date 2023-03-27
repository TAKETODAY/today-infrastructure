/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Detects whether an XML stream is using DTD- or XSD-based validation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class XmlValidationModeDetector {

  /**
   * Indicates that the validation should be disabled.
   */
  public static final int VALIDATION_NONE = 0;

  /**
   * Indicates that the validation mode should be auto-guessed, since we cannot find
   * a clear indication (probably choked on some special characters, or the like).
   */
  public static final int VALIDATION_AUTO = 1;

  /**
   * Indicates that DTD validation should be used (we found a "DOCTYPE" declaration).
   */
  public static final int VALIDATION_DTD = 2;

  /**
   * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
   */
  public static final int VALIDATION_XSD = 3;

  /**
   * The token in an XML document that declares the DTD to use for validation
   * and thus that DTD validation is being used.
   */
  private static final String DOCTYPE = "DOCTYPE";

  /**
   * The token that indicates the start of an XML comment.
   */
  private static final String START_COMMENT = "<!--";

  /**
   * The token that indicates the end of an XML comment.
   */
  private static final String END_COMMENT = "-->";

  /**
   * Indicates whether or not the current parse position is inside an XML comment.
   */
  private boolean inComment;

  /**
   * Detect the validation mode for the XML document in the supplied {@link InputStream}.
   * <p>Note that the supplied {@link InputStream} is closed by this method before returning.
   *
   * @param inputStream the InputStream to parse
   * @throws IOException in case of I/O failure
   * @see #VALIDATION_DTD
   * @see #VALIDATION_XSD
   */
  public int detectValidationMode(InputStream inputStream) throws IOException {
    this.inComment = false;

    // Peek into the file to look for DOCTYPE.
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      boolean isDtdValidated = false;
      String content;
      while ((content = reader.readLine()) != null) {
        content = consumeCommentTokens(content);
        if (StringUtils.isBlank(content)) {
          continue;
        }
        if (hasDoctype(content)) {
          isDtdValidated = true;
          break;
        }
        if (hasOpeningTag(content)) {
          // End of meaningful data...
          break;
        }
      }
      return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
    }
    catch (CharConversionException ex) {
      // Choked on some character encoding...
      // Leave the decision up to the caller.
      return VALIDATION_AUTO;
    }
  }

  /**
   * Does the content contain the DTD DOCTYPE declaration?
   */
  private boolean hasDoctype(String content) {
    return content.contains(DOCTYPE);
  }

  /**
   * Determine if the supplied content contains an XML opening tag.
   * <p>It is expected that all comment tokens will have been consumed for the
   * supplied content before passing the remainder to this method. However, as
   * a sanity check, if the parse state is currently in an XML comment this
   * method always returns {@code false}.
   */
  private boolean hasOpeningTag(String content) {
    if (this.inComment) {
      return false;
    }
    int openTagIndex = content.indexOf('<');
    return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
            Character.isLetter(content.charAt(openTagIndex + 1)));
  }

  /**
   * Consume all comments in the given String and return the remaining content,
   * which may be empty since the supplied content might be all comment data.
   * <p>This method takes the current "in comment" parsing state into account.
   */
  private String consumeCommentTokens(String line) {
    int indexOfStartComment = line.indexOf(START_COMMENT);
    if (indexOfStartComment == -1 && !line.contains(END_COMMENT)) {
      return line;
    }

    String result = "";
    String currLine = line;
    if (!this.inComment && (indexOfStartComment >= 0)) {
      result = line.substring(0, indexOfStartComment);
      currLine = line.substring(indexOfStartComment);
    }

    if ((currLine = consume(currLine)) != null) {
      result += consumeCommentTokens(currLine);
    }
    return result;
  }

  /**
   * Consume the next comment token, update the "inComment" flag,
   * and return the remaining content.
   */
  @Nullable
  private String consume(String line) {
    int index = (this.inComment ? endComment(line) : startComment(line));
    return (index == -1 ? null : line.substring(index));
  }

  /**
   * Try to consume the {@link #START_COMMENT} token.
   *
   * @see #commentToken(String, String, boolean)
   */
  private int startComment(String line) {
    return commentToken(line, START_COMMENT, true);
  }

  /**
   * Try to consume the {@link #END_COMMENT} token.
   *
   * @see #commentToken(String, String, boolean)
   */
  private int endComment(String line) {
    return commentToken(line, END_COMMENT, false);
  }

  /**
   * Try to consume the supplied token against the supplied content and update the
   * "in comment" parse state to the supplied value.
   * <p>Returns the index into the content which is after the token or -1 if the
   * token is not found.
   */
  private int commentToken(String line, String token, boolean inCommentIfPresent) {
    int index = line.indexOf(token);
    if (index > -1) {
      this.inComment = inCommentIfPresent;
    }
    return (index == -1 ? index : index + token.length());
  }

}
