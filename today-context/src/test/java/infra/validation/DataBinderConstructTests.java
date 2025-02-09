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

package infra.validation;

import org.junit.jupiter.api.Test;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import infra.core.ResolvableType;
import infra.format.support.DefaultFormattingConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataBinder} with constructor binding.
 *
 * @author Rossen Stoyanchev
 */
class DataBinderConstructTests {

  @Test
  void dataClassBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1", "param2", "true"));
    DataBinder binder = initDataBinder(DataClass.class);
    binder.construct(valueResolver);

    DataClass dataClass = getTarget(binder);
    assertThat(dataClass.param1()).isEqualTo("value1");
    assertThat(dataClass.param2()).isEqualTo(true);
    assertThat(dataClass.param3()).isEqualTo(0);
  }

  @Test
  void dataClassBindingWithOptionalParameter() {
    MapValueResolver valueResolver =
            new MapValueResolver(Map.of("param1", "value1", "param2", "true", "optionalParam", "8"));

    DataBinder binder = initDataBinder(DataClass.class);
    binder.construct(valueResolver);

    DataClass dataClass = getTarget(binder);
    assertThat(dataClass.param1()).isEqualTo("value1");
    assertThat(dataClass.param2()).isEqualTo(true);
    assertThat(dataClass.param3()).isEqualTo(8);
  }

  @Test
  void dataClassBindingWithMissingParameter() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1"));
    DataBinder binder = initDataBinder(DataClass.class);
    binder.construct(valueResolver);

    BindingResult bindingResult = binder.getBindingResult();
    assertThat(bindingResult.getAllErrors()).hasSize(1);
    assertThat(bindingResult.getFieldValue("param1")).isEqualTo("value1");
    assertThat(bindingResult.getFieldValue("param2")).isNull();
    assertThat(bindingResult.getFieldValue("param3")).isNull();
  }

  @Test
    // gh-31821
  void dataClassBindingWithNestedOptionalParameterWithMissingParameter() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1"));
    DataBinder binder = initDataBinder(NestedDataClass.class);
    binder.construct(valueResolver);

    NestedDataClass dataClass = getTarget(binder);
    assertThat(dataClass.param1()).isEqualTo("value1");
    assertThat(dataClass.nestedParam2()).isNull();
  }

  @Test
  void dataClassBindingWithConversionError() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("param1", "value1", "param2", "x"));
    DataBinder binder = initDataBinder(DataClass.class);
    binder.construct(valueResolver);

    BindingResult bindingResult = binder.getBindingResult();
    assertThat(bindingResult.getAllErrors()).hasSize(1);
    assertThat(bindingResult.getFieldValue("param1")).isEqualTo("value1");
    assertThat(bindingResult.getFieldValue("param2")).isEqualTo("x");
    assertThat(bindingResult.getFieldValue("param3")).isNull();
  }

  @Test
  void dataClassWithListBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of(
            "dataClassList[0].param1", "value1", "dataClassList[0].param2", "true",
            "dataClassList[1].param1", "value2", "dataClassList[1].param2", "true",
            "dataClassList[2].param1", "value3", "dataClassList[2].param2", "true"));

    DataBinder binder = initDataBinder(DataClassListRecord.class);
    binder.construct(valueResolver);

    DataClassListRecord target = getTarget(binder);
    List<DataClass> list = target.dataClassList();

    assertThat(list).hasSize(3);
    assertThat(list.get(0).param1()).isEqualTo("value1");
    assertThat(list.get(1).param1()).isEqualTo("value2");
    assertThat(list.get(2).param1()).isEqualTo("value3");
  }

  @Test
    // gh-34145
  void dataClassWithListBindingWithNonconsecutiveIndices() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of(
            "dataClassList[0].param1", "value1", "dataClassList[0].param2", "true",
            "dataClassList[1].param1", "value2", "dataClassList[1].param2", "true",
            "dataClassList[3].param1", "value3", "dataClassList[3].param2", "true"));

    DataBinder binder = initDataBinder(DataClassListRecord.class);
    binder.construct(valueResolver);

    DataClassListRecord target = getTarget(binder);
    List<DataClass> list = target.dataClassList();

    assertThat(list.get(0).param1()).isEqualTo("value1");
    assertThat(list.get(1).param1()).isEqualTo("value2");
    assertThat(list.get(3).param1()).isEqualTo("value3");
  }

  @Test
  void dataClassWithMapBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of(
            "dataClassMap[a].param1", "value1", "dataClassMap[a].param2", "true",
            "dataClassMap[b].param1", "value2", "dataClassMap[b].param2", "true",
            "dataClassMap['c'].param1", "value3", "dataClassMap['c'].param2", "true"));

    DataBinder binder = initDataBinder(DataClassMapRecord.class);
    binder.construct(valueResolver);

    DataClassMapRecord target = getTarget(binder);
    Map<String, DataClass> map = target.dataClassMap();

    assertThat(map).hasSize(3);
    assertThat(map.get("a").param1()).isEqualTo("value1");
    assertThat(map.get("b").param1()).isEqualTo("value2");
    assertThat(map.get("c").param1()).isEqualTo("value3");
  }

  @Test
  void dataClassWithArrayBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of(
            "dataClassArray[0].param1", "value1", "dataClassArray[0].param2", "true",
            "dataClassArray[1].param1", "value2", "dataClassArray[1].param2", "true",
            "dataClassArray[2].param1", "value3", "dataClassArray[2].param2", "true"));

    DataBinder binder = initDataBinder(DataClassArrayRecord.class);
    binder.construct(valueResolver);

    DataClassArrayRecord target = getTarget(binder);
    DataClass[] array = target.dataClassArray();

    assertThat(array).hasSize(3);
    assertThat(array[0].param1()).isEqualTo("value1");
    assertThat(array[1].param1()).isEqualTo("value2");
    assertThat(array[2].param1()).isEqualTo("value3");
  }

  @Test
  void simpleListBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("integerList[0]", "1", "integerList[1]", "2"));

    DataBinder binder = initDataBinder(IntegerListRecord.class);
    binder.construct(valueResolver);

    IntegerListRecord target = getTarget(binder);
    assertThat(target.integerList()).containsExactly(1, 2);
  }

  @Test
  void simpleMapBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("integerMap[a]", "1", "integerMap[b]", "2"));

    DataBinder binder = initDataBinder(IntegerMapRecord.class);
    binder.construct(valueResolver);

    IntegerMapRecord target = getTarget(binder);
    assertThat(target.integerMap()).hasSize(2).containsEntry("a", 1).containsEntry("b", 2);
  }

  @Test
  void simpleArrayBinding() {
    MapValueResolver valueResolver = new MapValueResolver(Map.of("integerArray[0]", "1", "integerArray[1]", "2"));

    DataBinder binder = initDataBinder(IntegerArrayRecord.class);
    binder.construct(valueResolver);

    IntegerArrayRecord target = getTarget(binder);
    assertThat(target.integerArray()).containsExactly(1, 2);
  }

  @SuppressWarnings("SameParameterValue")
  private static DataBinder initDataBinder(Class<?> targetType) {
    DataBinder binder = new DataBinder(null);
    binder.setTargetType(ResolvableType.forClass(targetType));
    binder.setConversionService(new DefaultFormattingConversionService());
    return binder;
  }

  @SuppressWarnings("unchecked")
  private static <T> T getTarget(DataBinder dataBinder) {
    assertThat(dataBinder.getBindingResult().getAllErrors()).isEmpty();
    Object target = dataBinder.getTarget();
    assertThat(target).isNotNull();
    return (T) target;
  }

  private static class DataClass {

    @NotNull
    private final String param1;

    private final boolean param2;

    private int param3;

    @ConstructorProperties({ "param1", "param2", "optionalParam" })
    DataClass(String param1, boolean p2, Optional<Integer> optionalParam) {
      this.param1 = param1;
      this.param2 = p2;
      Assert.notNull(optionalParam, "Optional must not be null");
      optionalParam.ifPresent(integer -> this.param3 = integer);
    }

    public String param1() {
      return this.param1;
    }

    public boolean param2() {
      return this.param2;
    }

    public int param3() {
      return this.param3;
    }
  }

  static class NestedDataClass {

    private final String param1;

    private final @Nullable DataClass nestedParam2;

    public NestedDataClass(String param1, @Nullable DataClass nestedParam2) {
      this.param1 = param1;
      this.nestedParam2 = nestedParam2;
    }

    public String param1() {
      return this.param1;
    }

    public @Nullable DataClass nestedParam2() {
      return this.nestedParam2;
    }
  }

  private record DataClassListRecord(List<DataClass> dataClassList) {
  }

  private record DataClassMapRecord(Map<String, DataClass> dataClassMap) {
  }

  private record DataClassArrayRecord(DataClass[] dataClassArray) {
  }

  private record IntegerListRecord(List<Integer> integerList) {
  }

  private record IntegerMapRecord(Map<String, Integer> integerMap) {
  }

  private record IntegerArrayRecord(Integer[] integerArray) {
  }

  private record MapValueResolver(Map<String, Object> map) implements DataBinder.ValueResolver {

    @Override
    public Object resolveValue(String name, Class<?> type) {
      return map.get(name);
    }

    @Override
    public Set<String> getNames() {
      return this.map.keySet();
    }
  }

}
