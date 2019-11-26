package com.gerantech.towers.sfs.handlers;

import com.gt.towers.exchanges.ExchangeItem;
import com.gt.utils.ExchangeUtils;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.utils.HttpUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ManJav
 *
 */
public class PurchaseVerificationHandler extends BaseClientRequestHandler
{

	private ExchangeItem item;
	private static String packageName = "***";
	private static String accessToken_cafebazaar = "***";

	public void handleClientRequest(User sender, ISFSObject params)
	{
		// Get the client parameters
		String productID = params.getText("productID");//com.grantech.towers.item_x
		String purchaseToken = params.getText("purchaseToken");
		if( params.containsKey("consume") )
		{
			consume(purchaseToken);
			return;
		}

		sendResult(sender, productID, purchaseToken);
	}

	private void sendResult(User sender, String productID, String purchaseToken)
	{
		// Create a response object
		ISFSObject resObj = SFSObject.newInstance();
		Game game = ((Game)sender.getSession().getProperty("core"));
		item = game.exchanger.items.get(Integer.parseInt(productID.replaceAll("^.*_", "")));

		trace("Player Purchase --playerId:", game.player.id, "--market:", game.market,  "--productID:", productID, "--purchaseToken:", purchaseToken, "--Hard Currency:", game.player.resources.get(ResourceType.R4_CURRENCY_HARD) );
		if( !game.market.equals("cafebazaar") && !game.market.equals("myket") && !game.market.equals("zarinpal") )
		{
			sendSuccessResult(sender, game, productID, purchaseToken, 1, 0, "", Instant.now().toEpochMilli());
			return;
		}

		HttpUtils.Data data;
		// If market is zarinpal we verify it with amount
		if( game.market.equals("zarinpal") )
		{
			String amount = Integer.toString(item.requirements.get(ResourceType.R5_CURRENCY_REAL));
			data = verify(productID, purchaseToken, game.market, amount);
		}
		else
		{
			data = verify(productID, purchaseToken, game.market, null);
		}

		// send purchase data to client
		// if consumptionState is zero, its means the product consumed.
		if( data.statusCode == HttpStatus.SC_OK )
		{
			if( game.market.equals("zarinpal") )
			{
				if( data.json.getInt("Status") == 100 )
				{
					String refid = data.json.getString("RefID");
					sendSuccessResult(sender, game, productID, refid, 0, 0, "", Instant.now().toEpochMilli());
				}
				else if( data.json.getInt("Status") == 101 )
				{
					resObj.putBool("success", false);
					resObj.putText("message", "already used");
					send("verify", resObj, sender);
				}
				else
				{
					resObj.putBool("success", false);
					resObj.putText("message", "not valid");
					send("verify", resObj, sender);
				}
				return;
			}

			sendSuccessResult(sender, game, productID, purchaseToken, data.json.getInt("consumptionState"), data.json.getInt("purchaseState"), data.json.getString("developerPayload"), data.json.getLong("purchaseTime"));
			return;
		}

		// when product id or purchase token is wrong
		if( data.statusCode == HttpStatus.SC_NOT_FOUND )
		{
			resObj.putBool("success", false);
			resObj.putText("message", data.json.getString("error_description"));
			send("verify", resObj, sender);
			return;
		}

		// when access token expired
		if( data.statusCode == HttpStatus.SC_UNAUTHORIZED )
		{
			if( refreshAccessToken() )
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

	private void sendSuccessResult(User sender, Game game, String productID, String purchaseToken, int consumptionState, int purchaseState, String developerPayload, long purchaseTime)
	{
		ISFSObject resObj = SFSObject.newInstance();
		if( purchaseState != 0 )
		{
			resObj.putBool("success", false);
			resObj.putText("message", "error in verification");
			trace("Purchase failed: response:" + purchaseState + " --playerId:", game.player.id, "--market:", game.market,  "--productID:", productID, "--purchaseToken:", purchaseToken, "--purchaseState:", purchaseState, "--Hard Currency:",  getHardOnDB(game.player.id), "Error Message: In exchange");
			return;
		}

		String beforePurchaseData = game.player.resources.toString();
		if( item.category == ExchangeType.C0_HARD )
		{
			int res = ExchangeUtils.getInstance().process(game, item.type, 0, 0);
			if( res != MessageTypes.RESPONSE_SUCCEED )
			{
				resObj.putBool("success", false);
				resObj.putText("message", "error in exchange");
				trace("Purchase failed: response:" + res + " --playerId:", game.player.id, "--market:", game.market,  "--productID:", productID, "--purchaseToken:", purchaseToken, "--purchaseState:", purchaseState, "--Hard Currency:",  getHardOnDB(game.player.id), "Error Message: In exchange");
				return;
			}
		}
		String afterPurchaseData = game.player.resources.toString();

		resObj.putBool("success", true);
		resObj.putText("productID", productID);
		resObj.putInt("consumptionState", consumptionState);
		resObj.putInt("purchaseState", purchaseState);
		resObj.putText("developerPayload", developerPayload);
		resObj.putLong("purchaseTime", purchaseTime);
		insertToDB(game, productID, purchaseToken, purchaseState, purchaseTime, item.requirements.values()[0], beforePurchaseData, afterPurchaseData);
		send("verify", resObj, sender);
		trace("Purchase Succeed --playerId:", game.player.id, "--market:", game.market,  "--productID:", productID, "--purchaseToken:", purchaseToken, "--Hard Currency:", getHardOnDB(game.player.id) );
	}

	private void insertToDB(Game game, String id, String token, int state, long time, int price, String beforePurchaseData, String afterPurchaseData)
	{
		String query = "INSERT INTO purchases(player_id, id, market, token, consumed, state, time, price, old_res, new_res ) VALUES (" + game.player.id + ", '" + id + "', '" + game.market + "', '" + token + "', 1, " + state + ", FROM_UNIXTIME(" + (time/1000) + "), " + price + ", '" + beforePurchaseData + "', '" + afterPurchaseData + "') ON DUPLICATE KEY UPDATE consumed = VALUES(consumed), state = VALUES(state)";
		trace(query);
		try {
			getParentExtension().getParentZone().getDBManager().executeInsert(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
	}

	private int getHardOnDB(int playerID)
	{
		ISFSArray res = null;
		try {
			res = getParentExtension().getParentZone().getDBManager().executeQuery("SELECT count From resources WHERE player_id = " + playerID + " AND type = 1003", new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		if( res != null && res.size() > 0 )
			return res.getSFSObject(0).getInt("count");
		return 0;
	}

	private void consume(String token)
	{
		String query = "UPDATE `purchases` SET `consumed`=0 WHERE `token`='" + token + "'";
		trace(query);
		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[]{});
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
		List<NameValuePair> argus = new ArrayList();
		argus.add(new BasicNameValuePair("grant_type", "authorization_code"));
		argus.add(new BasicNameValuePair("code", "***"));
		argus.add(new BasicNameValuePair("client_id", "***"));
		argus.add(new BasicNameValuePair("client_secret", "***"));
		argus.add(new BasicNameValuePair("redirect_uri", "http://www.gerantech.com/tanks/test.php?a=b"));
		HttpUtils.Data data = HttpUtils.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus, true);
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
		List<NameValuePair> argus = new ArrayList();
		argus.add(new BasicNameValuePair("grant_type", "refresh_token"));
		argus.add(new BasicNameValuePair("client_id", "***"));
		argus.add(new BasicNameValuePair("client_secret", "***"));
		argus.add(new BasicNameValuePair("refresh_token", "***"));
		HttpUtils.Data data = HttpUtils.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus, true);
		trace("refresh_token", data.statusCode, data.text);
		if(data.statusCode != HttpStatus.SC_OK || !data.json.containsKey("access_token") )
			return false;

		accessToken_cafebazaar = data.json.getString("access_token");
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
	HttpUtils.Data verify(String productID, String purchaseToken, String market, String amount)
	{
		// set headers
		Map<String, String> headers = new HashMap();
		if( market.equals("myket") )
		{
			headers.put("X-Access-Token", "4cc2d302-836c-460e-a3a7-e72c8cd9c666");
		}
		else if( market.equals("zarinpal") )
		{
			headers.put("User-Agent", "Zarinpal REST");
			headers.put("Content-Type", "application/json");
		}
		String url = null;
		// set url
		if( market.equals("zarinpal") )
		{
			url = "https://www.zarinpal.com/pg/rest/WebGate/PaymentVerification.json";
			List<NameValuePair> argus = new ArrayList<>();
			argus.add(new BasicNameValuePair("MerchantID", "b37e90ce-b2bc-11e9-832c-000c29344814"));
			argus.add(new BasicNameValuePair("Authority", purchaseToken));
			argus.add(new BasicNameValuePair("Amount", amount));
			HttpUtils.Data data = HttpUtils.post(url, argus, true, true);
			
			trace("verify", data.statusCode, data.text);
			return data;
		}

		if( market.equals("myket") )
			url = "https://developer.myket.ir/api/applications/" + packageName + "/purchases/products/" + productID + "/tokens/" + purchaseToken;
		else if( market.equals("cafebazaar") )
			url = "https://pardakht.cafebazaar.ir/devapi/v2/api/validate/"+packageName+"/inapp/"+productID+"/purchases/"+purchaseToken+"/?access_token="+ accessToken_cafebazaar;

		//trace("purchase url:", url);
		HttpUtils.Data data = HttpUtils.get(url, headers, true);
		trace("verify", data.statusCode, data.text);
		return data;
	}
}