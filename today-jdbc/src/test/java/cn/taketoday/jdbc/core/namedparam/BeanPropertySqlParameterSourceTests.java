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

package cn.taketoday.jdbc.core.namedparam;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;

import cn.taketoday.jdbc.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class BeanPropertySqlParameterSourceTests {

  @Test
  public void withNullBeanPassedToCtor() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new BeanPropertySqlParameterSource(null));
  }

  @Test
  public void getValueWhereTheUnderlyingBeanHasNoSuchProperty() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> source.getValue("thisPropertyDoesNotExist"));
  }

  @Test
  public void successfulPropertyAccess() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
    assertThat(Arrays.asList(source.getReadablePropertyNames()).contains("name")).isTrue();
    assertThat(Arrays.asList(source.getReadablePropertyNames()).contains("age")).isTrue();
    assertThat(source.getValue("name")).isEqualTo("tb");
    assertThat(source.getValue("age")).isEqualTo(99);
    assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
    assertThat(source.getSqlType("age")).isEqualTo(Types.INTEGER);
  }

  @Test
  public void successfulPropertyAccessWithOverriddenSqlType() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
    source.registerSqlType("age", Types.NUMERIC);
    assertThat(source.getValue("name")).isEqualTo("tb");
    assertThat(source.getValue("age")).isEqualTo(99);
    assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
    assertThat(source.getSqlType("age")).isEqualTo(Types.NUMERIC);
  }

  @Test
  public void hasValueWhereTheUnderlyingBeanHasNoSuchProperty() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
    assertThat(source.hasValue("thisPropertyDoesNotExist")).isFalse();
  }

  @Test
  public void getValueWhereTheUnderlyingBeanPropertyIsNotReadable() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> source.getValue("noOp"));
  }

  @Test
  public void hasValueWhereTheUnderlyingBeanPropertyIsNotReadable() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
    assertThat(source.hasValue("noOp")).isFalse();
  }

  @Test
  public void toStringShowsParameterDetails() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
    assertThat(source.toString())
            .startsWith("BeanPropertySqlParameterSource {")
            .contains("name=tb (type:VARCHAR)")
            .contains("age=99 (type:INTEGER)")
            .endsWith("}");
  }

  @Test
  public void toStringShowsCustomSqlType() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
    source.registerSqlType("name", Integer.MAX_VALUE);
    assertThat(source.toString())
            .startsWith("BeanPropertySqlParameterSource {")
            .contains("name=tb (type:" + Integer.MAX_VALUE + ")")
            .contains("age=99 (type:INTEGER)")
            .endsWith("}");
  }

  @Test
  public void toStringDoesNotShowTypeUnknown() {
    BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
    assertThat(source.toString())
            .startsWith("BeanPropertySqlParameterSource {")
            .contains("beanFactory=null")
            .doesNotContain("beanFactory=null (type:")
            .endsWith("}");
  }

  @SuppressWarnings("unused")
  private static final class NoReadableProperties {

    public void setNoOp(String noOp) {
    }
  }

}
