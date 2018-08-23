/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.web.action;

import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;

/**
 * @author Today
 * @date 2018年6月29日 下午4:26:38
 */
@RestController
public final class PathVariableAction {

	
	@ActionMapping(value = {"/path/{id}"}, method = RequestMethod.GET)
	public String pathVariable(@PathVariable Integer id) {
		
		return "id -> " + id;
	}
	
	@ActionMapping(value = {"/p/**/yhj.html"}, method = RequestMethod.GET)
	public String path() {
		
		return "/path/**";
	}
	
	@ActionMapping(value = {"/pa/{i}"}, method = RequestMethod.GET)
	public String path(@PathVariable Integer i) {
		
		return "/path/"+ i;
	}
	
	@ActionMapping(value = {"/paths/{name}"}, method = RequestMethod.GET)
	public String path(@PathVariable String name) {
		return name;
	}
	
	
	@ActionMapping(value = {"/path/{name}/{id}.html"}, method = RequestMethod.GET)
	public String path_(@PathVariable String name,@PathVariable Integer id) {
		return "name -> " + name + "/id -> " + id;
	}
	
	@ActionMapping(value = {"/path/{name}/{id}-{today}.html"}, method = RequestMethod.GET)
	public String path_(@PathVariable String name,@PathVariable Integer id, @PathVariable Integer today) {
		return "name -> " + name + "/id -> " + id + "/today->" + today;
	}
	
}






