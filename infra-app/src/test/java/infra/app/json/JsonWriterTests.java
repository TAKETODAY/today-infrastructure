/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import infra.app.json.JsonWriter.MemberPath;
import infra.app.json.JsonWriter.Members;
import infra.app.json.JsonWriter.NameProcessor;
import infra.app.json.JsonWriter.PairExtractor;
import infra.app.json.JsonWriter.ValueProcessor;
import infra.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class JsonWriterTests {

  private static final Person PERSON = new Person("Infra", "App", 10);

  @Test
  void writeToStringWritesToString() {
    assertThat(ofFormatString("%s").writeToString(123)).isEqualTo("123");
  }

  @Test
  void writeReturnsWritableJson() {
    assertThat(ofFormatString("%s").write(123)).isInstanceOf(WritableJson.class);
  }

  @Test
  void withSuffixAddsSuffixToWrittenString() {
    assertThat(ofFormatString("%s").withSuffix("000").writeToString(123)).isEqualTo("123000");
  }

  @Test
  void withSuffixWhenSuffixIsNullReturnsExistingWriter() {
    JsonWriter<?> writer = ofFormatString("%s");
    assertThat(writer.withSuffix(null)).isSameAs(writer);
  }

  @Test
  void withSuffixWhenSuffixIsEmptyReturnsExistingWriter() {
    JsonWriter<?> writer = ofFormatString("%s");
    assertThat(writer.withSuffix("")).isSameAs(writer);
  }

  @Test
  void withNewLineAtEndAddsNewLineToWrittenString() {
    assertThat(ofFormatString("%s").withNewLineAtEnd().writeToString(123)).isEqualTo("123\n");
  }

  @Test
  void ofAddingNamedSelf() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("test"));
    assertThat(writer.writeToString(PERSON)).isEqualTo("""
            {"test":"Infra App (10)"}""");
  }

  @Test
  void ofAddingUnnamedSelf() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.add());
    assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Infra App (10)"));
  }

  @Test
  void ofAddValue() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("Infra", "App"));
    assertThat(writer.writeToString(PERSON)).isEqualTo("""
            {"Infra":"App"}""");
  }

  @Test
  void ofAddSupplier() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("Infra", () -> "App"));
    assertThat(writer.writeToString(PERSON)).isEqualTo("""
            {"Infra":"App"}""");
  }

  @Test
  void ofAddExtractor() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> {
      members.add("firstName", Person::firstName);
      members.add("lastName", Person::lastName);
      members.add("age", Person::age);
    });
    assertThat(writer.writeToString(PERSON)).isEqualTo("""
            {"firstName":"Infra","lastName":"App","age":10}""");
  }

  @Test
  void ofFromValue() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.from("App"));
    assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("App"));
  }

  @Test
  void ofFromSupplier() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.from(() -> "App"));
    assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("App"));
  }

  @Test
  void ofFromExtractor() {
    JsonWriter<Person> writer = JsonWriter.of((members) -> members.from(Person::lastName));
    assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("App"));
  }

  @Test
  void ofAddMapEntries() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("a", "A");
    map.put("b", 123);
    map.put("c", true);
    JsonWriter<List<Map<String, Object>>> writer = JsonWriter
            .of((members) -> members.addMapEntries((list) -> list.get(0)));
    assertThat(writer.writeToString(List.of(map))).isEqualTo("""
            {"a":"A","b":123,"c":true}""");
  }

  @Test
  void ofWhenNoMembersAddedThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
    })).withMessage("No members have been added");
  }

  @Test
  void ofWhenOneContributesPairByNameAndOneHasNoNameThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
              members.add("Infra", "App");
              members.from("alone");
            }))
            .withMessage("Member at index 1 does not contribute a named pair, "
                    + "ensure that all members have a name or call an appropriate 'using' method");
  }

  @Test
  void ofWhenOneContributesPairByUsingPairsAndOneHasNoNameThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
              members.from(Map.of("Infra", "App")).usingPairs(Map::forEach);
              members.from("alone");
            }))
            .withMessage("Member at index 1 does not contribute a named pair, "
                    + "ensure that all members have a name or call an appropriate 'using' method");
  }

  @Test
  void ofWhenOneContributesPairByUsingMembersAndOneHasNoNameThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
              members.from(PERSON).usingMembers((personMembers) -> {
                personMembers.add("first", Person::firstName);
                personMembers.add("last", Person::firstName);
              });
              members.from("alone");
            }))
            .withMessage("Member at index 1 does not contribute a named pair, "
                    + "ensure that all members have a name or call an appropriate 'using' method");
  }

  private static String quoted(String value) {
    return "\"" + value + "\"";
  }

  private static <T> JsonWriter<T> ofFormatString(String json) {
    return (instance, out) -> out.append(json.formatted(instance));
  }

  /**
   * Tests for {@link JsonWriter#standard()}.
   */
  @Nested
  class StandardWriterTests {

    @Test
    void whenPrimitive() {
      assertThat(write(null)).isEqualTo("null");
      assertThat(write(123)).isEqualTo("123");
      assertThat(write(true)).isEqualTo("true");
      assertThat(write("test")).isEqualTo(quoted("test"));
    }

    @Test
    void whenMap() {
      assertThat(write(Map.of("infra", "app"))).isEqualTo("""
              {"infra":"app"}""");
    }

    @Test
    void whenArray() {
      assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
    }

    private <T> String write(T instance) {
      return JsonWriter.standard().writeToString(instance);
    }

  }

  /**
   * Tests for {@link Members} and {@link JsonWriter.Member}.
   */
  @Nested
  class MembersTest {

    @Test
    void whenNotNull() {
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add().whenNotNull());
      assertThat(writer.writeToString("test")).isEqualTo(quoted("test"));
      assertThat(writer.writeToString(null)).isEmpty();
    }

    @Test
    void whenNotNullExtracted() {
      Person personWithNull = new Person("Infra", null, 10);
      JsonWriter<Person> writer = JsonWriter.of((members) -> members.add().whenNotNull(Person::lastName));
      assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Infra App (10)"));
      assertThat(writer.writeToString(personWithNull)).isEmpty();
    }

    @Test
    void whenHasLength() {
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add().whenHasLength());
      assertThat(writer.writeToString("test")).isEqualTo(quoted("test"));
      assertThat(writer.writeToString("")).isEmpty();
      assertThat(writer.writeToString(null)).isEmpty();
    }

    @Test
    void whenHasLengthOnNonString() {
      JsonWriter<StringBuilder> writer = JsonWriter.of((members) -> members.add().whenHasLength());
      assertThat(writer.writeToString(new StringBuilder("test"))).isEqualTo(quoted("test"));
      assertThat(writer.writeToString(new StringBuilder())).isEmpty();
      assertThat(writer.writeToString(null)).isEmpty();
    }

    @Test
    void whenNotEmpty() {
      JsonWriter<Object> writer = JsonWriter.of((members) -> members.add().whenNotEmpty());
      assertThat(writer.writeToString(List.of("a"))).isEqualTo("""
              ["a"]""");
      assertThat(writer.writeToString(Collections.emptyList())).isEmpty();
      assertThat(writer.writeToString(new Object[] {})).isEmpty();
      assertThat(writer.writeToString(new int[] {})).isEmpty();
      assertThat(writer.writeToString(null)).isEmpty();
    }

    @Test
    void whenNot() {
      JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().whenNot(List::isEmpty));
      assertThat(writer.writeToString(List.of("a"))).isEqualTo("""
              ["a"]""");
      assertThat(writer.writeToString(Collections.emptyList())).isEmpty();
    }

    @Test
    void when() {
      JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().when(List::isEmpty));
      assertThat(writer.writeToString(List.of("a"))).isEmpty();
      assertThat(writer.writeToString(Collections.emptyList())).isEqualTo("[]");
    }

    @Test
    void chainedPredicates() {
      Set<String> banned = Set.of("Infra", "App");
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add()
              .whenHasLength()
              .whenNot(banned::contains)
              .whenNot((string) -> string.length() <= 2));
      assertThat(writer.writeToString("")).isEmpty();
      assertThat(writer.writeToString("a")).isEmpty();
      assertThat(writer.writeToString("App")).isEmpty();
      assertThat(writer.writeToString("JSON")).isEqualTo(quoted("JSON"));
    }

    @Test
    void as() {
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add().as(Integer::valueOf));
      assertThat(writer.writeToString("123")).isEqualTo("123");
    }

    @Test
    void asWhenValueIsNullDoesNotCallAdapter() {
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add().as((value) -> {
        throw new RuntimeException("bad");
      }));
      writer.writeToString(null);
    }

    @Test
    void chainedAs() {
      Function<Integer, Boolean> booleanAdapter = (integer) -> integer != 0;
      JsonWriter<String> writer = JsonWriter
              .of((members) -> members.add().as(Integer::valueOf).as(booleanAdapter));
      assertThat(writer.writeToString("0")).isEqualTo("false");
      assertThat(writer.writeToString("1")).isEqualTo("true");
    }

    @Test
    void chainedAsAndPredicates() {
      Function<Integer, Boolean> booleanAdapter = (integer) -> integer != 0;
      JsonWriter<String> writer = JsonWriter.of((members) -> members.add()
              .whenNot(String::isEmpty)
              .as(Integer::valueOf)
              .when((integer) -> integer < 2)
              .as(booleanAdapter));
      assertThat(writer.writeToString("")).isEmpty();
      assertThat(writer.writeToString("0")).isEqualTo("false");
      assertThat(writer.writeToString("1")).isEqualTo("true");
      assertThat(writer.writeToString("2")).isEmpty();
    }

    @Test
    void usingExtractedPairsWithExtractor() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("a", "A");
      map.put("b", "B");
      PairExtractor<Map.Entry<String, Object>> extractor = PairExtractor.of(Map.Entry::getKey,
              Map.Entry::getValue);
      JsonWriter<Map<String, Object>> writer = JsonWriter
              .of((members) -> members.add().as(Map::entrySet).usingExtractedPairs(Set::forEach, extractor));
      assertThat(writer.writeToString(map)).isEqualTo("""
              {"a":"A","b":"B"}""");
    }

    @Test
    void usingExtractedPairs() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("a", "A");
      map.put("b", "B");
      Function<Map.Entry<String, Object>, String> nameExtractor = Map.Entry::getKey;
      Function<Map.Entry<String, Object>, Object> valueExtractor = Map.Entry::getValue;
      JsonWriter<Map<String, Object>> writer = JsonWriter.of((members) -> members.add()
              .as(Map::entrySet)
              .usingExtractedPairs(Set::forEach, nameExtractor, valueExtractor));
      assertThat(writer.writeToString(map)).isEqualTo("""
              {"a":"A","b":"B"}""");
    }

    @Test
    void usingPairs() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("a", "A");
      map.put("b", "B");
      JsonWriter<Map<String, Object>> writer = JsonWriter.of((members) -> members.add().usingPairs(Map::forEach));
      assertThat(writer.writeToString(map)).isEqualTo("""
              {"a":"A","b":"B"}""");
    }

    @Test
    void usingPairsWhenAlreadyDeclaredThrowsException() {
      assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((
                      members) -> members.from(Collections.emptyMap()).usingPairs(Map::forEach).usingPairs(Map::forEach)))
              .withMessage("Pairs cannot be declared multiple times");
    }

    @Test
    void usingPairsWhenUsingMembersThrowsException() {
      assertThatIllegalStateException()
              .isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
                      .usingMembers((mapMembers) -> mapMembers.add("test"))
                      .usingPairs(Map::forEach)))
              .withMessage("Pairs cannot be declared when using members");
    }

    @Test
    void usingMembers() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.add("personOne", Couple::person1).usingMembers((personMembers) -> {
          personMembers.add("fn", Person::firstName);
          personMembers.add("ln", Person::lastName);
        });
        members.add("personTwo", Couple::person2).usingMembers((personMembers) -> {
          personMembers.add("details", Person::toString);
          personMembers.add("eldest", true);
        });
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"personOne":{"fn":"Infra","ln":"App"},""" + """
              "personTwo":{"details":"Infra Framework (20)","eldest":true}}""");
    }

    @Test
    void usingMembersWithoutName() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.add("version", 1);
        members.from(Couple::person1)
                .usingMembers((personMembers) -> personMembers.add("one", Person::toString));
        members.from(Couple::person2)
                .usingMembers((personMembers) -> personMembers.add("two", Person::toString));
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"version":1,"one":"Infra App (10)","two":"Infra Framework (20)"}""");
    }

    @Test
    void usingMembersWithoutNameInMember() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> members.add("only", Couple::person2)
              .usingMembers((personMembers) -> personMembers.from(Person::toString)));
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"only":"Infra Framework (20)"}""");
    }

    @Test
    void usingMemebersWithoutNameAtAll() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> members.from(Couple::person2)
              .usingMembers((personMembers) -> personMembers.from(Person::toString)));
      assertThat(writer.writeToString(couple)).isEqualTo(quoted("Infra Framework (20)"));
    }

    @Test
    void usingMembersWhenAlreadyDeclaredThrowsException() {
      assertThatIllegalStateException()
              .isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
                      .usingMembers((mapMembers) -> mapMembers.add("test"))
                      .usingMembers((mapMembers) -> mapMembers.add("test"))))
              .withMessage("Members cannot be declared multiple times");
    }

    @Test
    void usingMembersWhenUsingPairsThrowsException() {
      assertThatIllegalStateException()
              .isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
                      .usingPairs(Map::forEach)
                      .usingMembers((mapMembers) -> mapMembers.add("test"))))
              .withMessage("Members cannot be declared when using pairs");
    }

  }

  /**
   * Tests for {@link MemberPath}.
   */
  @Nested
  class MemberPathTests {

    @Test
    void createWhenIndexAndNamedThrowException() {
      assertThatIllegalArgumentException().isThrownBy(() -> new MemberPath(null, "app", 0))
              .withMessage("'name' and 'index' cannot be mixed");
      assertThatIllegalArgumentException().isThrownBy(() -> new MemberPath(null, null, -1))
              .withMessage("'name' and 'index' cannot be mixed");
    }

    @Test
    void toStringReturnsUsefulString() {
      assertThat(MemberPath.ROOT).hasToString("");
      MemberPath infra = new MemberPath(MemberPath.ROOT, "infra", MemberPath.UNINDEXED);
      MemberPath springDotBoot = new MemberPath(infra, "app", MemberPath.UNINDEXED);
      MemberPath springZero = new MemberPath(infra, null, 0);
      MemberPath springZeroDotBoot = new MemberPath(springZero, "app", MemberPath.UNINDEXED);
      assertThat(infra).hasToString("infra");
      assertThat(springDotBoot).hasToString("infra.app");
      assertThat(springZero).hasToString("infra[0]");
      assertThat(springZeroDotBoot).hasToString("infra[0].app");
    }

    @Test
    void childWithNameCreatesChild() {
      assertThat(MemberPath.ROOT.child("infra").child("app")).hasToString("infra.app");
    }

    @Test
    void childWithNameWhenNameSpecialChars() {
      assertThat(MemberPath.ROOT.child("infra.io").child("app")).hasToString("infra\\.io.app");
      assertThat(MemberPath.ROOT.child("infra[io]").child("app")).hasToString("infra\\[io\\].app");
      assertThat(MemberPath.ROOT.child("infra.[io]").child("app")).hasToString("infra\\.\\[io\\].app");
      assertThat(MemberPath.ROOT.child("infra\\io").child("app")).hasToString("infra\\\\io.app");
      assertThat(MemberPath.ROOT.child("infra.\\io").child("app")).hasToString("infra\\.\\\\io.app");
      assertThat(MemberPath.ROOT.child("infra[\\io]").child("app")).hasToString("infra\\[\\\\io\\].app");
      assertThat(MemberPath.ROOT.child("123").child("app")).hasToString("123.app");
      assertThat(MemberPath.ROOT.child("1.2.3").child("app")).hasToString("1\\.2\\.3.app");
    }

    @Test
    void childWithIndexCreatesChild() {
      assertThat(MemberPath.ROOT.child("infra").child(0)).hasToString("infra[0]");
    }

    @Test
    void ofParsesPaths() {
      assertOfFromToString(MemberPath.ROOT.child("infra").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra").child(0));
      assertOfFromToString(MemberPath.ROOT.child("infra.io").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra[io]").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra.[io]").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra\\io").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra.\\io").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("infra[\\io]").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("123").child("app"));
      assertOfFromToString(MemberPath.ROOT.child("1.2.3").child("app"));
    }

    private void assertOfFromToString(MemberPath path) {
      assertThat(MemberPath.of(path.toString())).isEqualTo(path);
    }

  }

  /**
   * Tests for {@link Members#applyingPathFilter(java.util.function.Predicate)}.
   */
  @Nested
  class PathFilterTests {

    @Test
    void filteringMember() {
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members.applyingPathFilter((path) -> path.name().equals("first"));
      });
      assertThat(writer.writeToString(new Person("infra", "app", 10))).isEqualTo("""
              {"last":"app"}""");
    }

    @Test
    void filteringInMap() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingPathFilter((path) -> path.name().equals("infra"));

      });
      assertThat(writer.writeToString(Map.of("infra", "app", "test", "test"))).isEqualTo("""
              {"test":"test"}""");
    }

  }

  /**
   * Tests for {@link NameProcessor}.
   */
  @Nested
  class NameProcessorTests {

    @Test
    void processNameWhenSimpleValue() {
      JsonWriter<String> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString("test")).isEqualTo("\"test\"");
    }

    @Test
    void processNameWhenMember() {
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString(new Person("infra", "app", 10))).isEqualTo("""
              {"FIRST":"infra","LAST":"app"}""");
    }

    @Test
    void processNameWhenInMap() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"INFRA":"app"}""");
    }

    @Test
    void processNameWhenInNestedMap() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString(Map.of("test", Map.of("infra", "app")))).isEqualTo("""
              {"TEST":{"INFRA":"app"}}""");
    }

    @Test
    void processNameWhenInPairs() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add().usingPairs(Map::forEach);
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"INFRA":"app"}""");
    }

    @Test
    void processNameWhenHasNestedMembers() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.from(Couple::person1)
                .usingMembers((personMembers) -> personMembers.add("one", Person::toString));
        members.from(Couple::person2)
                .usingMembers((personMembers) -> personMembers.add("two", Person::toString));
        members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"ONE":"Infra App (10)","TWO":"Infra Framework (20)"}""");
    }

    @Test
    void processNameWhenHasNestedMembersWithAdditionalValueProcessor() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.from(Couple::person1)
                .usingMembers((personMembers) -> personMembers.add("one", Person::toString));
        members.from(Couple::person2).usingMembers((personMembers) -> {
          personMembers.add("two", Person::toString);
          personMembers.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
        });
        members.applyingNameProcessor(NameProcessor.of((name) -> name + "!"));
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"one!":"Infra App (10)","TWO!":"Infra Framework (20)"}""");
    }

    @Test
    void processNameWhenDeeplyNestedUsesCompoundPaths() {
      List<String> paths = new ArrayList<>();
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.add("one", Couple::person1).usingMembers((personMembers) -> {
          personMembers.add("first", Person::firstName);
          personMembers.add("last", Person::lastName);
        });
        members.add("two", Couple::person2).usingMembers((personMembers) -> {
          personMembers.add("first", Person::firstName);
          personMembers.add("last", Person::lastName);
        });
        members.applyingNameProcessor((path, existingName) -> {
          paths.add(path.toString());
          return existingName;
        });
      });
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      writer.writeToString(couple);
      assertThat(paths).containsExactly("one", "one.first", "one.last", "two", "two.first", "two.last");
    }

    @Test
    void processNameWhenReturnsNullThrowsException() {
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members
                .applyingNameProcessor((path, existingName) -> !"first".equals(existingName) ? existingName : null);
      });
      assertThatIllegalStateException().isThrownBy(() -> writer.writeToString(new Person("infra", "app", 10)))
              .withMessageContaining("NameProcessor")
              .withMessageContaining("returned an empty result");
    }

    @Test
    void processNameWhenReturnsEmptyStringThrowsException() {
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members
                .applyingNameProcessor((path, existingName) -> !"first".equals(existingName) ? existingName : "");
      });
      assertThatIllegalStateException().isThrownBy(() -> writer.writeToString(new Person("infra", "app", 10)))
              .withMessageContaining("NameProcessor")
              .withMessageContaining("returned an empty result");
    }

  }

  /**
   * Tests for {@link ValueProcessor}.
   */
  @Nested
  class ValueProcessorTests {

    @Test
    void of() {
      ValueProcessor<String> processor = ValueProcessor.of(String::toUpperCase);
      assertThat(processor.processValue(null, "test")).isEqualTo("TEST");
    }

    @Test
    void ofWhenNull() {
      assertThatIllegalArgumentException().isThrownBy(() -> ValueProcessor.of(null))
              .withMessage("'action' is required");
    }

    @Test
    void whenHasPathWithStringWhenPathMatches() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase).whenHasPath("foo");
      assertThat(processor.processValue(MemberPath.ROOT.child("foo"), "test")).isEqualTo("TEST");
    }

    @Test
    void whenHasPathWithStringWhenPathDoesNotMatch() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase).whenHasPath("foo");
      assertThat(processor.processValue(MemberPath.ROOT.child("bar"), "test")).isEqualTo("test");
    }

    @Test
    void whenHasPathWithPredicateWhenPathMatches() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase)
              .whenHasPath((path) -> path.toString().startsWith("f"));
      assertThat(processor.processValue(MemberPath.ROOT.child("foo"), "test")).isEqualTo("TEST");
    }

    @Test
    void whenHasPathWithPredicateWhenPathDoesNotMatch() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase)
              .whenHasPath((path) -> path.toString().startsWith("f"));
      assertThat(processor.processValue(MemberPath.ROOT.child("bar"), "test")).isEqualTo("test");
    }

    @Test
    void whenInstanceOfWhenInstanceMatches() {
      ValueProcessor<Object> processor = ValueProcessor.of((value) -> value.toString().toUpperCase(Locale.ROOT))
              .whenInstanceOf(String.class);
      assertThat(processor.processValue(null, "test")).hasToString("TEST");
    }

    @Test
    void whenInstanceOfWhenInstanceDoesNotMatch() {
      ValueProcessor<Object> processor = ValueProcessor.of((value) -> value.toString().toUpperCase(Locale.ROOT))
              .whenInstanceOf(String.class);
      assertThat(processor.processValue(null, new StringBuilder("test"))).hasToString("test");
    }

    @Test
    void whenWhenPredicateMatches() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase).when("test"::equals);
      assertThat(processor.processValue(null, "test")).isEqualTo("TEST");
    }

    @Test
    void whenWhenPredicateDoesNotMatch() {
      ValueProcessor<String> processor = ValueProcessor.<String>of(String::toUpperCase).when("test"::equals);
      assertThat(processor.processValue(null, "other")).isEqualTo("other");
    }

    @Test
    void processValueWhenSimpleValue() {
      JsonWriter<String> writer = simpleWriterWithUppercaseProcessor();
      assertThat(writer.writeToString("test")).isEqualTo("\"TEST\"");
    }

    @Test
    void processValueWhenMemberValue() {
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize));
      });
      assertThat(writer.writeToString(new Person("infra", "app", 10))).isEqualTo("""
              {"first":"Infra","last":"App"}""");
    }

    @Test
    void processValueWhenInMap() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"infra":"App"}""");
    }

    @Test
    void processValueWhenInNestedMap() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize));
      });
      assertThat(writer.writeToString(Map.of("test", Map.of("infra", "app")))).isEqualTo("""
              {"test":{"infra":"App"}}""");
    }

    @Test
    void processValueWhenInPairs() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add().usingPairs(Map::forEach);
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"infra":"App"}""");
    }

    @Test
    void processValueWhenCalledWithMultipleTypesIgnoresLambdaErrors() {
      JsonWriter<Object> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize));
      });
      assertThat(writer.writeToString("infra")).isEqualTo("\"Infra\"");
      assertThat(writer.writeToString(123)).isEqualTo("123");
      assertThat(writer.writeToString(true)).isEqualTo("true");
    }

    @Test
    void processValueWhenLimitedToPath() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(ValueProcessor.of(StringUtils::capitalize).whenHasPath("infra"));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"infra":"App"}""");
      assertThat(writer.writeToString(Map.of("app", "infra"))).isEqualTo("""
              {"app":"infra"}""");
    }

    @Test
    void processValueWhen() {
      JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(
                ValueProcessor.of(StringUtils::capitalize).when((candidate) -> candidate.startsWith("b")));
      });
      assertThat(writer.writeToString(Map.of("infra", "app"))).isEqualTo("""
              {"infra":"app"}""");
      assertThat(writer.writeToString(Map.of("app", "infra"))).isEqualTo("""
              {"app":"infra"}""");
    }

    @Test
    void processValueWhenHasNestedMembers() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.from(Couple::person1)
                .usingMembers((personMembers) -> personMembers.add("one", Person::toString));
        members.from(Couple::person2)
                .usingMembers((personMembers) -> personMembers.add("two", Person::toString));
        members.applyingValueProcessor(ValueProcessor.of(String.class, String::toUpperCase));
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"one":"INFRA APP (10)","two":"INFRA FRAMEWORK (20)"}""");
    }

    @Test
    void processValueWhenHasNestedMembersWithAdditionalValueProcessor() {
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.from(Couple::person1)
                .usingMembers((personMembers) -> personMembers.add("one", Person::toString));
        members.from(Couple::person2).usingMembers((personMembers) -> {
          personMembers.add("two", Person::toString);
          personMembers.applyingValueProcessor(ValueProcessor.of(String.class, (item) -> item + "!"));
        });
        members.applyingValueProcessor(ValueProcessor.of(String.class, String::toUpperCase));
      });
      assertThat(writer.writeToString(couple)).isEqualTo("""
              {"one":"INFRA APP (10)","two":"INFRA FRAMEWORK (20)!"}""");
    }

    @Test
    void processValueWhenDeeplyNestedUsesCompoundPaths() {
      List<String> paths = new ArrayList<>();
      JsonWriter<Couple> writer = JsonWriter.of((members) -> {
        members.add("one", Couple::person1).usingMembers((personMembers) -> {
          personMembers.add("first", Person::firstName);
          personMembers.add("last", Person::lastName);
        });
        members.add("two", Couple::person2).usingMembers((personMembers) -> {
          personMembers.add("first", Person::firstName);
          personMembers.add("last", Person::lastName);
        });
        members.applyingValueProcessor((path, value) -> {
          paths.add(path.toString());
          return value;
        });
      });
      Couple couple = new Couple(PERSON, new Person("Infra", "Framework", 20));
      writer.writeToString(couple);
      assertThat(paths).containsExactly("one", "one.first", "one.last", "two", "two.first", "two.last");
    }

    @Test
    void processValueWhenUsingListUsesIndexedPaths() {
      List<String> paths = new ArrayList<>();
      JsonWriter<List<String>> writer = JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor((path, value) -> {
          paths.add(path.toString());
          return value;
        });
      });
      writer.writeToString(List.of("a", "b", "c"));
      assertThat(paths).containsExactly("", "[0]", "[1]", "[2]");
    }

    @Test
    void processValueUsesUnprocessedNameInPath() {
      List<String> paths = new ArrayList<>();
      JsonWriter<Person> writer = JsonWriter.of((members) -> {
        members.add("first", Person::firstName);
        members.add("last", Person::lastName);
        members.applyingValueProcessor((path, value) -> {
          paths.add(path.toString());
          return value;
        });
        members.applyingNameProcessor((path, existingName) -> "the-" + existingName);
      });
      writer.writeToString(PERSON);
      assertThat(paths).containsExactly("first", "last");
    }

    private JsonWriter<String> simpleWriterWithUppercaseProcessor() {
      return JsonWriter.of((members) -> {
        members.add();
        members.applyingValueProcessor(ValueProcessor.of(String.class, String::toUpperCase));
      });
    }

  }

  record Person(String firstName, String lastName, int age) {

    @Override
    public String toString() {
      return "%s %s (%s)".formatted(this.firstName, this.lastName, this.age);
    }

  }

  record Couple(Person person1, Person person2) {

  }

}
