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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.json.JSONArray;
import cn.taketoday.context.properties.json.JSONObject;

/**
 * Converter to change meta-data objects into JSON objects.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JsonConverter {

  private static final ItemMetadataComparator ITEM_COMPARATOR = new ItemMetadataComparator();

  JSONArray toJsonArray(ConfigurationMetadata metadata, ItemMetadata.ItemType itemType) throws Exception {
    JSONArray jsonArray = new JSONArray();
    List<ItemMetadata> items = metadata.getItems()
            .stream()
            .filter((item) -> item.isOfItemType(itemType))
            .sorted(ITEM_COMPARATOR)
            .toList();
    for (ItemMetadata item : items) {
      if (item.isOfItemType(itemType)) {
        jsonArray.put(toJsonObject(item));
      }
    }
    return jsonArray;
  }

  JSONArray toJsonArray(Collection<ItemHint> hints) throws Exception {
    JSONArray jsonArray = new JSONArray();
    for (ItemHint hint : hints) {
      jsonArray.put(toJsonObject(hint));
    }
    return jsonArray;
  }

  JSONObject toJsonObject(ItemMetadata item) throws Exception {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", item.getName());
    jsonObject.putOpt("type", item.getType());
    jsonObject.putOpt("description", item.getDescription());
    jsonObject.putOpt("sourceType", item.getSourceType());
    jsonObject.putOpt("sourceMethod", item.getSourceMethod());
    Object defaultValue = item.getDefaultValue();
    if (defaultValue != null) {
      putDefaultValue(jsonObject, defaultValue);
    }
    ItemDeprecation deprecation = item.getDeprecation();
    if (deprecation != null) {
      jsonObject.put("deprecated", true); // backward compatibility
      JSONObject deprecationJsonObject = new JSONObject();
      if (deprecation.getLevel() != null) {
        deprecationJsonObject.put("level", deprecation.getLevel());
      }
      if (deprecation.getReason() != null) {
        deprecationJsonObject.put("reason", deprecation.getReason());
      }
      if (deprecation.getReplacement() != null) {
        deprecationJsonObject.put("replacement", deprecation.getReplacement());
      }
      if (deprecation.getSince() != null) {
        deprecationJsonObject.put("since", deprecation.getSince());
      }
      jsonObject.put("deprecation", deprecationJsonObject);
    }
    return jsonObject;
  }

  private JSONObject toJsonObject(ItemHint hint) throws Exception {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", hint.getName());
    if (!hint.getValues().isEmpty()) {
      jsonObject.put("values", getItemHintValues(hint));
    }
    if (!hint.getProviders().isEmpty()) {
      jsonObject.put("providers", getItemHintProviders(hint));
    }
    return jsonObject;
  }

  private JSONArray getItemHintValues(ItemHint hint) throws Exception {
    JSONArray values = new JSONArray();
    for (ItemHint.ValueHint value : hint.getValues()) {
      values.put(getItemHintValue(value));
    }
    return values;
  }

  private JSONObject getItemHintValue(ItemHint.ValueHint value) throws Exception {
    JSONObject result = new JSONObject();
    putHintValue(result, value.getValue());
    result.putOpt("description", value.getDescription());
    return result;
  }

  private JSONArray getItemHintProviders(ItemHint hint) throws Exception {
    JSONArray providers = new JSONArray();
    for (ItemHint.ValueProvider provider : hint.getProviders()) {
      providers.put(getItemHintProvider(provider));
    }
    return providers;
  }

  private JSONObject getItemHintProvider(ItemHint.ValueProvider provider) throws Exception {
    JSONObject result = new JSONObject();
    result.put("name", provider.getName());
    if (provider.getParameters() != null && !provider.getParameters().isEmpty()) {
      JSONObject parameters = new JSONObject();
      for (Map.Entry<String, Object> entry : provider.getParameters().entrySet()) {
        parameters.put(entry.getKey(), extractItemValue(entry.getValue()));
      }
      result.put("parameters", parameters);
    }
    return result;
  }

  private void putHintValue(JSONObject jsonObject, Object value) throws Exception {
    Object hintValue = extractItemValue(value);
    jsonObject.put("value", hintValue);
  }

  private void putDefaultValue(JSONObject jsonObject, Object value) throws Exception {
    Object defaultValue = extractItemValue(value);
    jsonObject.put("defaultValue", defaultValue);
  }

  private Object extractItemValue(Object value) {
    Object defaultValue = value;
    if (value.getClass().isArray()) {
      JSONArray array = new JSONArray();
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        array.put(Array.get(value, i));
      }
      defaultValue = array;

    }
    return defaultValue;
  }

  private static class ItemMetadataComparator implements Comparator<ItemMetadata> {

    private static final Comparator<ItemMetadata> GROUP = Comparator.comparing(ItemMetadata::getName)
            .thenComparing(ItemMetadata::getSourceType, Comparator.nullsFirst(Comparator.naturalOrder()));

    private static final Comparator<ItemMetadata> ITEM = Comparator.comparing(ItemMetadataComparator::isDeprecated)
            .thenComparing(ItemMetadata::getName)
            .thenComparing(ItemMetadata::getSourceType, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Override
    public int compare(ItemMetadata o1, ItemMetadata o2) {
      if (o1.isOfItemType(ItemMetadata.ItemType.GROUP)) {
        return GROUP.compare(o1, o2);
      }
      return ITEM.compare(o1, o2);
    }

    private static boolean isDeprecated(ItemMetadata item) {
      return item.getDeprecation() != null;
    }

  }

}
