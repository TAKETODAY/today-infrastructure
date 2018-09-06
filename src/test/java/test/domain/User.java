package test.domain;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
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

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"id\":\"")//
				.append(id)//
				.append("\",\n\t\"userName\":\"")//
				.append(userName)//
				.append("\",\n\t\"age\":\"")//
				.append(age)//
				.append("\",\n\t\"passwd\":\"")//
				.append(passwd)//
				.append("\",\n\t\"userId\":\"")//
				.append(userId)//
				.append("\",\n\t\"sex\":\"")//
				.append(sex)//
				.append("\",\n\t\"brithday\":\"")//
				.append(brithday)//
				.append("\"\n}")//
				.toString();
	}

}
