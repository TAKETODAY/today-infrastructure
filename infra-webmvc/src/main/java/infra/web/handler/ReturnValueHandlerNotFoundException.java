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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import infra.web.InfraConfigurationException;
import infra.web.ReturnValueHandler;

/**
 * For {@link ReturnValueHandler} not found
 *
 * @author TODAY 2021/4/26 22:18
 * @since 3.0
 */
public class ReturnValueHandlerNotFoundException extends InfraConfigurationException {

  @Nullable
  private final Object handler;

  @Nullable
  private final Object returnValue;

  /**
   * @param handler target handler
   */
  public ReturnValueHandlerNotFoundException(Object handler) {
    super("No ReturnValueHandler for handler: [%s]".formatted(handler));
    this.returnValue = null;
    this.handler = handler;
  }

  /**
   * @param returnValue handler's return value or execution result
   * @param handler target handler
   */
  public ReturnValueHandlerNotFoundException(@Nullable Object returnValue, @Nullable Object handler) {
    super("No ReturnValueHandler for return-value: [%s]".formatted(returnValue));
    this.returnValue = returnValue;
    this.handler = handler;
  }

  @Nullable
  public Object getHandler() {
    return handler;
  }

  @Nullable
  public Object getReturnValue() {
    return returnValue;
  }

}
