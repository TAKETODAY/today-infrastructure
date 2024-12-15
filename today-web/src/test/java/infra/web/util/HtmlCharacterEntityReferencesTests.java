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

package infra.web.util;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Martin Kersten
 * @author Juergen Hoeller
 */
public class HtmlCharacterEntityReferencesTests {

  private static final String DTD_FILE = "HtmlCharacterEntityReferences.dtd";

  @Test
  public void testSupportsAllCharacterEntityReferencesDefinedByHtml() {
    HtmlCharacterEntityReferences references = new HtmlCharacterEntityReferences();
    Map<Integer, String> charactersMap = getReferenceCharacterMap();
    for (int character = 0; character < 10000; character++) {
      String referenceName = charactersMap.get(character);
      if (referenceName != null) {
        String fullReference = HtmlCharacterEntityReferences.REFERENCE_START + referenceName + HtmlCharacterEntityReferences.REFERENCE_END;
        assertThat(references.isMappedToReference((char) character))
                .as("The unicode character " + character + " should be mapped to a reference")
                .isTrue();
        assertThat(references.convertToReference((char) character))
                .as("The reference of unicode character " + character + " should be entity " + referenceName)
                .isEqualTo(fullReference);
        assertThat(references.convertToCharacter(referenceName))
                .as("The entity reference [" + referenceName + "] should be mapped to unicode character " + character)
                .isEqualTo((char) character);
      }
      else if (character == 39) {
        assertThat(references.isMappedToReference((char) character)).isTrue();
        assertThat(references.convertToReference((char) character)).isEqualTo("&#39;");
      }
      else {
        assertThat(references.isMappedToReference((char) character))
                .as("The unicode character " + character + " should not be mapped to a reference")
                .isFalse();
        assertThat(references.convertToReference((char) character))
                .as("No entity reference of unicode character " + character + " should exist")
                .isNull();
      }
    }
    assertThat(references.getSupportedReferenceCount())
            .as("The registered entity count of entityReferences should match the number of entity references")
            .isEqualTo(charactersMap.size() + 1);
    assertThat(references.getSupportedReferenceCount()).as(
                    "The HTML 4.0 Standard defines 252+1 entity references so do entityReferences")
            .isEqualTo(252 + 1);
    assertThat((int) references.convertToCharacter("invalid"))
            .as("Invalid entity reference names should not be convertible")
            .isEqualTo((char) -1);
  }

  //
  @Test
  public void testConvertToReferenceUTF8() {
    HtmlCharacterEntityReferences entityReferences = new HtmlCharacterEntityReferences();
    String utf8 = "UTF-8";
    assertThat(entityReferences.convertToReference('<', utf8)).isEqualTo("&lt;");
    assertThat(entityReferences.convertToReference('>', utf8)).isEqualTo("&gt;");
    assertThat(entityReferences.convertToReference('&', utf8)).isEqualTo("&amp;");
    assertThat(entityReferences.convertToReference('"', utf8)).isEqualTo("&quot;");
    assertThat(entityReferences.convertToReference('\'', utf8)).isEqualTo("&#39;");
    assertThat(entityReferences.convertToReference((char) 233, utf8)).isNull();
    assertThat(entityReferences.convertToReference((char) 934, utf8)).isNull();
  }

  private Map<Integer, String> getReferenceCharacterMap() {
    CharacterEntityResourceIterator entityIterator = new CharacterEntityResourceIterator();
    Map<Integer, String> referencedCharactersMap = new HashMap<>();
    while (entityIterator.hasNext()) {
      int character = entityIterator.getReferredCharacter();
      String entityName = entityIterator.nextEntry();
      referencedCharactersMap.put(character, entityName);
    }
    return referencedCharactersMap;
  }

  private static class CharacterEntityResourceIterator {

    private final StreamTokenizer tokenizer;

    private String currentEntityName = null;

    private int referredCharacter = -1;

    public CharacterEntityResourceIterator() {
      try {
        InputStream inputStream = getClass().getResourceAsStream(DTD_FILE);
        if (inputStream == null) {
          throw new IOException("Cannot find definition resource [" + DTD_FILE + "]");
        }
        tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to open definition resource [" + DTD_FILE + "]");
      }
    }

    public boolean hasNext() {
      return (currentEntityName != null || readNextEntity());
    }

    public String nextEntry() {
      if (hasNext()) {
        String entityName = currentEntityName;
        currentEntityName = null;
        return entityName;
      }
      return null;
    }

    public int getReferredCharacter() {
      return referredCharacter;
    }

    private boolean readNextEntity() {
      try {
        while (navigateToNextEntity()) {
          String entityName = nextWordToken();
          if ("CDATA".equals(nextWordToken())) {
            int referredCharacter = nextReferredCharacterId();
            if (entityName != null && referredCharacter != -1) {
              this.currentEntityName = entityName;
              this.referredCharacter = referredCharacter;
              return true;
            }
          }
        }
        return false;
      }
      catch (IOException ex) {
        throw new IllegalStateException("Could not parse definition resource: " + ex.getMessage());
      }
    }

    private boolean navigateToNextEntity() throws IOException {
      while (tokenizer.nextToken() != StreamTokenizer.TT_WORD || !"ENTITY".equals(tokenizer.sval)) {
        if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
          return false;
        }
      }
      return true;
    }

    private int nextReferredCharacterId() throws IOException {
      String reference = nextWordToken();
      if (reference != null && reference.startsWith("&#") && reference.endsWith(";")) {
        return Integer.parseInt(reference.substring(2, reference.length() - 1));
      }
      return -1;
    }

    private String nextWordToken() throws IOException {
      tokenizer.nextToken();
      return tokenizer.sval;
    }
  }

}
