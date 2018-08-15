package test.web.domain;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public final class User implements Serializable {

	private static final long	serialVersionUID	= 8795680197276813853L;

	private Integer				id					= null;
	private String				userName			= null;
	private Integer				age					= null;
	private String				passwd				= null;
	private String				userId				= null;
	private String				sex					= null;
	private Date				brithday			= null;

	public User() {

	}

	@Override
	public String toString() {
		return " { \"id\":\"" + id + "\", \"userName\":\"" + userName + "\", \"age\":\"" + age + "\", \"passwd\":\""
				+ passwd + "\", \"userId\":\"" + userId + "\", \"sex\":\"" + sex + "\", \"brithday\":\"" + brithday
				+ "\"}";
	}
}
