package test.web.action;

import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.Session;
import test.web.domain.User;


@RestController
public class ArticleAction extends BaseAction{

	private static final long serialVersionUID = 3914176913983375014L;

	
	@ActionMapping(value = {"/article"})
	public String index(@Session("USER_INFO") User userInfo) {
		
		
		return userInfo.getUserId();
	}
	
	
	
	
	
	
	
}
