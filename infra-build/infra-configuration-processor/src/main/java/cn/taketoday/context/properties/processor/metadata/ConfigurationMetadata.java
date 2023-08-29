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

package cn.taketoday.context.properties.processor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ItemMetadata
 * @since 4.0
 */
public class ConfigurationMetadata {

  private static final Set<Character> SEPARATORS = Set.of('-', '_');

  private final Map<String, List<ItemMetadata>> items;

  private final Map<String, List<ItemHint>> hints;

  public ConfigurationMetadata() {
    this.items = new LinkedHashMap<>();
    this.hints = new LinkedHashMap<>();
  }

  public ConfigurationMetadata(ConfigurationMetadata metadata) {
    this.items = new LinkedHashMap<>(metadata.items);
    this.hints = new LinkedHashMap<>(metadata.hints);
  }

  /**
   * Add item meta-data.
   *
   * @param itemMetadata the meta-data to add
   */
  public void add(ItemMetadata itemMetadata) {
    add(this.items, itemMetadata.getName(), itemMetadata, false);
  }

  /**
   * Add item meta-data if it's not already present.
   *
   * @param itemMetadata the meta-data to add
   */
  public void addIfMissing(ItemMetadata itemMetadata) {
    add(this.items, itemMetadata.getName(), itemMetadata, true);
  }

  /**
   * Add item hint.
   *
   * @param itemHint the item hint to add
   */
  public void add(ItemHint itemHint) {
    add(this.hints, itemHint.getName(), itemHint, false);
  }

  /**
   * Merge the content from another {@link ConfigurationMetadata}.
   *
   * @param metadata the {@link ConfigurationMetadata} instance to merge
   */
  public void merge(ConfigurationMetadata metadata) {
    for (ItemMetadata additionalItem : metadata.getItems()) {
      mergeItemMetadata(additionalItem);
    }
    for (ItemHint itemHint : metadata.getHints()) {
      add(itemHint);
    }
  }

  /**
   * Return item meta-data.
   *
   * @return the items
   */
  public List<ItemMetadata> getItems() {
    return flattenValues(this.items);
  }

  /**
   * Return hint meta-data.
   *
   * @return the hints
   */
  public List<ItemHint> getHints() {
    return flattenValues(this.hints);
  }

  protected void mergeItemMetadata(ItemMetadata metadata) {
    ItemMetadata matching = findMatchingItemMetadata(metadata);
    if (matching != null) {
      if (metadata.getDescription() != null) {
        matching.setDescription(metadata.getDescription());
      }
      if (metadata.getDefaultValue() != null) {
        matching.setDefaultValue(metadata.getDefaultValue());
      }
      ItemDeprecation deprecation = metadata.getDeprecation();
      ItemDeprecation matchingDeprecation = matching.getDeprecation();
      if (deprecation != null) {
        if (matchingDeprecation == null) {
          matching.setDeprecation(deprecation);
        }
        else {
          if (deprecation.getReason() != null) {
            matchingDeprecation.setReason(deprecation.getReason());
          }
          if (deprecation.getReplacement() != null) {
            matchingDeprecation.setReplacement(deprecation.getReplacement());
          }
          if (deprecation.getLevel() != null) {
            matchingDeprecation.setLevel(deprecation.getLevel());
          }
          if (deprecation.getSince() != null) {
            matchingDeprecation.setSince(deprecation.getSince());
          }
        }
      }
    }
    else {
      add(this.items, metadata.getName(), metadata, false);
    }
  }

  private <K, V> void add(Map<K, List<V>> map, K key, V value, boolean ifMissing) {
    List<V> values = map.computeIfAbsent(key, (k) -> new ArrayList<>());
    if (!ifMissing || values.isEmpty()) {
      values.add(value);
    }
  }

  private ItemMetadata findMatchingItemMetadata(ItemMetadata metadata) {
    List<ItemMetadata> candidates = this.items.get(metadata.getName());
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    candidates = new ArrayList<>(candidates);
    candidates.removeIf((itemMetadata) -> !itemMetadata.hasSameType(metadata));
    if (candidates.size() > 1 && metadata.getType() != null) {
      candidates.removeIf((itemMetadata) -> !metadata.getType().equals(itemMetadata.getType()));
    }
    if (candidates.size() == 1) {
      return candidates.get(0);
    }
    for (ItemMetadata candidate : candidates) {
      if (nullSafeEquals(candidate.getSourceType(), metadata.getSourceType())) {
        return candidate;
      }
    }
    return null;
  }

  private boolean nullSafeEquals(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    }
    return o1 != null && o1.equals(o2);
  }

  public static String nestedPrefix(String prefix, String name) {
    String nestedPrefix = (prefix != null) ? prefix : "";
    String dashedName = toDashedCase(name);
    nestedPrefix += (nestedPrefix == null || nestedPrefix.isEmpty()) ? dashedName : "." + dashedName;
    return nestedPrefix;
  }

  static String toDashedCase(String name) {
    StringBuilder dashed = new StringBuilder();
    Character previous = null;
    for (int i = 0; i < name.length(); i++) {
      char current = name.charAt(i);
      if (SEPARATORS.contains(current)) {
        dashed.append("-");
      }
      else if (Character.isUpperCase(current) && previous != null && !SEPARATORS.contains(previous)) {
        dashed.append("-").append(current);
      }
      else {
        dashed.append(current);
      }
      previous = current;

    }
    return dashed.toString().toLowerCase(Locale.ENGLISH);
  }

  private static <T extends Comparable<T>> List<T> flattenValues(Map<?, List<T>> map) {
    List<T> content = new ArrayList<>();
    for (List<T> values : map.values()) {
      content.addAll(values);
    }
    Collections.sort(content);
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ConfigurationMetadata that))
      return false;
    return Objects.equals(items, that.items) && Objects.equals(hints, that.hints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, hints);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(String.format("items: %n"));
    this.items.values().forEach((itemMetadata) -> result.append("\t").append(String.format("%s%n", itemMetadata)));
    return result.toString();
  }

}
