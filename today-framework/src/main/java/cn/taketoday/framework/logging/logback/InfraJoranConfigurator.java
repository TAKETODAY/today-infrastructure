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

package cn.taketoday.framework.logging.logback;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import cn.taketoday.framework.logging.LoggingStartupContext;

/**
 * Extended version of the Logback {@link JoranConfigurator} that adds additional Infra
 * Boot rules.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class InfraJoranConfigurator extends JoranConfigurator {

  private final LoggingStartupContext startupContext;

  InfraJoranConfigurator(LoggingStartupContext startupContext) {
    this.startupContext = startupContext;
  }

  @Override
  protected void sanityCheck(Model topModel) {
    super.sanityCheck(topModel);
    performCheck(new InfraProfileIfNestedWithinSecondPhaseElementSanityChecker(), topModel);
  }

  @Override
  protected void addModelHandlerAssociations(DefaultProcessor processor) {
    processor.addHandler(InfraPropertyModel.class,
            (handlerContext, handlerMic) ->
                    new InfraPropertyModelHandler(context, startupContext.getEnvironment()));
    processor.addHandler(InfraProfileModel.class,
            (handlerContext, handlerMic) ->
                    new InfraProfileModelHandler(context, startupContext.getEnvironment()));
    super.addModelHandlerAssociations(processor);
  }

  @Override
  public void addElementSelectorAndActionAssociations(RuleStore ruleStore) {
    super.addElementSelectorAndActionAssociations(ruleStore);
    ruleStore.addRule(new ElementSelector("configuration/infra-property"), InfraPropertyAction::new);
    ruleStore.addRule(new ElementSelector("*/infra-profile"), InfraProfileAction::new);
    ruleStore.addTransparentPathPart("infra-profile");
  }

}
