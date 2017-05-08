package com.gerantech.towers.sfs.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.gerantech.towers.sfs.utils.HttpTool;
import com.gerantech.towers.sfs.utils.HttpTool.Data;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * @author ManJav
 *
 */
public class IABHandler extends BaseClientRequestHandler 
{

	private static String packageName = "air.com.gilaas.tank";
	private static String accessToken = "8tVrY3BKnp08BoW1MEVEvLQjVzagnB";
	
	public IABHandler() {}

	/* (non-Javadoc)
	 * @see com.smartfoxserver.v2.extensions.IClientRequestHandler#handleClientRequest(com.smartfoxserver.v2.entities.User, com.smartfoxserver.v2.entities.data.ISFSObject)
	 */
	public void handleClientRequest(User sender, ISFSObject params)
    {
        // Get the client parameters
        String productID = params.getText("productID");//"coin_pack_03";//
        String purchaseToken = params.getText("purchaseToken");//"SDu10PZdud5JoToeZs";//
        sendResult(sender, productID, purchaseToken);
   }

	private void sendResult(User sender, String productID, String purchaseToken)
	{
        // Create a response object
        ISFSObject resObj = SFSObject.newInstance(); 
        Data data = verify(productID, purchaseToken);
        
        if(data.statusCode == HttpStatus.SC_OK)
        {
    		resObj.putBool("success", true);
    		resObj.putInt("consumptionState", data.json.getInt("consumptionState"));
    		resObj.putInt("purchaseState", data.json.getInt("purchaseState"));
    		resObj.putText("developerPayload", data.json.getString("developerPayload"));
    		resObj.putLong("purchaseTime", data.json.getLong("purchaseTime"));
    	    send("verify", resObj, sender);		
    		return;
        }
        
        if(data.statusCode == HttpStatus.SC_NOT_FOUND)
		{
			resObj.putBool("success", false);
			resObj.putText("message", data.json.getString("error_description"));
		    send("verify", resObj, sender);
    		return;
	    }
        
		if(data.statusCode == HttpStatus.SC_UNAUTHORIZED)
		{
			if(refreshAccessToken())
		        sendResult(sender, productID, purchaseToken);
	        else
	        {
				resObj.putBool("success", false);
				resObj.putText("error_description", "refresh access token faild.");
			    send("verify", resObj, sender);
			    trace(ExtensionLogLevel.ERROR, "refresh access token faild.");
			}
    		return;
		}
	    trace(ExtensionLogLevel.ERROR, "Unknown Error.");
	}

	String requestAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "authorization_code"));
		argus.add(new BasicNameValuePair("code", "n5dAwjsTPYUhkKGUzYwSrZwLODdSP7"));
		argus.add(new BasicNameValuePair("client_id", "XFkcFFhCzh8QrtUcrHFm8DDB9Cd9PthIdUXQQyss"));
		argus.add(new BasicNameValuePair("client_secret", "qnbM4vCdkNQOLEva8iAXZ0kYFrEL8YpSYtgtqYkLTcU8O1Hoijkch6U6SZh2"));
		argus.add(new BasicNameValuePair("redirect_uri", "http://www.gerantech.com/tanks/test.php?as=asdasda"));
        return(HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus).text);
    }

	Boolean refreshAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "refresh_token"));
		argus.add(new BasicNameValuePair("client_id", "XFkcFFhCzh8QrtUcrHFm8DDB9Cd9PthIdUXQQyss"));
		argus.add(new BasicNameValuePair("client_secret", "qnbM4vCdkNQOLEva8iAXZ0kYFrEL8YpSYtgtqYkLTcU8O1Hoijkch6U6SZh2"));
		argus.add(new BasicNameValuePair("refresh_token", "3OqZ66EgXWyxA4WZUGf6iXryCEYQqL"));
		Data data = HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus);
		//trace("refresh_token", data.statusCode, data.text);
		if(data.statusCode != HttpStatus.SC_OK || !data.json.containsKey("access_token") )
			return false;

		accessToken = data.json.getString("access_token");
		return true;
    }

	Data verify(String productID, String purchaseToken)
	{
		Data data = HttpTool.get("https://pardakht.cafebazaar.ir/devapi/v2/api/validate/"+packageName+"/inapp/"+productID+"/purchases/"+purchaseToken+"/?access_token="+accessToken);
		//trace("verify", data.statusCode, data.text);
		return data;
	}	
}