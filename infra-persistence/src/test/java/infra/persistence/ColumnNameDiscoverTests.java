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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;
import infra.jdbc.model.UserModel;

import static infra.beans.BeanProperty.valueOf;
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