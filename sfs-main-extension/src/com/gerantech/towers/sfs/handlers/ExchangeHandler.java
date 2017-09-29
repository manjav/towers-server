package com.gerantech.towers.sfs.handlers;

import java.time.Instant;

import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.exchanges.Exchanger;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class ExchangeHandler extends BaseClientRequestHandler 
{
	public ExchangeHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
    	// provide init data
		Game game = ((Game)sender.getSession().getProperty("core"));
		int type = params.getInt("type");
		int now = (int)Instant.now().getEpochSecond();
		int hardsConfimed = 0;
		if(params.containsKey("hards")) {
			hardsConfimed = params.getInt("hards");
		}

		// call exchanger and update database
		boolean succeed = exchange(game, type, now, hardsConfimed, params.containsKey("isAd"));
		params.putBool("succeed", succeed);
		params.putInt("now", now);
		if( !succeed )
		{
			send("exchange", params, sender);
			return;
		}

		// return chest rewars as params
		if(ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST)
		{
			ExchangeItem item = game.exchanger.items.get(type);
			SFSArray sfsRewards = new SFSArray();
			int[] outKeys = item.outcomes.keys();
    		for (int i : outKeys)
    		{
	    		SFSObject so = new SFSObject();
	    		so.putInt("t", i);
	    		so.putInt("c", item.outcomes.get(i));
	    		sfsRewards.addSFSObject( so );
    		}
    		params.putSFSArray("rewards", sfsRewards);
		}

		send("exchange", params, sender);
    }

	public boolean exchange(Game game, int type, int now, int hardsConfimed, boolean isAd)
	{
		ExchangeItem item = game.exchanger.items.get(type);

		if(ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST)
		{
			if( isAd )
				item.outcomes = game.exchanger.getAdChestOutcomes(type);
			else
				item.outcomes = game.exchanger.getChestOutcomes(type);
		}

		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		game.player.resources.changeCallback = mapChangeCallback;
		Boolean succeed = game.exchanger.exchange(item, now, hardsConfimed);
		game.player.resources.changeCallback = null;
		if( !succeed )
			return false;

		// logs .....
		/*int[] inserts = mapChangeCallback.inserts.keys();
		for(int i = 0; i<inserts.length; i++)
			trace("inserts", inserts[i], mapChangeCallback.inserts.get(inserts[i]));
		int[] updates = mapChangeCallback.updates.keys();
		for(int o = 0; o<updates.length; o++)
			trace("updates", updates[o], mapChangeCallback.inserts.get(updates[o]));*/
		trace("type:", type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes.keys().length, " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

		int outcome = ExchangeType.getCategory(type) == ExchangeType.S_20_SPECIALS ? item.outcomes.keys()[0] : 0;

		// Run db queries
		try
		{
			trace(UserManager.updateResources(getParentExtension(), game.player, mapChangeCallback.updates));
			trace(UserManager.insertResources(getParentExtension(), game.player, mapChangeCallback.inserts));
			if( ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST ||  ExchangeType.getCategory(type) == ExchangeType.S_20_SPECIALS )
				trace(UserManager.updateExchange(getParentExtension(), type, game.player.id, item.expiredAt, item.numExchanges, outcome));
		}
		catch (Exception e)
		{
			trace(e.getMessage());
			return false;
		}
		return true;
	}
}