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

package infra.persistence;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.function.Consumer;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.jdbc.type.TypeHandlerManager;
import infra.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/18 16:55
 */
class DefaultEntityMetadataFactoryTests {
  DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

  @Test
  void defaultState() {
    EntityMetadata entityMetadata = factory.createEntityMetadata(UserModel.class);
    assertThat(entityMetadata).isNotNull();
    assertThatThrownBy(() ->
            factory.createEntityMetadata(Object.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine properties for entity: " + Object.class);
  }

  @Test
  void exception() {
    assertIllegalArgumentException(factory::setPropertyFilter);
    assertIllegalArgumentException(factory::setColumnNameDiscover);
    assertIllegalArgumentException(factory::setIdPropertyDiscover);
    assertIllegalArgumentException(factory::setTableNameGenerator);

    factory.setTableNameGenerator(TableNameGenerator.forTableAnnotation());
    assertThatThrownBy(() ->
            factory.createEntityMetadata(Object.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Cannot determine table name for entity: " + Object.class);

    factory.setTableNameGenerator(
            TableNameGenerator.forTableAnnotation()
                    .and(TableNameGenerator.defaultStrategy())
    );

    factory.setColumnNameDiscover(ColumnNameDiscover.forColumnAnnotation());

    assertThatThrownBy(() ->
            factory.createEntityMetadata(UserModel.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine column name for property: UserModel#");

    factory.setPropertyFilter(PropertyFilter.acceptAny());
  }

  @Test
  void multipleId() {
    class MultipleId extends UserModel {
      @Id
      private Long id_;
    }
    assertThatThrownBy(() ->
            factory.createEntityMetadata(MultipleId.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Only one Id property supported, entity: " + MultipleId.class);
  }

  @Test
  void overrideId() {
    class OverrideId extends UserModel {
      @Id
      public Integer id;

    }

    EntityMetadata entityMetadata = factory.createEntityMetadata(OverrideId.class);
    assertThat(entityMetadata.idProperty).isNotNull();
    BeanProperty id = BeanMetadata.forClass(OverrideId.class).obtainBeanProperty("id");
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(id);
  }

  @Test
  void idDiscover() {
    class IdDiscover {
      private Long id;

      private Long id_;
    }

    // default

    EntityMetadata entityMetadata = factory.createEntityMetadata(IdDiscover.class);
    assertThat(entityMetadata.idProperty).isNotNull();
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(BeanProperty.valueOf(IdDiscover.class, "id"));

    factory.setIdPropertyDiscover(IdPropertyDiscover.forPropertyName("id_"));

    entityMetadata = factory.createEntityMetadata(IdDiscover.class);
    assertThat(entityMetadata.idProperty).isNotNull();
    assertThat(entityMetadata.idProperty()).isNotNull();
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(BeanProperty.valueOf(IdDiscover.class, "id_"));
  }

  @Test
  void getEntityHolder() {
    EntityMetadata entityMetadata = factory.getEntityMetadata(UserModel.class);
    assertThat(factory.entityCache.get(UserModel.class)).isEqualTo(entityMetadata).isEqualTo(entityMetadata);
    factory.entityCache.remove(UserModel.class);

    assertThat(factory.getEntityMetadata(UserModel.class)).isEqualTo(entityMetadata);
    assertThat(factory.getEntityMetadata(UserModel.class).hashCode()).isEqualTo(entityMetadata.hashCode());

    assertThat(entityMetadata.toString()).contains("tableName = 't_user'");
    assertThat(entityMetadata.toString()).contains("columnName = 'id'");
    assertThat(entityMetadata.toString()).contains("beanProperties = array<BeanProperty>[");
    assertThat(entityMetadata.toString()).contains("entityProperties = array<EntityProperty>[[");
  }

  @Test
  void nullId() {
    class NullId {
      String name;
    }
    EntityMetadata entityMetadata = factory.getEntityMetadata(NullId.class);
    assertThat(factory.entityCache.get(NullId.class)).isSameAs(entityMetadata);
    assertThat(entityMetadata.idProperty).isNull();
    assertThat(entityMetadata.idColumnName).isNull();
    assertThat(entityMetadata.autoGeneratedId).isFalse();
    assertThat(entityMetadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));

    assertThatThrownBy(entityMetadata::idProperty)
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("ID property is required");
  }

  @Test
  void findProperty() {
    EntityMetadata entityMetadata = factory.getEntityMetadata(UserModel.class);
    assertThat(factory.entityCache.get(UserModel.class)).isEqualTo(entityMetadata);
    EntityProperty property = entityMetadata.findProperty("name");
    assertThat(property).isNotNull();
  }

  @Test
  void autoGeneratedId() {
    class AutoGeneratedId {

      Long id_;

      @GeneratedId
      public Long getId_() {
        return id_;
      }
    }

    var entityMetadata = factory.getEntityMetadata(AutoGeneratedId.class);
    assertThat(entityMetadata.idProperty).isNotNull();
    assertThat(entityMetadata.idColumnName).isNotNull().isEqualTo("id_");
    assertThat(entityMetadata.autoGeneratedId).isTrue();
    assertThat(entityMetadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void metaGeneratedId() {
    class AutoGeneratedId {

      Long id_;

      @MyGeneratedId
      public Long getId_() {
        return id_;
      }
    }

    var entityMetadata = factory.getEntityMetadata(AutoGeneratedId.class);
    assertThat(entityMetadata.idProperty).isNotNull();
    assertThat(entityMetadata.idColumnName).isNotNull().isEqualTo("id_");
    assertThat(entityMetadata.autoGeneratedId).isTrue();
    assertThat(entityMetadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  // ref id

  @Test
  void entityRefId() {
    factory.setTableNameGenerator(new TableNameGenerator() {

      @Nullable
      @Override
      public String generateTableName(Class<?> entityClass) {
        if (entityClass == UserModel.class) {
          return "t_user";
        }
        return null;
      }
    });

    EntityMetadata metadata = factory.getEntityMetadata(UpdateUserName.class);
    assertThat(metadata.tableName).isEqualTo("t_user");
    assertThat(metadata.autoGeneratedId).isFalse();
    assertThat(metadata.refIdProperty).isNotNull().isSameAs(
            factory.getEntityMetadata(UserModel.class).idProperty);
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldCreateEntityMetadataWithValidEntity() {
    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);

    assertThat(metadata).isNotNull();
    assertThat(metadata.entityClass).isEqualTo(UserModel.class);
    assertThat(metadata.tableName).isNotNull();
    assertThat(metadata.columnNames).isNotEmpty();
    assertThat(metadata.entityProperties).isNotEmpty();
  }

  @Test
  void shouldFilterOutInnerClassProperty() {

    class OuterClass {
      class InnerClass {
        String name;
      }
    }

    EntityMetadata metadata = factory.createEntityMetadata(OuterClass.InnerClass.class);
    // Should not contain "this$0" property
    assertThat(metadata.findProperty("this$0")).isNull();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleNullColumnName() {
    factory.setColumnNameDiscover(property -> null);

    assertThatThrownBy(() -> factory.createEntityMetadata(UserModel.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageContaining("Cannot determine column name for property:");
  }

  @Test
  void shouldHandleEntityRefWithNoRefMetadata() {
    factory.setTableNameGenerator(entityClass -> null);

    @EntityRef(Object.class)
    class NoRefMetadataEntity {
      Long id;
      String name;
    }

    // Object.class has no properties, so this should fail
    assertThatThrownBy(() -> factory.createEntityMetadata(NoRefMetadataEntity.class))
            .isInstanceOf(IllegalEntityException.class);
  }

  @Test
  void shouldCreateEntityPropertyWithCorrectTypeHandler() {
    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);
    for (EntityProperty property : metadata.entityProperties) {
      // TypeHandler should be assigned for each property
      assertThat(property.typeHandler).isNotNull();
    }
  }

  @Test
  void shouldExcludeIdPropertyFromExcludedLists() {
    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);

    if (metadata.idProperty != null) {
      // ID column should not be in columnNamesExcludeId
      assertThat(metadata.columnNamesExcludeId).doesNotContain(metadata.idProperty.columnName);

      // ID property should not be in entityPropertiesExcludeId
      assertThat(metadata.entityPropertiesExcludeId).noneMatch(ep -> ep == metadata.idProperty);
    }
  }

  @Test
  void shouldHandlePropertyFilter() {
    class TestEntity {
      Long id;
      String name;
      String password; // This should be filtered out
    }

    // Set filter to exclude "password" field
    factory.setPropertyFilter(PropertyFilter.filteredNames(Set.of("password", "class")));

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    // Should not contain password property
    assertThat(metadata.findProperty("password")).isNull();
    // But should contain id and name
    assertThat(metadata.findProperty("id")).isNotNull();
    assertThat(metadata.findProperty("name")).isNotNull();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleTableNameGenerator() {

    final String customTableName = "custom_table";
    factory.setTableNameGenerator(entityClass -> customTableName);

    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);
    assertThat(metadata.tableName).isEqualTo(customTableName);
  }

  @Test
  void shouldHandleColumnNameDiscover() {

    final String customColumnName = "custom_column";
    factory.setColumnNameDiscover(property -> customColumnName);

    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);
    // All columns should have the custom name (though this would cause issues with duplicates)
    assertThat(metadata.columnNames).allMatch(name -> name.equals(customColumnName));
  }

  @Test
  void shouldHandleIdPropertyDiscover() {

    class TestEntity {
      Long identifier; // Not standard "id"
      String name;
    }

    // Configure to use "identifier" as ID property
    factory.setIdPropertyDiscover(IdPropertyDiscover.forPropertyName("identifier"));

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.idProperty).isNotNull();
    assertThat(metadata.idProperty.property.getName()).isEqualTo("identifier");
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldCacheEntityMetadata() {

    EntityMetadata metadata1 = factory.getEntityMetadata(UserModel.class);
    EntityMetadata metadata2 = factory.getEntityMetadata(UserModel.class);

    assertThat(metadata1).isSameAs(metadata2);
  }

  @Test
  void shouldHandleTypeHandlerManager() {

    TypeHandlerManager customManager = new TypeHandlerManager();
    factory.setTypeHandlerManager(customManager);

    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);

    // EntityProperty should use type handlers from the custom manager
    for (EntityProperty property : metadata.entityProperties) {
      assertThat(property.typeHandler).isNotNull();
    }
  }

  @Test
  void shouldHandleClassPropertyFiltering() {

    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);

    // "class" property should be filtered out by default
    assertThat(metadata.findProperty("class")).isNull();
  }

  @Test
  void shouldUseDefaultTableNameGenerator() {

    class TestEntity {
      Long id;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    // Default table name should be generated based on class name
    assertThat(metadata.tableName).isNotNull();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleTransientAnnotationFiltering() {

    class TestEntity {
      Long id;

      @Transient
      String temporaryField;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    // Should contain id property
    assertThat(metadata.findProperty("id")).isNotNull();
    // Should not contain transient field
    assertThat(metadata.findProperty("temporaryField")).isNull();
  }

  @Test
  void shouldHandleIdAnnotationDiscovery() {

    class TestEntity {
      @Id
      Long identifier;

      String name;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.idProperty).isNotNull();
    assertThat(metadata.idProperty.property.getName()).isEqualTo("identifier");
    assertThat(metadata.idColumnName).isNotNull();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleColumnAnnotationDiscovery() {

    class TestEntity {
      @Id
      Long id;

      @Column("user_name")
      String name;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    EntityProperty nameProperty = metadata.findProperty("name");
    assertThat(nameProperty).isNotNull();
    assertThat(nameProperty.columnName).isEqualTo("user_name");
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleCamelCaseToUnderscoreConversion() {

    class TestEntity {
      @Id
      Long id;

      String userName;
      String firstName;
      String lastName;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    EntityProperty userNameProperty = metadata.findProperty("userName");
    assertThat(userNameProperty.columnName).isEqualTo("user_name");

    EntityProperty firstNameProperty = metadata.findProperty("firstName");
    assertThat(firstNameProperty.columnName).isEqualTo("first_name");

    EntityProperty lastNameProperty = metadata.findProperty("lastName");
    assertThat(lastNameProperty.columnName).isEqualTo("last_name");
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleGeneratedIdAnnotation() {

    class TestEntity {
      @Id
      @GeneratedId
      Long id;

      String name;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.autoGeneratedId).isTrue();
    assertThat(metadata.idProperty.isPresent(GeneratedId.class)).isTrue();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldHandleEntityRefAnnotation() {

    @EntityRef(UserModel.class)
    class UserUpdate {
      String name;
    }

    // Set up table name generator to return null for UserUpdate
    factory.setTableNameGenerator(new TableNameGenerator() {
      @Override
      public String generateTableName(Class<?> entityClass) {
        if (entityClass == UserUpdate.class) {
          return null;
        }
        return TableNameGenerator.defaultStrategy().generateTableName(entityClass);
      }
    });

    EntityMetadata metadata = factory.createEntityMetadata(UserUpdate.class);

    // Should use UserModel's table name
    assertThat(metadata.tableName).isEqualTo("t_user");
    // Should have refIdProperty from UserModel
    assertThat(metadata.refIdProperty).isNotNull();
    assertThat(metadata).isNotEqualTo(factory.getEntityMetadata(UserModel.class));
  }

  @Test
  void shouldThrowExceptionForMultipleIdProperties() {
    class TestEntity extends UserModel {
      @Id
      Long customId;
    }

    assertThatThrownBy(() -> factory.createEntityMetadata(TestEntity.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Only one Id property supported, entity: " + TestEntity.class);
  }

  @Test
  void shouldHandleNullTableNameWithoutRef() {
    factory.setTableNameGenerator(entityClass -> null);

    class SimpleEntity {
      @Id
      Long id;
    }

    assertThatThrownBy(() -> factory.createEntityMetadata(SimpleEntity.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Cannot determine table name for entity: " + SimpleEntity.class);
  }

  @Test
  void shouldHandleCustomPropertyFilter() {
    class TestEntity {
      @Id
      Long id;
      String name;
      String password;
      String version;
    }

    // Filter out password and version fields
    factory.setPropertyFilter(PropertyFilter.filteredNames(Set.of("password", "version", "class")));

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.findProperty("id")).isNotNull();
    assertThat(metadata.findProperty("name")).isNotNull();
    assertThat(metadata.findProperty("password")).isNull();
    assertThat(metadata.findProperty("version")).isNull();
  }

  @Test
  void shouldHandleEqualsAndHashCode() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    EntityMetadata metadata1 = factory.getEntityMetadata(UserModel.class);
    EntityMetadata metadata2 = factory.getEntityMetadata(UserModel.class);

    assertThat(metadata1).isEqualTo(metadata2);
    assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
  }

  @Test
  void shouldHandleEqualsWithDifferentType() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(UserModel.class);

    assertThat(metadata).isNotEqualTo(new Object());
  }

  @Test
  void shouldHandleToString() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(UserModel.class);

    String toString = metadata.toString();
    assertThat(toString).contains("tableName");
    assertThat(toString).contains("columnNames");
    assertThat(toString).contains("entityClass");
  }

  @Test
  void shouldHandleGetAnnotation() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    @Table
    class TestEntity {
      @Id
      Long id;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    var idAnnotation = metadata.getAnnotation(Id.class);
    assertThat(idAnnotation.isPresent()).isFalse();
    assertThat(metadata.getAnnotation(Table.class).isPresent()).isTrue();
  }

  @Test
  void shouldHandleIsPresent() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    @Table
    class TestEntity {
      @Id
      Long id;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.isPresent(Id.class)).isFalse();
    assertThat(metadata.isPresent(Table.class)).isTrue();
    assertThat(metadata.isPresent(Transient.class)).isFalse();
  }

  @Test
  void shouldHandleGetAnnotations() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    class TestEntity {
      @Id
      Long id;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    var annotations = metadata.getAnnotations();
    assertThat(annotations).isNotNull();

    // Second call should return the same instance (cached)
    assertThat(metadata.getAnnotations()).isSameAs(annotations);
  }

  @Test
  void shouldHandleNullIdPropertyInDetermineGeneratedId() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    class TestEntity {
      String name;
    }

    EntityMetadata metadata = factory.createEntityMetadata(TestEntity.class);

    assertThat(metadata.autoGeneratedId).isFalse();
  }

  @Test
  void shouldHandleEntityPropertyMap() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    EntityMetadata metadata = factory.createEntityMetadata(UserModel.class);

    // Should be able to find properties by name
    for (EntityProperty property : metadata.entityProperties) {
      EntityProperty found = metadata.findProperty(property.property.getName());
      assertThat(found).isNotNull().isEqualTo(property);
    }
  }

  static <T> void assertIllegalArgumentException(Consumer<T> throwingCallable) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> throwingCallable.accept(null))
            .withMessageEndingWith("is required");
  }

  @GeneratedId
  @Target({ ElementType.FIELD, ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyGeneratedId {

  }

  @EntityRef(UserModel.class)
  static class UpdateUserName {

    public final String name;

    UpdateUserName(String name) {
      this.name = name;
    }
  }

}