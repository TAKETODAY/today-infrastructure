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

package cn.taketoday.framework.logging.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.joran.sanity.IfNestedWithinSecondPhaseElementSC;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.core.joran.sanity.Pair;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ContextAwareBase;
import cn.taketoday.lang.Nullable;

/**
 * {@link SanityChecker} to ensure that {@code infra-profile} elements are not nested
 * within second-phase elements.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see IfNestedWithinSecondPhaseElementSC
 * @since 4.0
 */
class InfraProfileIfNestedWithinSecondPhaseElementSanityChecker extends ContextAwareBase implements SanityChecker {

  private static final List<Class<? extends Model>> SECOND_PHASE_TYPES =
          List.of(AppenderModel.class, LoggerModel.class, RootLoggerModel.class);

  @Override
  public void check(@Nullable Model model) {
    if (model == null) {
      return;
    }
    var models = new ArrayList<Model>();
    for (Class<? extends Model> type : SECOND_PHASE_TYPES) {
      deepFindAllModelsOfType(type, models, model);
    }

    var nestedPairs = deepFindNestedSubModelsOfType(InfraProfileModel.class, models);
    if (!nestedPairs.isEmpty()) {
      addWarn("<infra-profile> elements cannot be nested within an <appender>, <logger> or <root> element");
      for (Pair<Model, Model> nested : nestedPairs) {
        Model first = nested.first;
        Model second = nested.second;
        addWarn("Element <%s> at line %s contains a nested <%s> element at line %s".formatted(first.getTag(),
                first.getLineNumber(), second.getTag(), second.getLineNumber()));
      }
    }
  }

}
