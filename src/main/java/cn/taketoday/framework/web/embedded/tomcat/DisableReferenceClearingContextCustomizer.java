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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;

/**
 * A {@link TomcatContextCustomizer} that disables Tomcat's reflective reference clearing
 * to avoid reflective access warnings on Java 9 and later JVMs.
 *
 * @author Andy Wilkinson
 */
class DisableReferenceClearingContextCustomizer implements TomcatContextCustomizer {

  @Override
  public void customize(Context context) {
    if (!(context instanceof StandardContext)) {
      return;
    }
    StandardContext standardContext = (StandardContext) context;
    try {
      standardContext.setClearReferencesObjectStreamClassCaches(false);
      standardContext.setClearReferencesRmiTargets(false);
      standardContext.setClearReferencesThreadLocals(false);
    }
    catch (NoSuchMethodError ex) {
      // Earlier version of Tomcat (probably without
      // setClearReferencesThreadLocals). Continue.
    }
  }

}
