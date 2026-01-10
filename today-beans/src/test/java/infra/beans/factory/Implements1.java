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

package infra.beans.factory;

import infra.core.testfixture.stereotype.Component;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author Today <br>
 *
 * 2019-01-22 19:41
 */
@Component
public class Implements1 implements Interface {
  private static final Logger log = LoggerFactory.getLogger(Implements1.class);

  @Override
  public void test() {
    log.debug("Implements1");
  }

}
