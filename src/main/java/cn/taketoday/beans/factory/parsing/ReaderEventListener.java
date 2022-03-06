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

import java.util.EventListener;

/**
 * Interface that receives callbacks for component, alias and import
 * registrations during a bean definition reading process.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 * @see ReaderContext
 */
public interface ReaderEventListener extends EventListener {

	/**
	 * Notification that the given defaults has been registered.
	 * @param defaultsDefinition a descriptor for the defaults
	 * @see cn.taketoday.beans.factory.xml.DocumentDefaultsDefinition
	 */
	void defaultsRegistered(DefaultsDefinition defaultsDefinition);

	/**
	 * Notification that the given component has been registered.
	 * @param componentDefinition a descriptor for the new component
	 * @see BeanComponentDefinition
	 */
	void componentRegistered(ComponentDefinition componentDefinition);

	/**
	 * Notification that the given alias has been registered.
	 * @param aliasDefinition a descriptor for the new alias
	 */
	void aliasRegistered(AliasDefinition aliasDefinition);

	/**
	 * Notification that the given import has been processed.
	 * @param importDefinition a descriptor for the import
	 */
	void importProcessed(ImportDefinition importDefinition);

}
