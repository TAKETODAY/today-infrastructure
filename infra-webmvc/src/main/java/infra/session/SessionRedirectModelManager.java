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

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.web.AbstractRedirectModelManager;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.ServerHttpExchange;
import infra.web.util.WebUtils;

/**
 * Store {@link RedirectModel} in {@link Session}
 *
 * @author TODAY 2021/4/2 21:55
 * @since 3.0
 */
public class SessionRedirectModelManager extends AbstractRedirectModelManager implements RedirectModelManager {

  private static final String SESSION_ATTRIBUTE = SessionRedirectModelManager.class.getName() + ".RedirectModel";

  @Override
  @SuppressWarnings("unchecked")
  protected @Nullable List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
    return (List<RedirectModel>) request.getSessionAttribute(SESSION_ATTRIBUTE);
  }

  @Override
  protected void updateRedirectModel(List<RedirectModel> redirectModels, ServerHttpExchange request) {
    request.setSessionAttribute(SESSION_ATTRIBUTE, redirectModels.isEmpty() ? null : redirectModels);
  }

  @Override
  protected @Nullable Object getRedirectModelMutex(ServerHttpExchange request) {
    Session session = request.getSession(false);
    if (session != null) {
      return WebUtils.getSessionMutex(session);
    }
    return null;
  }

}
