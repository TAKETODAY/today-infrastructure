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

import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;
import cn.taketoday.boot.web.server.PortInUseException;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PortInUseException}.
 *
 * @author Andy Wilkinson
 */
class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer<PortInUseException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, PortInUseException cause) {
    return new FailureAnalysis("Web server failed to start. Port " + cause.getPort() + " was already in use.",
            "Identify and stop the process that's listening on port " + cause.getPort() + " or configure this "
                    + "application to listen on another port.",
            cause);
  }

}
