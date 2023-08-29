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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.json.JSONArray;
import cn.taketoday.context.properties.json.JSONObject;

/**
 * Marshaller to write {@link ConfigurationMetadata} as JSON.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JsonMarshaller {

  private static final int BUFFER_SIZE = 4098;

  public void write(ConfigurationMetadata metadata, OutputStream outputStream) throws IOException {
    try {
      JSONObject object = new JSONObject();
      JsonConverter converter = new JsonConverter();
      object.put("groups", converter.toJsonArray(metadata, ItemMetadata.ItemType.GROUP));
      object.put("properties", converter.toJsonArray(metadata, ItemMetadata.ItemType.PROPERTY));
      object.put("hints", converter.toJsonArray(metadata.getHints()));
      outputStream.write(object.toString(2).getBytes(StandardCharsets.UTF_8));
    }
    catch (Exception ex) {
      if (ex instanceof IOException ioException) {
        throw ioException;
      }
      if (ex instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(ex);
    }
  }

  public ConfigurationMetadata read(InputStream inputStream) throws Exception {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    JSONObject object = new JSONObject(toString(inputStream));
    JSONArray groups = object.optJSONArray("groups");
    if (groups != null) {
      for (int i = 0; i < groups.length(); i++) {
        metadata.add(toItemMetadata((JSONObject) groups.get(i), ItemMetadata.ItemType.GROUP));
      }
    }
    JSONArray properties = object.optJSONArray("properties");
    if (properties != null) {
      for (int i = 0; i < properties.length(); i++) {
        metadata.add(toItemMetadata((JSONObject) properties.get(i), ItemMetadata.ItemType.PROPERTY));
      }
    }
    JSONArray hints = object.optJSONArray("hints");
    if (hints != null) {
      for (int i = 0; i < hints.length(); i++) {
        metadata.add(toItemHint((JSONObject) hints.get(i)));
      }
    }
    return metadata;
  }

  private ItemMetadata toItemMetadata(JSONObject object, ItemMetadata.ItemType itemType) throws Exception {
    String name = object.getString("name");
    String type = object.optString("type", null);
    String description = object.optString("description", null);
    String sourceType = object.optString("sourceType", null);
    String sourceMethod = object.optString("sourceMethod", null);
    Object defaultValue = readItemValue(object.opt("defaultValue"));
    ItemDeprecation deprecation = toItemDeprecation(object);
    return new ItemMetadata(itemType, name, null, type, sourceType, sourceMethod, description, defaultValue,
            deprecation);
  }

  private ItemDeprecation toItemDeprecation(JSONObject object) throws Exception {
    if (object.has("deprecation")) {
      JSONObject deprecationJsonObject = object.getJSONObject("deprecation");
      ItemDeprecation deprecation = new ItemDeprecation();
      deprecation.setLevel(deprecationJsonObject.optString("level", null));
      deprecation.setReason(deprecationJsonObject.optString("reason", null));
      deprecation.setReplacement(deprecationJsonObject.optString("replacement", null));
      deprecation.setSince(deprecationJsonObject.optString("since", null));
      return deprecation;
    }
    return object.optBoolean("deprecated") ? new ItemDeprecation() : null;
  }

  private ItemHint toItemHint(JSONObject object) throws Exception {
    String name = object.getString("name");
    List<ItemHint.ValueHint> values = new ArrayList<>();
    if (object.has("values")) {
      JSONArray valuesArray = object.getJSONArray("values");
      for (int i = 0; i < valuesArray.length(); i++) {
        values.add(toValueHint((JSONObject) valuesArray.get(i)));
      }
    }
    List<ItemHint.ValueProvider> providers = new ArrayList<>();
    if (object.has("providers")) {
      JSONArray providersObject = object.getJSONArray("providers");
      for (int i = 0; i < providersObject.length(); i++) {
        providers.add(toValueProvider((JSONObject) providersObject.get(i)));
      }
    }
    return new ItemHint(name, values, providers);
  }

  private ItemHint.ValueHint toValueHint(JSONObject object) throws Exception {
    Object value = readItemValue(object.get("value"));
    String description = object.optString("description", null);
    return new ItemHint.ValueHint(value, description);
  }

  private ItemHint.ValueProvider toValueProvider(JSONObject object) throws Exception {
    String name = object.getString("name");
    Map<String, Object> parameters = new HashMap<>();
    if (object.has("parameters")) {
      JSONObject parametersObject = object.getJSONObject("parameters");
      for (Iterator<?> iterator = parametersObject.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        Object value = readItemValue(parametersObject.get(key));
        parameters.put(key, value);
      }
    }
    return new ItemHint.ValueProvider(name, parameters);
  }

  private Object readItemValue(Object value) throws Exception {
    if (value instanceof JSONArray array) {
      Object[] content = new Object[array.length()];
      for (int i = 0; i < array.length(); i++) {
        content[i] = array.get(i);
      }
      return content;
    }
    return value;
  }

  private String toString(InputStream inputStream) throws IOException {
    StringBuilder out = new StringBuilder();
    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    char[] buffer = new char[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = reader.read(buffer)) != -1) {
      out.append(buffer, 0, bytesRead);
    }
    return out.toString();
  }

}
