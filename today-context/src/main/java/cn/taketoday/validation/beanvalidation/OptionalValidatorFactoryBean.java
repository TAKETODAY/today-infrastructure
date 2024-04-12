/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.validation.beanvalidation;

import cn.taketoday.logging.LoggerFactory;
import jakarta.validation.ValidationException;

/**
 * {@link LocalValidatorFactoryBean} subclass that simply turns
 * {@link cn.taketoday.validation.Validator} calls into no-ops
 * in case of no Bean Validation provider being available.
 *
 * <p>This is the actual class used by Framework's MVC configuration namespace,
 * in case of the {@code jakarta.validation} API being present but no explicit
 * Validator having been configured.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OptionalValidatorFactoryBean extends LocalValidatorFactoryBean {

  @Override
  public void afterPropertiesSet() {
    try {
      super.afterPropertiesSet();
    }
    catch (ValidationException ex) {
      LoggerFactory.getLogger(getClass()).debug("Failed to set up a Bean Validation provider", ex);
    }
  }

}
