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

import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.GroupExpression;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.PrefixLike;
import infra.persistence.annotation.SuffixLike;
import infra.persistence.annotation.Where;
import infra.persistence.annotation.WhereIsNull;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restrictions;
import infra.persistence.support.PropertyCondition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link GroupExpression @GroupExpression} parses boolean expressions
 * over property names and builds the correct WHERE clause.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class GroupExpressionTests {

  private final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  private final List<PropertyConditionStrategy> strategies =
          new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  private String renderWhere(Object example) {
    EntityMetadata metadata = metadataFactory.getEntityMetadata(example.getClass());
    StringBuilder sql = new StringBuilder();
    new ExampleQuery(metadataFactory, example, strategies)
            .appendWhereClause(Platform.mysql(), metadata, sql);
    return sql.toString();
  }

  @Test
  void allAnd_flattensWithoutRedundantParens() {
    AllAnd query = new AllAnd();
    query.name = "TODAY";
    query.age = 10;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? AND age = ?");
  }

  @Test
  void orWithParens_nestsParenthesizedGroup() {
    OrWithParens query = new OrWithParens();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? OR (age = ? AND email = ?)");
  }

  @Test
  void precedence_andBindsTighterThanOr() {
    PrecedenceQuery query = new PrecedenceQuery();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    // a OR b AND c → a OR b AND c (no parentheses in the source)
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? OR age = ? AND email = ?");
  }

  @Test
  void userExpression_rendersComplexNesting() {
    UserExpression query = new UserExpression();
    query.name = "TODAY";
    query.age = 10;
    query.id = 1;
    query.password = "secret";
    query.email = "x";
    query.avatar = "pic";

    // a AND b OR (c AND d) AND (s OR b or f)
    // → (a AND b) OR ((c AND d) AND (s OR b OR f))
    String sql = renderWhere(query);
    assertThat(sql).contains("name = ?");
    assertThat(sql).contains("age = ?");
    assertThat(sql).contains("id = ?");
    assertThat(sql).contains("password = ?");
    assertThat(sql).contains("email = ?");
    assertThat(sql).contains("avatar = ?");
    assertThat(sql).contains("OR");
    assertThat(sql).contains("AND");
  }

  @Test
  void nullLeaf_isSkipped() {
    NullLeaf query = new NullLeaf();
    query.name = "TODAY";
    query.age = null;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ?");
  }

  @Test
  void allNullGroup_isRemoved() {
    AllNullGroup query = new AllNullGroup();
    query.name = "TODAY";
    query.age = null;
    query.email = null;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ?");
  }

  @Test
  void xor_rendersXorConnector() {
    XorQuery query = new XorQuery();
    query.name = "TODAY";
    query.age = 10;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? XOR age = ?");
  }

  @Test
  void symbolOperators_areEquivalentToWords() {
    SymbolQuery query = new SymbolQuery();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    // a && b || c → a AND b OR c (no parentheses in the source)
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? AND age = ? OR email = ?");
  }

  @Test
  void singleProperty_rendersWithoutParens() {
    SingleProperty query = new SingleProperty();
    query.name = "TODAY";

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ?");
  }

  @Test
  void deepNesting_rendersArbitraryDepth() {
    DeepNested query = new DeepNested();
    query.name = "n";
    query.age = 1;
    query.email = "e";
    query.id = 2;
    query.password = "p";
    query.avatar = "a";

    // name AND (age OR (email XOR (id AND (password OR avatar))))
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? AND (age = ? OR (email = ? XOR (id = ? AND (password = ? OR avatar = ?))))");
  }

  @Test
  void nonExistentProperty_throws() {
    assertThatThrownBy(() -> renderWhere(new NonExistentPropertyQuery()))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageContaining("nonexistent");
  }

  @Test
  void mixedOperators_shouldRenderEachPredicate() {
    MixedOperators query = new MixedOperators();
    query.age = 25;
    query.name = "TODAY";
    query.id = 7;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE age >= ? AND name like ? OR id <> ?");
  }

  @Test
  void rawFragments_shouldRenderAsIs() {
    RawFragments query = new RawFragments();
    query.active = true;
    query.age = 25;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE is_active = 1 AND age > ?");
  }

  @Test
  void rawAndOperatorMixed_shouldRenderInExpressionOrder() {
    RawAndOperator query = new RawAndOperator();
    query.age = 25;
    query.id = 7;
    query.email = "x";

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE age > ? AND id <> ? OR email = ?");
  }

  // ---- test query classes ----

  @EntityRef(UserModel.class)
  @GroupExpression("name AND age")
  static class AllAnd {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name OR (age AND email)")
  static class OrWithParens {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name OR age AND email")
  static class PrecedenceQuery {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND age OR (id AND password) AND (email OR avatar OR name)")
  static class UserExpression {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("id = ?")
    Integer id;
    @Where("password = ?")
    String password;
    @Where("email = ?")
    String email;
    @Where("avatar = ?")
    String avatar;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND age")
  static class NullLeaf {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND (age AND email)")
  static class AllNullGroup {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name XOR age")
  static class XorQuery {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name && age || email")
  static class SymbolQuery {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name")
  static class SingleProperty {
    @Where("name = ?")
    String name;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND (age OR (email XOR (id AND (password OR avatar))))")
  static class DeepNested {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
    @Where("id = ?")
    Integer id;
    @Where("password = ?")
    String password;
    @Where("avatar = ?")
    String avatar;
  }

  @Test
  void likeFamily_mixedWithAndOrXor_shouldRender() {
    LikeFamilyMixed query = new LikeFamilyMixed();
    query.name = "TODAY";
    query.email = "x";
    query.mobilePhone = "12";
    query.gender = null;

    // name AND (email OR mobilePhone) XOR genderIsNull
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name like ? AND (email like ? OR mobile_phone like ?) XOR gender is null");
  }

  @Test
  void whereIsNull_pairShouldRenderNullnessPredicates() {
    WhereIsNullPair query = new WhereIsNullPair();
    query.deletedAt = null;
    query.status = null;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE deleted_at is null OR status is not null");
  }

  @Test
  void whereIsNull_singleLeaf() {
    SingleIsNull query = new SingleIsNull();
    query.deletedAt = null;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE deleted_at is null");
  }

  @Test
  void not_singleLeaf() {
    NotSingle query = new NotSingle();
    query.name = "TODAY";

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE NOT (name = ?)");
  }

  @Test
  void not_parenthesizedGroup() {
    NotGroup query = new NotGroup();
    query.name = "TODAY";
    query.age = 10;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE NOT (name = ? OR age = ?)");
  }

  @Test
  void not_mixedWithAndOrXor() {
    NotMixed query = new NotMixed();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";
    query.id = 1;

    // name AND NOT (email OR id) XOR age
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? AND NOT (email = ? OR id = ?) XOR age = ?");
  }

  @Test
  void not_bindsTighterThanAnd() {
    NotPrecedence query = new NotPrecedence();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    // NOT name AND email → (NOT name) AND email
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE NOT (name = ?) AND email = ?");
  }

  @Test
  void repeatedProperty_rendersEveryOccurrence() {
    RepeatedProperty query = new RepeatedProperty();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    // name OR (email AND name)
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name = ? OR (email = ? AND name = ?)");
  }

  @Test
  void repeatedProperty_resolvesThePropertyOnlyOnce() {
    CountingStrategy counting = new CountingStrategy();
    RepeatedProperty query = new RepeatedProperty();
    query.name = "TODAY";
    query.age = 10;
    query.email = "x";

    EntityMetadata metadata = metadataFactory.getEntityMetadata(RepeatedProperty.class);
    StringBuilder sql = new StringBuilder();
    new ExampleQuery(metadataFactory, query, List.of(counting))
            .appendWhereClause(Platform.mysql(), metadata, sql);

    assertThat(sql.toString())
            .isEqualTo(" WHERE name = ? OR (email = ? AND name = ?)");
    // name appears twice in the expression but is resolved into one condition only
    assertThat(counting.nameResolves).isEqualTo(1);
  }

  @Test
  void repeatedPropertyComplex_rendersEveryOccurrenceAndReuses() {
    CountingStrategy counting = new CountingStrategy();
    ComplexRepeated query = new ComplexRepeated();
    query.name = "TODAY";
    query.email = "x";
    query.age = 10;

    EntityMetadata metadata = metadataFactory.getEntityMetadata(ComplexRepeated.class);
    StringBuilder sql = new StringBuilder();
    new ExampleQuery(metadataFactory, query, List.of(counting))
            .appendWhereClause(Platform.mysql(), metadata, sql);

    // name appears three times, across an AND group, a NOT, and a parenthesized group
    assertThat(sql.toString())
            .isEqualTo(" WHERE name = ? AND (email = ? OR NOT (name = ?))"
                    + " XOR (name = ? AND age = ?)");
    assertThat(counting.nameResolves).isEqualTo(1);
  }

  @Test
  void malformedExpressions_shouldThrow() {
    assertThatThrownBy(() -> GroupExpressionParser.parse(""))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("   "))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("name AND"))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("AND name"))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("(name OR age"))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("name OR age)"))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("name age"))
            .isInstanceOf(IllegalEntityException.class);
    assertThatThrownBy(() -> GroupExpressionParser.parse("name &&& age"))
            .isInstanceOf(IllegalEntityException.class);
  }

  @Test
  void bigCombo_rendersComplexExpression() {
    BigCombo query = new BigCombo();
    query.name = "TODAY";
    query.email = "x";
    query.mobilePhone = "12";
    query.age = 25;
    query.id = 7;
    query.password = "p";
    query.avatar = "a";
    query.gender = null;

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name like ?"
                    + " AND (email like ? XOR mobile_phone like ? OR age > ?)"
                    + " AND NOT (id = ? AND password = ?)"
                    + " XOR (avatar = ? OR gender is null)");
  }

  @Test
  void bigCombo_nullLeaves_arePrunedAlongTheWholeTree() {
    BigCombo query = new BigCombo();
    query.name = "TODAY";
    query.gender = null;

    // every other leaf is null -> the groups collapse away, but the
    // explicit parentheses around (avatar OR gender) are preserved
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE name like ? XOR (gender is null)");
  }

  // ---- test query classes ----

  @EntityRef(UserModel.class)
  @GroupExpression("nonexistent")
  static class NonExistentPropertyQuery {
    @Where("name = ?")
    String name;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("age AND name OR id")
  static class MixedOperators {
    @Where(operator = " >= ")
    Integer age;
    @Where(operator = " like ")
    String name;
    @Where(operator = " <> ")
    Integer id;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("active AND age")
  static class RawFragments {
    @Where("is_active = 1")
    boolean active;
    @Where("age > ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("age AND id OR email")
  static class RawAndOperator {
    @Where("age > ?")
    Integer age;
    @Where("id <> ?")
    Integer id;
    @Where("email = ?")
    String email;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND (email OR mobilePhone) XOR gender")
  static class LikeFamilyMixed {
    @Like
    String name;
    @PrefixLike
    String email;
    @SuffixLike
    String mobilePhone;
    @WhereIsNull
    Integer gender;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("deletedAt OR status")
  static class WhereIsNullPair {
    @WhereIsNull
    Integer deletedAt;
    @WhereIsNull(not = true)
    Integer status;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("deletedAt")
  static class SingleIsNull {
    @WhereIsNull
    Integer deletedAt;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("NOT name")
  static class NotSingle {
    @Where("name = ?")
    String name;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("NOT (name OR age)")
  static class NotGroup {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND NOT (email OR id) XOR age")
  static class NotMixed {
    @Where("name = ?")
    String name;
    @Where("age = ?")
    Integer age;
    @Where("email = ?")
    String email;
    @Where("id = ?")
    Integer id;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("NOT name AND email")
  static class NotPrecedence {
    @Where("name = ?")
    String name;
    @Where("email = ?")
    String email;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name OR (email AND name)")
  static class RepeatedProperty {
    @Where("name = ?")
    String name;
    @Where("email = ?")
    String email;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND (email OR NOT name) XOR (name AND age)")
  static class ComplexRepeated {
    @Where("name = ?")
    String name;
    @Where("email = ?")
    String email;
    @Where("age = ?")
    Integer age;
  }

  @EntityRef(UserModel.class)
  @GroupExpression("name AND (email XOR mobilePhone OR age) AND NOT (id AND password) XOR (avatar OR gender)")
  static class BigCombo {
    @Like
    String name;
    @PrefixLike
    String email;
    @SuffixLike
    String mobilePhone;
    @Where("age > ?")
    Integer age;
    @Where("id = ?")
    Integer id;
    @Where("password = ?")
    String password;
    @Where("avatar = ?")
    String avatar;
    @WhereIsNull
    Integer gender;
  }

  /**
   * Counts how often the {@code name} property is resolved, proving that a
   * property referenced several times is parsed into a single condition.
   */
  private static final class CountingStrategy implements PropertyConditionStrategy {

    int nameResolves;

    @Override
    public Condition resolve(EntityMetadata entityMetadata, EntityProperty property,
            Object value, ValueNormalizer valueNormalizer) {
      if (property.getName().equals("name")) {
        nameResolves++;
      }
      return new PropertyCondition(value, Restrictions.equal(property.getColumnName()), property);
    }
  }

}