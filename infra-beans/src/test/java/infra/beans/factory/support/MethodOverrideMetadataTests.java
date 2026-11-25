/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MethodOverrideMetadataTests {

  @Test
  void replaceOverrideMatchesOverloadsByArgumentCountAndTypeName() throws Exception {
    var identifiers = new ArrayList<>(List.of("String"));
    var override = new ReplaceOverride("lookup", "replacer", identifiers);
    identifiers.clear();
    var noArg = Lookups.class.getDeclaredMethod("lookup");
    var text = Lookups.class.getDeclaredMethod("lookup", String.class);
    var number = Lookups.class.getDeclaredMethod("lookup", Integer.class);
    var qualified = Lookups.class.getDeclaredMethod("lookup", String.class, int.class);

    assertThat(override.getTypeIdentifiers()).containsExactly("String");
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> override.getTypeIdentifiers().clear());
    assertThat(override.matches(text)).isTrue();
    assertThat(override.matches(noArg)).isFalse();
    assertThat(override.matches(number)).isFalse();

    override.addTypeIdentifier("int");
    assertThat(override.matches(text)).isFalse();
    assertThat(override.matches(qualified)).isTrue();
    override.setOverloaded(false);
    assertThat(override.matches(number)).isTrue();
    assertThat(override.matches(Object.class.getMethod("toString"))).isFalse();
  }

  @Test
  void lookupOverrideDistinguishesExplicitMethodsFromOverloadedNames() throws Exception {
    var noArg = Lookups.class.getDeclaredMethod("lookup");
    var concrete = Lookups.class.getDeclaredMethod("lookup", String.class);
    var abstractMethod = Lookups.class.getDeclaredMethod("lookup", Integer.class);
    var named = new LookupOverride("lookup", null);
    var explicit = new LookupOverride(concrete, "target");

    assertThat(named.matches(noArg)).isTrue();
    assertThat(named.matches(abstractMethod)).isTrue();
    assertThat(named.matches(concrete)).isFalse();
    assertThat(named.matches(Object.class.getMethod("toString"))).isFalse();
    named.setOverloaded(false);
    assertThat(named.matches(concrete)).isTrue();
    assertThat(explicit.matches(concrete)).isTrue();
    assertThat(explicit.matches(noArg)).isFalse();
  }

  @Test
  void overrideEqualityIncludesSourceTargetAndSignature() throws Exception {
    var method = Lookups.class.getDeclaredMethod("lookup", String.class);
    var lookup = new LookupOverride(method, "target");
    var equalLookup = new LookupOverride(method, "target");
    var replacement = new ReplaceOverride("lookup", "replacer", List.of("String"));
    var equalReplacement = new ReplaceOverride("lookup", "replacer", List.of("String"));

    assertThat(lookup).isEqualTo(equalLookup).hasSameHashCodeAs(equalLookup);
    assertThat(lookup).isNotEqualTo(new LookupOverride(method, "other"))
            .isNotEqualTo(new LookupOverride("lookup", "target")).hasToString("LookupOverride for method 'lookup'");
    assertThat(replacement).isEqualTo(equalReplacement).hasSameHashCodeAs(equalReplacement);
    assertThat(replacement).isNotEqualTo(new ReplaceOverride("lookup", "other", List.of("String")))
            .isNotEqualTo(new ReplaceOverride("lookup", "replacer", List.of("Integer")))
            .isNotEqualTo(new ReplaceOverride("other", "replacer", List.of("String")))
            .hasToString("Replace override for method 'lookup'");
    assertThat(replacement).isNotEqualTo(lookup);
    assertThat(lookup).isNotEqualTo(replacement);

    equalReplacement.setSource("configuration source");
    assertThat(equalReplacement.getSource()).isEqualTo("configuration source");
    assertThat(replacement).isNotEqualTo(equalReplacement);
  }

  @Test
  void copiesOverrideCollectionsAndUsesTheLastMatch() throws Exception {
    var method = Lookups.class.getDeclaredMethod("lookup");
    var original = new MethodOverrides();
    var first = new LookupOverride(method, "first");
    var last = new LookupOverride(method, "last");
    original.addOverrides(null);
    original.addOverride(first);
    var copy = new MethodOverrides(original);

    assertThat(copy).isEqualTo(original).hasSameHashCodeAs(original).isNotEqualTo("not overrides");
    assertThat(copy.equals(copy)).isTrue();
    original.addOverride(last);
    assertThat(original.getOverride(method)).isSameAs(last);
    assertThat(copy.getOverride(method)).isSameAs(first);
    assertThat(copy).isNotEqualTo(original);
  }

  abstract static class Lookups {

    Object lookup() {
      return null;
    }

    Object lookup(String value) {
      return value;
    }

    abstract Object lookup(Integer value);

    Object lookup(String value, int count) {
      return value.repeat(count);
    }
  }

}
