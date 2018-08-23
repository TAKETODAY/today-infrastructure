package test.web.action;


import cn.taketoday.web.Constant;
import cn.taketoday.web.utils.Json;


public class BaseAction implements Constant{
	
	private static final long serialVersionUID = -2443355463459978644L;

	public Json sendToClient(boolean success,String msg) {
		return new Json(success,msg);
	}
	
	public Json sendToClient(boolean success,String msg,Object data) {
		return new Json(success,msg,data);
	}
	
	
}
