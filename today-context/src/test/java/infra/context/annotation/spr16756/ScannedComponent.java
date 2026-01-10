/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation.spr16756;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Scope;
import infra.stereotype.Component;

@Component
public class ScannedComponent {

  @Autowired
  private State state;

  public String iDoAnything() {
    return state.anyMethod();
  }

  public interface State {

    String anyMethod();
  }

  @Component
  @Scope(/*proxyMode = ScopedProxyMode.INTERFACES,*/ value = "prototype")
  public static class StateImpl implements State {

    @Override
    public String anyMethod() {
      return "anyMethod called";
    }
  }

}
