/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.assertj.core.api.Condition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.context.index.processor.CandidateComponentsMetadata;
import cn.taketoday.context.index.processor.ItemMetadata;

/**
 * AssertJ {@link Condition} to help test {@link CandidateComponentsMetadata}.
 *
 * @author Stephane Nicoll
 */
class Metadata {

  public static Condition<CandidateComponentsMetadata> of(Class<?> type, Class<?>... stereotypes) {
    return of(type.getName(), Arrays.stream(stereotypes).map(Class::getName).collect(Collectors.toList()));
  }

  public static Condition<CandidateComponentsMetadata> of(String type, String... stereotypes) {
    return of(type, Arrays.asList(stereotypes));
  }

  public static Condition<CandidateComponentsMetadata> of(String type,
          List<String> stereotypes) {
    return new Condition<>(metadata -> {
      ItemMetadata itemMetadata = metadata.getItems().stream()
              .filter(item -> item.getType().equals(type))
              .findFirst().orElse(null);
      return itemMetadata != null && itemMetadata.getStereotypes().size() == stereotypes.size()
              && itemMetadata.getStereotypes().containsAll(stereotypes);
    }, "Candidates with type %s and stereotypes %s", type, stereotypes);
  }

}
