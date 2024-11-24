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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.aot.hint.ProxyHints;
import infra.aot.hint.TypeReference;

/**
 * Tests for {@link ProxyHintsWriter}.
 *
 * @author Sebastien Deleuze
 */
public class ProxyHintsWriterTests {

  @Test
  void empty() throws JSONException {
    ProxyHints hints = new ProxyHints();
    assertEquals("[]", hints);
  }

  @Test
  void shouldWriteOneEntry() throws JSONException {
    ProxyHints hints = new ProxyHints();
    hints.registerJdkProxy(Function.class);
    assertEquals("""
            [
            	{ "interfaces": [ "java.util.function.Function" ] }
            ]""", hints);
  }

  @Test
  void shouldWriteMultipleEntries() throws JSONException {
    ProxyHints hints = new ProxyHints();
    hints.registerJdkProxy(Function.class);
    hints.registerJdkProxy(Function.class, Consumer.class);
    assertEquals("""
            [
            	{ "interfaces": [ "java.util.function.Function" ] },
            	{ "interfaces": [ "java.util.function.Function", "java.util.function.Consumer" ] }
            ]""", hints);
  }

  @Test
  void shouldWriteEntriesInNaturalOrder() throws JSONException {
    ProxyHints hints = new ProxyHints();
    hints.registerJdkProxy(Supplier.class);
    hints.registerJdkProxy(Function.class);
    assertEquals("""
            [
            	{ "interfaces": [ "java.util.function.Function" ] },
            	{ "interfaces": [ "java.util.function.Supplier" ] }
            ]""", hints);
  }

  @Test
  void shouldWriteInnerClass() throws JSONException {
    ProxyHints hints = new ProxyHints();
    hints.registerJdkProxy(Inner.class);
    assertEquals("""
            [
            	{ "interfaces": [ "infra.aot.nativex.ProxyHintsWriterTests$Inner" ] }
            ]""", hints);
  }

  @Test
  void shouldWriteCondition() throws JSONException {
    ProxyHints hints = new ProxyHints();
    hints.registerJdkProxy(builder -> builder.proxiedInterfaces(Function.class)
            .onReachableType(TypeReference.of("org.example.Test")));
    assertEquals("""
            [
            	{ "condition": { "typeReachable": "org.example.Test"}, "interfaces": [ "java.util.function.Function" ] }
            ]""", hints);
  }

  private void assertEquals(String expectedString, ProxyHints hints) throws JSONException {
    StringWriter out = new StringWriter();
    BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
    ProxyHintsWriter.write(writer, hints);
    JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.NON_EXTENSIBLE);
  }

  interface Inner {

  }

}
