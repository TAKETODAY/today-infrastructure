/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.mapping;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.RandomAccess;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.web.Constant;

/**
 * Store {@link HandlerMapping}
 * 
 * @author Today <br>
 * 
 *         2018-07-1 20:47:06
 */
@Singleton(Constant.HANDLER_MAPPING_REGISTRY)
public class HandlerMappingRegistry implements RandomAccess {

	/** pool **/
	private HandlerMapping[] array;
	/** regex **/
	private RegexMapping[] regexMappings;
	/** mapping */
	private Map<String, Integer> requestMappings;

	public HandlerMappingRegistry setRegexMappings(Map<String, Integer> regexMappings) {
		this.regexMappings = new RegexMapping[regexMappings.size()];
		int i = 0;
		for (Entry<String, Integer> entry : regexMappings.entrySet()) {
			this.regexMappings[i++] = new RegexMapping(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public HandlerMappingRegistry setRequestMappings(Map<String, Integer> requestMappings) {
		this.requestMappings = requestMappings;
		return this;
	}

	public HandlerMappingRegistry() {
		array = new HandlerMapping[0];
	}

	public final RegexMapping[] getRegexMappings() {
		return regexMappings;
	}

	/**
	 * Get HandlerMapping count.
	 * 
	 * @return HandlerMapping count
	 */
	public int size() {
		return array.length;
	}

	/**
	 * Get handler index in array
	 * 
	 * @param key
	 *            request method and request uri
	 * @return
	 */
	public final Integer getIndex(String key) {
		return requestMappings.get(key);
	}

	/**
	 * Get HandlerMapping instance.
	 * 
	 * @param index
	 *            the HandlerMapping number
	 * @return
	 */
	public final HandlerMapping get(int index) {
		return array[index];
	}

	/**
	 * Add HandlerMapping to pool.
	 * 
	 * @param e
	 *            HandlerMapping instance
	 * @return
	 */
	public int add(HandlerMapping e) {

		for (int i = 0; i < array.length; i++) {
			if (e.equals(array[i])) {
				return i;
			}
		}

		HandlerMapping[] newArray = new HandlerMapping[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;

		array = newArray;

		return array.length - 1;
	}

	public String toString() {
		return Arrays.toString(array);
	}

}
