/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.jdbc.persistence.model.UserModel;
import cn.taketoday.lang.Constant;

import static cn.taketoday.beans.BeanProperty.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/31 11:16
 */
class ColumnNameDiscoverTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.FIELD })
  public @interface MyColumn {

    @AliasFor("name")
    String value() default Constant.BLANK;

    @AliasFor("value")
    String name() default Constant.BLANK;

    String myValue() default "";

  }

  @Test
  void forColumnAnnotation() {
    class Model {
      @Column("test-name")
      int test;
    }

    ColumnNameDiscover nameDiscover = ColumnNameDiscover.forColumnAnnotation();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age")))
            .isEqualTo(null);

    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test")))
            .isEqualTo("test-name");

  }

  @Test
  void forAnnotation() {
    class Model {
      @MyColumn("test-name")
      int test;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = ColumnNameDiscover.forAnnotation(MyColumn.class);
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test"))).isEqualTo("test-name");

    var myValueDiscover = ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue");
    assertThat(myValueDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
    assertThat(myValueDiscover.getColumnName(valueOf(Model.class, "test"))).isEqualTo(null);
    assertThat(myValueDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

  }

  @Test
  void camelCaseToUnderscore() {
    class Model {

      int testName;
    }

    var nameDiscover = ColumnNameDiscover.camelCaseToUnderscore();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobile_phone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("test_name");

  }

  @Test
  void forPropertyName() {
    class Model {

      int testName;
    }

    var nameDiscover = ColumnNameDiscover.forPropertyName();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

  }

  @Test
  void and() {
    class Model {
      int testName;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = ColumnNameDiscover.forColumnAnnotation()
            .and(ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"))
            .and(ColumnNameDiscover.forPropertyName());

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo("age");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

  }

  @Test
  void composite() {
    class Model {
      int testName;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = ColumnNameDiscover.composite(
            ColumnNameDiscover.forColumnAnnotation(),
            ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"),
            ColumnNameDiscover.forPropertyName()
    );

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo("age");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

    // composite null

    nameDiscover = ColumnNameDiscover.composite(
            List.of(ColumnNameDiscover.forColumnAnnotation(),
                    ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"))
    );
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
  }

}