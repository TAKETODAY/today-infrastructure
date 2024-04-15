/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.Profiles;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Logback {@link ModelHandlerBase model handler} to support {@code <infra-profile>} tags.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraProfileModel
 * @see InfraProfileAction
 * @since 4.0
 */
class InfraProfileModelHandler extends ModelHandlerBase {

  @Nullable
  private final Environment environment;

  InfraProfileModelHandler(Context context, @Nullable Environment environment) {
    super(context);
    this.environment = environment;
  }

  @Override
  public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
    InfraProfileModel profileModel = (InfraProfileModel) model;
    if (!acceptsProfiles(intercon, profileModel)) {
      model.deepMarkAsSkipped();
    }
  }

  private boolean acceptsProfiles(ModelInterpretationContext ic, InfraProfileModel model) {
    if (this.environment == null) {
      return false;
    }
    String[] profileNames = StringUtils.trimArrayElements(
            StringUtils.commaDelimitedListToStringArray(model.getName()));
    if (profileNames.length == 0) {
      return false;
    }
    for (int i = 0; i < profileNames.length; i++) {
      try {
        profileNames[i] = OptionHelper.substVars(profileNames[i], ic, this.context);
      }
      catch (ScanException ex) {
        throw ExceptionUtils.sneakyThrow(ex);
      }
    }
    return this.environment.acceptsProfiles(Profiles.parse(profileNames));
  }

}
