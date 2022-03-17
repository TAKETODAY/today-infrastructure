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

package cn.taketoday.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import cn.taketoday.lang.Nullable;

/**
 * Represents a set of character entity references defined by the
 * HTML 4.0 standard.
 *
 * <p>A complete description of the HTML 4.0 character set can be found
 * at https://www.w3.org/TR/html4/charset.html.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 4.0
 */
class HtmlCharacterEntityReferences {

  private static final String PROPERTIES_FILE = "HtmlCharacterEntityReferences.properties";

  static final char REFERENCE_START = '&';

  static final String DECIMAL_REFERENCE_START = "&#";

  static final String HEX_REFERENCE_START = "&#x";

  static final char REFERENCE_END = ';';

  static final char CHAR_NULL = (char) -1;

  private final String[] characterToEntityReferenceMap = new String[3000];

  private final HashMap<String, Character> entityReferenceToCharacterMap = new HashMap<>(512);

  /**
   * Returns a new set of character entity references reflecting the HTML 4.0 character set.
   */
  public HtmlCharacterEntityReferences() {
    Properties entityReferences = new Properties();

    // Load reference definition file
    InputStream is = HtmlCharacterEntityReferences.class.getResourceAsStream(PROPERTIES_FILE);
    if (is == null) {
      throw new IllegalStateException(
              "Cannot find reference definition file [HtmlCharacterEntityReferences.properties] as class path resource");
    }
    try {
      try {
        entityReferences.load(is);
      }
      finally {
        is.close();
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException(
              "Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " + ex.getMessage());
    }

    // Parse reference definition properties
    Enumeration<?> keys = entityReferences.propertyNames();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      int referredChar = Integer.parseInt(key);
      if (!(referredChar < 1000 || (referredChar >= 8000 && referredChar < 10000))) {
        throw new IllegalStateException("Invalid reference to special HTML entity: " + referredChar);
      }
      int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
      String reference = entityReferences.getProperty(key);
      this.characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
      this.entityReferenceToCharacterMap.put(reference, (char) referredChar);
    }
  }

  /**
   * Return the number of supported entity references.
   */
  public int getSupportedReferenceCount() {
    return this.entityReferenceToCharacterMap.size();
  }

  /**
   * Return true if the given character is mapped to a supported entity reference.
   */
  public boolean isMappedToReference(char character) {
    return isMappedToReference(character, HtmlUtils.CHARACTER_ENCODING);
  }

  /**
   * Return true if the given character is mapped to a supported entity reference.
   */
  public boolean isMappedToReference(char character, String encoding) {
    return (convertToReference(character, encoding) != null);
  }

  /**
   * Return the reference mapped to the given character, or {@code null} if none found.
   */
  @Nullable
  public String convertToReference(char character) {
    return convertToReference(character, HtmlUtils.CHARACTER_ENCODING);
  }

  /**
   * Return the reference mapped to the given character, or {@code null} if none found.
   */
  @Nullable
  public String convertToReference(char character, String encoding) {
    if (encoding.startsWith("UTF-")) {
      switch (character) {
        case '<':
          return "&lt;";
        case '>':
          return "&gt;";
        case '"':
          return "&quot;";
        case '&':
          return "&amp;";
        case '\'':
          return "&#39;";
      }
    }
    else if (character < 1000 || (character >= 8000 && character < 10000)) {
      int index = (character < 1000 ? character : character - 7000);
      return this.characterToEntityReferenceMap[index];
    }
    return null;
  }

  /**
   * Return the char mapped to the given entityReference or -1.
   */
  public char convertToCharacter(String entityReference) {
    Character referredCharacter = this.entityReferenceToCharacterMap.get(entityReference);
    if (referredCharacter != null) {
      return referredCharacter;
    }
    return CHAR_NULL;
  }

}
