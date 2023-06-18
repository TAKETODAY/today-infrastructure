/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.index.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.util.CollectionUtils;

/**
 * Marshaller to write {@link CandidateComponentsMetadata} as properties.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0
 */
abstract class PropertiesMarshaller {

  public static void write(CandidateComponentsMetadata metadata, OutputStream out) throws IOException {
    Properties props = CollectionUtils.createSortedProperties(true);
    metadata.getItems().forEach(m -> props.put(m.getType(), String.join(",", m.getStereotypes())));
    props.store(out, null);
  }

  public static CandidateComponentsMetadata read(InputStream in) throws IOException {
    CandidateComponentsMetadata result = new CandidateComponentsMetadata();
    Properties props = new Properties();
    props.load(in);
    props.forEach((type, value) -> {
      Set<String> candidates = new HashSet<>(Arrays.asList(((String) value).split(",")));
      result.add(new ItemMetadata((String) type, candidates));
    });
    return result;
  }

}
