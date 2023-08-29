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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.processor.test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.internal.Objects;

import cn.taketoday.context.properties.processor.metadata.ItemDeprecation;
import cn.taketoday.context.properties.processor.metadata.ItemMetadata;
import cn.taketoday.context.properties.processor.metadata.ItemMetadata.ItemType;

/**
 * AssertJ assert for {@link ItemMetadata}.
 *
 * @author Stephane Nicoll
 */
public class ItemMetadataAssert extends AbstractAssert<ItemMetadataAssert, ItemMetadata>
        implements AssertProvider<ItemMetadataAssert> {

  private static final Objects objects = Objects.instance();

  public ItemMetadataAssert(ItemMetadata itemMetadata) {
    super(itemMetadata, ItemMetadataAssert.class);
    objects.assertNotNull(this.info, itemMetadata);
  }

  public ItemMetadataAssert isProperty() {
    objects.assertEqual(this.info, this.actual.isOfItemType(ItemType.PROPERTY), true);
    return this;
  }

  public ItemMetadataAssert isGroup() {
    objects.assertEqual(this.info, this.actual.isOfItemType(ItemType.GROUP), true);
    return this;
  }

  public ItemMetadataAssert hasName(String name) {
    objects.assertEqual(this.info, this.actual.getName(), name);
    return this;
  }

  public ItemMetadataAssert hasType(String type) {
    objects.assertEqual(this.info, this.actual.getType(), type);
    return this;
  }

  public ItemMetadataAssert hasType(Class<?> type) {
    return hasType(type.getName());
  }

  public ItemMetadataAssert hasDescription(String description) {
    objects.assertEqual(this.info, this.actual.getDescription(), description);
    return this;
  }

  public ItemMetadataAssert hasNoDescription() {
    return hasDescription(null);
  }

  public ItemMetadataAssert hasSourceType(String type) {
    objects.assertEqual(this.info, this.actual.getSourceType(), type);
    return this;
  }

  public ItemMetadataAssert hasSourceType(Class<?> type) {
    return hasSourceType(type.getName());
  }

  public ItemMetadataAssert hasSourceMethod(String type) {
    objects.assertEqual(this.info, this.actual.getSourceMethod(), type);
    return this;
  }

  public ItemMetadataAssert hasDefaultValue(Object defaultValue) {
    objects.assertEqual(this.info, this.actual.getDefaultValue(), defaultValue);
    return this;
  }

  public ItemMetadataAssert isDeprecatedWithNoInformation() {
    assertItemDeprecation();
    return this;
  }

  public ItemMetadataAssert isDeprecatedWithReason(String reason) {
    ItemDeprecation deprecation = assertItemDeprecation();
    objects.assertEqual(this.info, deprecation.getReason(), reason);
    return this;
  }

  public ItemMetadataAssert isDeprecatedWithReplacement(String replacement) {
    ItemDeprecation deprecation = assertItemDeprecation();
    objects.assertEqual(this.info, deprecation.getReplacement(), replacement);
    return this;
  }

  public ItemMetadataAssert isNotDeprecated() {
    objects.assertNull(this.info, this.actual.getDeprecation());
    return this;
  }

  private ItemDeprecation assertItemDeprecation() {
    ItemDeprecation deprecation = this.actual.getDeprecation();
    objects.assertNotNull(this.info, deprecation);
    objects.assertNull(this.info, deprecation.getLevel());
    return deprecation;
  }

  @Override
  public ItemMetadataAssert assertThat() {
    return this;
  }

}
