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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.model.util.PropertyModelHandlerHelper;
import ch.qos.logback.core.util.OptionHelper;
import infra.core.env.Environment;

/**
 * Logback {@link ModelHandlerBase model handler} to support {@code <infra-property>}
 * tags. Allows Logback properties to be sourced from the Infra environment.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraPropertyAction
 * @see InfraPropertyModel
 * @since 4.0
 */
class InfraPropertyModelHandler extends ModelHandlerBase {

  @Nullable
  private final Environment environment;

  InfraPropertyModelHandler(Context context, @Nullable Environment environment) {
    super(context);
    this.environment = environment;
  }

  @Override
  public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
    InfraPropertyModel propertyModel = (InfraPropertyModel) model;
    Scope scope = ActionUtil.stringToScope(propertyModel.getScope());
    String defaultValue = propertyModel.getDefaultValue();
    String source = propertyModel.getSource();
    if (OptionHelper.isNullOrEmpty(propertyModel.getName()) || OptionHelper.isNullOrEmpty(source)) {
      addError("The \"name\" and \"source\" attributes of <infra-property> must be set");
    }
    PropertyModelHandlerHelper.setProperty(intercon,
            propertyModel.getName(), getValue(source, defaultValue), scope);
  }

  private String getValue(String source, String defaultValue) {
    if (environment == null) {
      addWarn("No Infra environment available to resolve " + source);
      return defaultValue;
    }
    return environment.getProperty(source, defaultValue);
  }

}
