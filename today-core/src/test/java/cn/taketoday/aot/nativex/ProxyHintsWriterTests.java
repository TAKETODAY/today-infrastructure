/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.nativex;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.TypeReference;

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
	void shouldWriteInnerClass() throws JSONException {
		ProxyHints hints = new ProxyHints();
		hints.registerJdkProxy(Inner.class);
		assertEquals("""
				[
					{ "interfaces": [ "cn.taketoday.aot.nativex.ProxyHintsWriterTests$Inner" ] }
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
		ProxyHintsWriter.INSTANCE.write(writer, hints);
		JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.NON_EXTENSIBLE);
	}

	interface Inner {

	}

}
