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
package infra.bytecode.commons;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SimpleRemapper}.
 *
 * @author Eric Bruneton
 */
public class SimpleRemapperTest {

  @Test
  void testMapSignature_remapParentOnly_nestedClassExtends() {
    String inputSignature = "LOuter<Ljava/lang/Object;>.Inner;";
    Remapper remapper = new SimpleRemapper(Map.of("Outer", "RenamedOuter"));
    String remappedSignature = remapper.mapSignature(inputSignature, false);
    assertEquals("LRenamedOuter<Ljava/lang/Object;>.Inner;", remappedSignature);
  }

  @Test
  void testMapSignature_remapChildOnly_nestedClassExtends() {
    String inputSignature = "LOuter<Ljava/lang/Object;>.Inner;";
    Remapper remapper = new SimpleRemapper(
            Map.of("Outer$Inner", "Outer$RenamedInner"));

    String remappedSignature = remapper.mapSignature(inputSignature, false);

    assertEquals("LOuter<Ljava/lang/Object;>.RenamedInner;", remappedSignature);
  }

  @Test
  void testMapSignature_remapChildOnly_nestedClassExtends_identifiersWithDollarSign() {
    String inputSignature = "LOuter<Ljava/lang/Object;>.Inner$1;";
    Remapper remapper = new SimpleRemapper(
            Map.of("Outer$Inner$1", "Outer$RenamedInner$1"));

    String remappedSignature = remapper.mapSignature(inputSignature, false);

    assertEquals("LOuter<Ljava/lang/Object;>.RenamedInner$1;", remappedSignature);
  }

  @Test
  void testMapSignature_remapBothParentAndChild_nestedClassExtends() {
    String inputSignature = "LOuter<Ljava/lang/Object;>.Inner;";
    Map<String, String> mapping =
            Map.of("Outer", "RenamedOuter", "Outer$Inner", "RenamedOuter$RenamedInner");
    Remapper remapper = new SimpleRemapper(mapping);

    String remappedSignature = remapper.mapSignature(inputSignature, false);

    assertEquals("LRenamedOuter<Ljava/lang/Object;>.RenamedInner;", remappedSignature);
  }
}
