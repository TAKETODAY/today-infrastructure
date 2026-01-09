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
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import infra.core.env.Environment;
import infra.core.env.Profiles;
import infra.util.ExceptionUtils;
import infra.util.StringUtils;

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

  @SuppressWarnings("NullAway")
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
