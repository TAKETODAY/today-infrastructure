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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelUtil;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Nullable;

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
    ModelUtil.setProperty(intercon, propertyModel.getName(), getValue(source, defaultValue), scope);
  }

  private String getValue(String source, String defaultValue) {
    if (environment == null) {
      addWarn("No Infra environment available to resolve " + source);
      return defaultValue;
    }
    return environment.getProperty(source, defaultValue);
  }

}
