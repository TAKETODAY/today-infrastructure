/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.ssl;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of non-watchable bundle
 * content failures caused by {@link BundleContentNotWatchableException}.
 *
 * @author Moritz Halbritter
 */
class BundleContentNotWatchableFailureAnalyzer extends AbstractFailureAnalyzer<BundleContentNotWatchableException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, BundleContentNotWatchableException cause) {
    return new FailureAnalysis(cause.getMessage(), "Update your application to correct the invalid configuration:\n"
            + "Either use a watchable resource, or disable bundle reloading by setting reload-on-update = false on the bundle.",
            cause);
  }

}
