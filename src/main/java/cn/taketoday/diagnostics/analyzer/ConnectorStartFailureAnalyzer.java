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
import cn.taketoday.boot.web.embedded.tomcat.ConnectorStartFailedException;

/**
 * An {@link AbstractFailureAnalyzer} for {@link ConnectorStartFailedException}.
 *
 * @author Andy Wilkinson
 */
class ConnectorStartFailureAnalyzer extends AbstractFailureAnalyzer<ConnectorStartFailedException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, ConnectorStartFailedException cause) {
    return new FailureAnalysis(
            "The Tomcat connector configured to listen on port " + cause.getPort()
                    + " failed to start. The port may already be in use or the connector may be misconfigured.",
            "Verify the connector's configuration, identify and stop any process that's listening on port "
                    + cause.getPort() + ", or configure this application to listen on another port.",
            cause);
  }

}
