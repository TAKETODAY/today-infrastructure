/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;

/**
 * Settings to apply when configuring Hibernate.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public class HibernateSettings {

  @Nullable
  private Supplier<String> ddlAuto;

  @Nullable
  private HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;

  private Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

  public HibernateSettings ddlAuto(HibernateDefaultDdlAutoProvider defaultDdlAutoProvider) {
    this.defaultDdlAutoProvider = defaultDdlAutoProvider;
    return this;
  }

  public HibernateSettings ddlAuto(Supplier<String> ddlAuto) {
    this.ddlAuto = ddlAuto;
    return this;
  }

  @Nullable
  public String getDdlAuto() {
    if (ddlAuto != null) {
      return ddlAuto.get();
    }
    if (defaultDdlAutoProvider != null) {
      return defaultDdlAutoProvider.getDefaultDdlAuto();
    }
    return null;
  }

  public HibernateSettings hibernatePropertiesCustomizers(
          Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
    this.hibernatePropertiesCustomizers = new ArrayList<>(hibernatePropertiesCustomizers);
    return this;
  }

  public Collection<HibernatePropertiesCustomizer> getHibernatePropertiesCustomizers() {
    return this.hibernatePropertiesCustomizers;
  }

}
