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

package infra.app.logging.logback;

import org.jspecify.annotations.Nullable;

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
