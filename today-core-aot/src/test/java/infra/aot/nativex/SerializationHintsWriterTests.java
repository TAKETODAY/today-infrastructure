/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aot.nativex;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;

import infra.aot.hint.SerializationHints;
import infra.aot.hint.TypeReference;
import infra.core.env.Environment;

/**
 * Tests for {@link SerializationHintsWriter}.
 *
 * @author Sebastien Deleuze
 */
public class SerializationHintsWriterTests {

  @Test
  void shouldWriteEmptyHint() throws JSONException {
    SerializationHints hints = new SerializationHints();
    assertEquals("[]", hints);
  }

  @Test
  void shouldWriteSingleHint() throws JSONException {
    SerializationHints hints = new SerializationHints().registerType(TypeReference.of(String.class));
    assertEquals("""
            [
            	{ "name": "java.lang.String" }
            ]""", hints);
  }

  @Test
  void shouldWriteMultipleHints() throws JSONException {
    SerializationHints hints = new SerializationHints()
            .registerType(TypeReference.of(String.class))
            .registerType(TypeReference.of(Environment.class));
    assertEquals("""
            [
            	{ "name": "java.lang.String" },
            	{ "name": "infra.core.env.Environment" }
            ]""", hints);
  }

  @Test
  void shouldWriteSingleHintWithCondition() throws JSONException {
    SerializationHints hints = new SerializationHints().registerType(TypeReference.of(String.class),
            builder -> builder.onReachableType(TypeReference.of("org.example.Test")));
    assertEquals("""
            [
            	{ "condition": { "typeReachable": "org.example.Test" }, "name": "java.lang.String" }
            ]""", hints);
  }

  private void assertEquals(String expectedString, SerializationHints hints) throws JSONException {
    StringWriter out = new StringWriter();
    BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
    SerializationHintsWriter.write(writer, hints);
    JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.NON_EXTENSIBLE);
  }

}
