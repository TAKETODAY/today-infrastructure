/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package cn.taketoday.web.view;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author Today
 * @date 2018年6月26日 下午12:46:33
 */
public final class AllScopesModel extends SimpleHash {

	private static final long			serialVersionUID	= -1105666491730662342L;

	private final ObjectWrapper			wrapper;
	private final ServletContext		context;
	private final HttpServletRequest	request;
	
	private final Map<String, Object>	unlistedModels		= new HashMap<>();

	public AllScopesModel(ObjectWrapper wrapper, ServletContext context, HttpServletRequest request) {
		this.wrapper = wrapper;
		this.context = context;
		this.request = request;
	}

	/**
	 * Stores a model in the hash so that it doesn't show up in <tt>keys()</tt> and
	 * <tt>values()</tt> methods. Used to put the Application, Session, Request,
	 * RequestParameters and JspTaglibs objects.
	 * 
	 * @param key
	 *            the key under which the model is stored
	 * @param model
	 *            the stored model
	 */
	public void putUnlistedModel(String key, TemplateModel model) {
		unlistedModels.put(key, model);
	}

	public TemplateModel get(String key) throws TemplateModelException {
		// Lookup in page scope
		TemplateModel model = super.get(key);
		if (model != null) {
			return model;
		}

		// Look in unlisted models
		model = (TemplateModel) unlistedModels.get(key);
		if (model != null) {
			return model;
		}

		// Lookup in request scope
		Object obj = request.getAttribute(key);
		if (obj != null) {
			return wrapper.wrap(obj);
		}

		// Lookup in session scope
		HttpSession session = request.getSession(false);
		if (session != null) {
			obj = session.getAttribute(key);
			if (obj != null) {
				return wrapper.wrap(obj);
			}
		}

		// Lookup in application scope
		obj = context.getAttribute(key);
		if (obj != null) {
			return wrapper.wrap(obj);
		}
		// return wrapper's null object (probably null).
		return wrapper.wrap(null);
	}
}
