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

package infra.annotation.config.validation;

import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Role;
import infra.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for validation.
 *
 * @author Yanming Zhou
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConfigurationProperties("infra.validation")
public class ValidationProperties {

  private Method method = new Method();

  public Method getMethod() {
    return this.method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  /**
   * Method validation properties.
   */
  public static class Method {

    /**
     * Whether to adapt ConstraintViolations to MethodValidationResult.
     */
    private boolean adaptConstraintViolations;

    public boolean isAdaptConstraintViolations() {
      return this.adaptConstraintViolations;
    }

    public void setAdaptConstraintViolations(boolean adaptConstraintViolations) {
      this.adaptConstraintViolations = adaptConstraintViolations;
    }

  }

}
