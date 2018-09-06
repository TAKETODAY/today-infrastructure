package test.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today
 *
 */
@Setter
@Getter
@AllArgsConstructor
public final class Article implements Serializable {

	private static final long serialVersionUID = 1930544427904752617L;

	private Integer	id		= null;
	private String	title	= null;
	private String	content	= null;
	private User	author	= null;

	
	public Article() {

	}

	@Override
	public String toString() {
		return "{\n\t\"id\":\"" + id + "\",\n\t\"title\":\"" + title + "\",\n\t\"content\":\"" + content
				+ "\",\n\t\"author\":\"" + author + "\"\n}";
	}

}
