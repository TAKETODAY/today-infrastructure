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

import java.util.Collection;

import cn.taketoday.context.properties.json.JSONArray;
import cn.taketoday.context.properties.json.JSONObject;
import cn.taketoday.context.properties.processor.metadata.ItemMetadata.ItemType;

/**
 * {@link JsonConverter} for use in tests.
 *
 * @author Phillip Webb
 */
public class TestJsonConverter extends JsonConverter {

  @Override
  public JSONArray toJsonArray(ConfigurationMetadata metadata, ItemType itemType) throws Exception {
    return super.toJsonArray(metadata, itemType);
  }

  @Override
  public JSONArray toJsonArray(Collection<ItemHint> hints) throws Exception {
    return super.toJsonArray(hints);
  }

  @Override
  public JSONObject toJsonObject(ItemMetadata item) throws Exception {
    return super.toJsonObject(item);
  }

}
