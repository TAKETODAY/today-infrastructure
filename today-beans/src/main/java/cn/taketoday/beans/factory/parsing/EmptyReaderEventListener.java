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

package cn.taketoday.beans.factory.parsing;

/**
 * Empty implementation of the {@link ReaderEventListener} interface,
 * providing no-op implementations of all callback methods.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class EmptyReaderEventListener implements ReaderEventListener {

  @Override
  public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
    // no-op
  }

  @Override
  public void componentRegistered(ComponentDefinition componentDefinition) {
    // no-op
  }

  @Override
  public void aliasRegistered(AliasDefinition aliasDefinition) {
    // no-op
  }

  @Override
  public void importProcessed(ImportDefinition importDefinition) {
    // no-op
  }

}
