package com.gerantech.towers.sfs.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.gerantech.towers.sfs.utils.HttpTool;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class IABHandler extends BaseClientRequestHandler 
{

	public IABHandler() {}

	/* (non-Javadoc)
	 * @see com.smartfoxserver.v2.extensions.IClientRequestHandler#handleClientRequest(com.smartfoxserver.v2.entities.User, com.smartfoxserver.v2.entities.data.ISFSObject)
	 */
	public void handleClientRequest(User sender, ISFSObject params)
    {
		trace("verify");
        // Get the client parameters
        //String n1 = params.getText("");
//         
//        // Create a response object
        //ISFSObject resObj = SFSObject.newInstance(); 
//        resObj.putInt("res", n1 + n2);
//         
//        // Send it back
       // send("verify", resObj, sender);

		trace(verify2("coin_pack_03", "SDu10PZdud5JoToeZ"));
    }

	String requestAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "authorization_code"));
		argus.add(new BasicNameValuePair("code", "n5dAwjsTPYUhkKGUzYwSrZwLODdSP7"));
		argus.add(new BasicNameValuePair("client_id", "XFkcFFhCzh8QrtUcrHFm8DDB9Cd9PthIdUXQQyss"));
		argus.add(new BasicNameValuePair("client_secret", "qnbM4vCdkNQOLEva8iAXZ0kYFrEL8YpSYtgtqYkLTcU8O1Hoijkch6U6SZh2"));
		argus.add(new BasicNameValuePair("redirect_uri", "http://www.gerantech.com/tanks/test.php?as=asdasda"));
        return(HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus));
    }

	String refreshAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "refresh_token"));
		argus.add(new BasicNameValuePair("client_id", "XFkcFFhCzh8QrtUcrHFm8DDB9Cd9PthIdUXQQyss"));
		argus.add(new BasicNameValuePair("client_secret", "qnbM4vCdkNQOLEva8iAXZ0kYFrEL8YpSYtgtqYkLTcU8O1Hoijkch6U6SZh2"));
		argus.add(new BasicNameValuePair("refresh_token", "3OqZ66EgXWyxA4WZUGf6iXryCEYQqL"));
		return(HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus));
    }

	String verify(String productID, String purchaseToken)
	{
		String packageName = "air.com.gilaas.tank";
		String accessToken = "t19UX8TKJOF6ycJ8LHu5gHRqxIT8oV";
		return(HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/api/access_token/"+accessToken+"/validate/"+packageName+"/inapp/"+productID+"/purchases/"+purchaseToken, null));
	}
	
	String verify2(String productID, String purchaseToken)
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		//argus.add(new BasicNameValuePair("access_token", "t19UX8TKJOF6ycJ8LHu5gHRqxIT8oV"));
		argus.add(new BasicNameValuePair("validate", "air.com.gilaas.tank"));
		argus.add(new BasicNameValuePair("inapp", productID));
		argus.add(new BasicNameValuePair("purchases", purchaseToken));
		return(HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/api/", argus));
	}
}