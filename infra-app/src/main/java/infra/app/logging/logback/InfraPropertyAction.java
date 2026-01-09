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

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;

/**
 * Logback {@link BaseModelAction} for {@code <infra-property>} tags. Allows Logback
 * properties to be sourced from the Infra environment.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @see InfraPropertyModel
 * @see InfraPropertyModelHandler
 */
class InfraPropertyAction extends BaseModelAction {

  private static final String SOURCE_ATTRIBUTE = "source";

  private static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";

  @Override
  protected Model buildCurrentModel(SaxEventInterpretationContext context,
          String name, Attributes attributes) {
    InfraPropertyModel model = new InfraPropertyModel();
    model.setName(attributes.getValue(NAME_ATTRIBUTE));
    model.setSource(attributes.getValue(SOURCE_ATTRIBUTE));
    model.setScope(attributes.getValue(SCOPE_ATTRIBUTE));
    model.setDefaultValue(attributes.getValue(DEFAULT_VALUE_ATTRIBUTE));
    return model;
  }

}
