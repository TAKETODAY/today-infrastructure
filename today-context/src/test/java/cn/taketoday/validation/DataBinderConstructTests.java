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

package cn.taketoday.validation;

import org.junit.jupiter.api.Test;

import java.beans.ConstructorProperties;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataBinder} with constructor binding.
 *
 * @author Rossen Stoyanchev
 */
public class DataBinderConstructTests {

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
      Assert.notNull(optionalParam, "Optional is required");
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

  private static class NestedDataClass {

    private final String param1;

    @Nullable
    private final DataClass nestedParam2;

    public NestedDataClass(String param1, @Nullable DataClass nestedParam2) {
      this.param1 = param1;
      this.nestedParam2 = nestedParam2;
    }

    public String param1() {
      return this.param1;
    }

    @Nullable
    public DataClass nestedParam2() {
      return this.nestedParam2;
    }
  }

  private static class MapValueResolver implements DataBinder.ValueResolver {

    private final Map<String, Object> values;

    private MapValueResolver(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public Object resolveValue(String name, Class<?> type) {
      return values.get(name);
    }

    @Override
    public Set<String> getNames() {
      return this.values.keySet();
    }
  }

}
