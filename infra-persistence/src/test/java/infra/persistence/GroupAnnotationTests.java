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

import java.util.ArrayList;
import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.annotation.Connector;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Group;
import infra.persistence.annotation.GroupOR;
import infra.persistence.annotation.OR;
import infra.persistence.annotation.Where;
import infra.persistence.platform.GenericPlatform;
import infra.persistence.platform.Platform;
import infra.persistence.sql.LogicalOperator;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link Group @Group} collapses same-named properties into one nested
 * condition. Inside a group members join with {@code AND} by default and their
 * own {@link OR @OR} switches to {@code OR}, exactly like top-level conditions.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class GroupAnnotationTests {

  private final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  private final EntityMetadata userModelMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  private final List<PropertyConditionStrategy> strategies =
          new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  @Test
  void group_shouldRenderNestedOrWithAndConnector() {
    GroupedQuery query = new GroupedQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND (status2 = ? OR status3 <= ?)");

    ArrayList<Restriction> restrictions = new ArrayList<>();
    exampleQuery.collectRestrictions(userModelMetadata, restrictions);
    assertThat(restrictions).hasSize(1);
    ConditionTree tree = (ConditionTree) restrictions.get(0);
    assertThat(tree.occurrences()).hasSize(2);
  }

  @Test
  void groupConnector_shouldOverrideTheGroupLink() {
    OrLinkedGroupQuery query = new OrLinkedGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE (status > ? OR (status2 = ? AND status3 <= ?))");
  }

  @Test
  void memberOr_shouldJoinInsideTheGroup() {
    MemberOrGroupQuery query = new MemberOrGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    // the @OR on status3 makes the second member join with OR
    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND (status2 = ? OR status3 <= ?)");
  }

  @Test
  void nestedGroups_shouldRenderToAnyDepth() {
    NestedGroupQuery query = new NestedGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;
    query.status4 = 4;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND (status2 = ? AND (status3 <= ? AND status4 > ?))");
  }

  @Test
  void nestedGroups_shouldMixConnectorsAcrossLevels() {
    MixedNestedGroupQuery query = new MixedNestedGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;
    query.status4 = 4;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND (status2 = ? AND (status3 <= ? OR status4 > ?))");
  }

  @Test
  void multipleGroups_shouldMixAtTopLevel() {
    MultiGroupQuery query = new MultiGroupQuery();
    query.name = "TODAY";
    query.age = 1;
    query.email = "x";
    query.password = 2;
    query.id = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE ((name = ? AND (age > ? AND email like ?))"
                    + " OR (password > ? AND id > ?))");
  }

  @Test
  void nestedGroup_withOrLinkOnInnerGroup_shouldRenderGroupOfGroup() {
    GroupOfGroupQuery query = new GroupOfGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;
    query.status4 = 4;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND (status2 = ? OR (status3 <= ? AND status4 > ?))");
  }

  @Test
  void deepNesting_withOrLinksAtEveryLevel_shouldRender() {
    DeepNestedGroupQuery query = new DeepNestedGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;
    query.status4 = 4;
    query.status5 = 5;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(new GenericPlatform(), userModelMetadata, sqlBuffer);

    // g.h joins the preceding member of g with its connector OR;
    // inside h, status4 ORs with status3 while status5 is ANDed
    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE status > ? AND"
                    + " (status2 = ? OR ((status3 <= ? OR status4 > ?) AND status5 < ?))");
  }

  @EntityRef(UserModel.class)
  static class NestedGroupQuery {

    @Where("status > ?")
    public int status;

    @Group("g")
    @Where(operator = " = ")
    public int status2;

    @Group("g.h")
    @Where(operator = " <= ")
    public int status3;

    @Group("g.h.i")
    @Where(operator = " > ")
    public int status4;

  }

  @EntityRef(UserModel.class)
  static class MixedNestedGroupQuery {

    @Where("status > ?")
    public int status;

    @Group("g")
    @Where(operator = " = ")
    public int status2;

    @Group("g.h")
    @Where(operator = " <= ")
    public int status3;

    @OR
    @Group("g.h")
    @Where(operator = " > ")
    public int status4;

  }

  @Test
  void groupConnectorXor_shouldJoinGroupWithXor() {
    XorLinkedGroupQuery query = new XorLinkedGroupQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, strategies);
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.appendWhereClause(Platform.mysql(), userModelMetadata, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE (status > ? XOR (status2 = ? AND status3 <= ?))");
  }

  @EntityRef(UserModel.class)
  static class GroupedQuery {

    @Where("status > ?")
    public int status;

    @Group("state")
    @Where(operator = " = ")
    public int status2;

    @Group("state")
    @OR
    @Where(operator = " <= ")
    public int status3;

  }

  @EntityRef(UserModel.class)
  static class XorLinkedGroupQuery {

    @Where("status > ?")
    public int status;

    @Connector(value = LogicalOperator.XOR, group = true)
    @Group("state")
    @Where(operator = " = ")
    public int status2;

    @Group("state")
    @Where(operator = " <= ")
    public int status3;

  }

  @EntityRef(UserModel.class)
  static class OrLinkedGroupQuery {

    @Where("status > ?")
    public int status;

    @GroupOR
    @Group("state")
    @Where(operator = " = ")
    public int status2;

    @Group("state")
    @Where(operator = " <= ")
    public int status3;

  }

  @EntityRef(UserModel.class)
  static class MemberOrGroupQuery {

    @Where("status > ?")
    public int status;

    @Group("state")
    @Where(operator = " = ")
    public int status2;

    @Group("state")
    @OR
    @Where(operator = " <= ")
    public int status3;

  }

  @EntityRef(UserModel.class)
  static class MultiGroupQuery {

    @Where("name = ?")
    public String name;

    @Group("g1")
    @Where(operator = " > ")
    public int age;

    @Group("g1")
    @Where(operator = " like ")
    public String email;

    @GroupOR
    @Group("g2")
    @Where(operator = " > ")
    public int password;

    @Group("g2")
    @Where(operator = " > ")
    public int id;

  }

  @EntityRef(UserModel.class)
  static class GroupOfGroupQuery {

    @Where("status > ?")
    public int status;

    @Group("g")
    @Where(operator = " = ")
    public int status2;

    @GroupOR
    @Group("g.h")
    @Where(operator = " <= ")
    public int status3;

    @Group("g.h")
    @Where(operator = " > ")
    public int status4;

  }

  @EntityRef(UserModel.class)
  static class DeepNestedGroupQuery {

    @Where("status > ?")
    public int status;

    @Group("g")
    @Where(operator = " = ")
    public int status2;

    @GroupOR
    @Group("g.h")
    @Where(operator = " <= ")
    public int status3;

    @Group("g.h")
    @OR
    @Where(operator = " > ")
    public int status4;

    @Group("g.h")
    @Where(operator = " < ")
    public int status5;

  }

}