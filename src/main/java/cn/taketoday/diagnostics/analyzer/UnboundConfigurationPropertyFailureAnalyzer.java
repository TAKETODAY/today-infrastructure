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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.diagnostics.analyzer;

import cn.taketoday.boot.context.properties.bind.BindException;
import cn.taketoday.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import cn.taketoday.boot.context.properties.source.ConfigurationProperty;
import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by any
 * {@link UnboundConfigurationPropertiesException}.
 *
 * @author Madhura Bhave
 */
class UnboundConfigurationPropertyFailureAnalyzer
		extends AbstractFailureAnalyzer<UnboundConfigurationPropertiesException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, UnboundConfigurationPropertiesException cause) {
		BindException exception = findCause(rootFailure, BindException.class);
		return analyzeUnboundConfigurationPropertiesException(exception, cause);
	}

	private FailureAnalysis analyzeUnboundConfigurationPropertiesException(BindException cause,
			UnboundConfigurationPropertiesException exception) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", cause.getTarget()));
		for (ConfigurationProperty property : exception.getUnboundProperties()) {
			buildDescription(description, property);
			description.append(String.format("%n    Reason: %s", exception.getMessage()));
		}
		return getFailureAnalysis(description, cause);
	}

	private void buildDescription(StringBuilder description, ConfigurationProperty property) {
		if (property != null) {
			description.append(String.format("%n    Property: %s", property.getName()));
			description.append(String.format("%n    Value: %s", property.getValue()));
			description.append(String.format("%n    Origin: %s", property.getOrigin()));
		}
	}

	private FailureAnalysis getFailureAnalysis(Object description, BindException cause) {
		return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
	}

}
