package com.gerantech.towers.sfs.handlers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gerantech.towers.sfs.TowerExtension;
import com.gt.towers.Game;
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
public class CafeBazaarVerificationHandler extends BaseClientRequestHandler 
{

	private static String packageName = "air.com.grantech.towers";
	private static String accessToken = "riN8RxzQMsC9x05kCz8EWscxwjSu7r";
	
	public CafeBazaarVerificationHandler() {}

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
        
        // send purchase data to client
        // if consumptionState is zero, thats mean product consumed.
        if(data.statusCode == HttpStatus.SC_OK)
        {
			Game game = ((Game)sender.getSession().getProperty("core"));
			ExchangeHandler exchangeHandler = ((TowerExtension) getParentExtension()).exchangeHandler;
			int item = Integer.parseInt(productID.substring(productID.length()-1, productID.length() ));
			if( !exchangeHandler.exchange(game, item, 0, 0, false))
			{
				resObj.putBool("success", false);
				resObj.putText("message", data.json.getString("error_exchange"));
				return;
			}

    		resObj.putBool("success", true);
    		resObj.putInt("consumptionState", data.json.getInt("consumptionState"));
    		resObj.putInt("purchaseState", data.json.getInt("purchaseState"));
    		resObj.putText("developerPayload", data.json.getString("developerPayload"));
    		resObj.putLong("purchaseTime", data.json.getLong("purchaseTime"));

    		insertToDB(game, productID, purchaseToken, resObj);
    	    send("verify", resObj, sender);		
    		return;
        }
        
        // when product id or purchase token is wrong
        if(data.statusCode == HttpStatus.SC_NOT_FOUND)
		{
			resObj.putBool("success", false);
			resObj.putText("message", data.json.getString("error_description"));
		    send("verify", resObj, sender);
    		return;
	    }
        
        // when access token expired
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
		
        // unknown error
	    trace(ExtensionLogLevel.ERROR, "Unknown Error.");
	}

	private void insertToDB(Game game, String productID, String purchaseToken, ISFSObject result)
	{
		String query = "INSERT INTO `purchases`(`product_id`, `player_id`, `market`, `purchase_token`, `consume_state`, `purchase_state`, `purchase_time`, `success`) VALUES (" +
		productID+", "+game.player.id+", "+game.market+", "+purchaseToken+ ","+result.getInt("consumptionState")+","+result.getInt("purchaseState")+","+result.getLong("purchaseTime")+"," +1+")";
		try {
				getParentExtension().getParentZone().getDBManager().executeInsert(query, new Object[]{});
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method only called in initial setup
	 * @return json string contains:<br/>
	 *  <b>"access_token"</b>: access token needs per verification.<br/>
	 *  <b>"token_type"</b>: "Bearer"<br/>
	 *  <b>"expires_in"</b>: after expires_in seconds, access_token expired.<br/>
	 *  <b>"refresh_token"</b>: we need refresh token for get new access token when expired.<br/>
	 *  <b>"scope"</b>: "androidpublisher"
	 */
	String requestAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "authorization_code"));
		argus.add(new BasicNameValuePair("code", "OerQgUmJk5U2ASq8UqiGA98nPyDlHq"));
		argus.add(new BasicNameValuePair("client_id", "1PsJN4ZdDKrolOyuDRLKQZaYKhTnIrmbSkaHK40L"));
		argus.add(new BasicNameValuePair("client_secret", "C1nYSNSzbP72dK9J0VysZzbS8bo55AjB0UKl7X6hiCLdYACizDEeyLHoVKZt"));
		argus.add(new BasicNameValuePair("redirect_uri", "http://www.gerantech.com/tanks/test.php?a=b"));
		Data data = HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus);
		trace("request_AccessToken", data.statusCode, data.text);
		return(data.text);
    }

	/**
	 * This method called when access token expired.<br/>
	 * Web request get json string contains:<br/>
	 * <b>"access_token"</b>: access token needs per verification.<br/>
	 * <b>"token_type"</b>: "Bearer"<br/>
	 * <b>"expires_in"</b>: after expires_in seconds, access_token expired.<br/>
	 * <b>"scope"</b>: "androidpublisher"
	 * @return boolean value <br/>
	 * if access dtoken refreshed return true else false
	 */
	Boolean refreshAccessToken()
	{
		List<NameValuePair> argus = new ArrayList<NameValuePair>();
		argus.add(new BasicNameValuePair("grant_type", "refresh_token"));
		argus.add(new BasicNameValuePair("client_id", "1PsJN4ZdDKrolOyuDRLKQZaYKhTnIrmbSkaHK40L"));
		argus.add(new BasicNameValuePair("client_secret", "C1nYSNSzbP72dK9J0VysZzbS8bo55AjB0UKl7X6hiCLdYACizDEeyLHoVKZt"));
		argus.add(new BasicNameValuePair("refresh_token", "7Q3ZAgkZyDTd5Iftdpbvq09IPF2iyh"));
		Data data = HttpTool.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus);
		trace("refresh_token", data.statusCode, data.text);
		if(data.statusCode != HttpStatus.SC_OK || !data.json.containsKey("access_token") )
			return false;

		accessToken = data.json.getString("access_token");
		return true;
    }

	/**
	 * Server side purchase verification method.<br/>
	 * Web request get json string if succeed, contains:<br/>
	 * 	<b>"consumptionState"</b>: if consumptionState is zero, thats mean product consumed.<br/>
	 * 	<b>"purchaseState"</b>: type of purchase.<br/>
	 * 	<b>"kind"</b>: "androidpublisher#inappPurchase"<br/>
	 * 	<b>"developerPayload"</b>: the payload that developer when started purchase flow send to market server.<br/>
	 * 	<b>"purchaseTime"</b>: purchase time in miliseconds<br/>
	 * @return Data <br/>
	 */
	Data verify(String productID, String purchaseToken)
	{
		Data data = HttpTool.get("https://pardakht.cafebazaar.ir/devapi/v2/api/validate/"+packageName+"/inapp/"+productID+"/purchases/"+purchaseToken+"/?access_token="+accessToken);
		trace("verify", data.statusCode, data.text);
		return data;
	}	
}