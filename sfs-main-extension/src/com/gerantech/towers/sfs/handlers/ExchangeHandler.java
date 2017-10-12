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
import com.sun.org.apache.xpath.internal.operations.Bool;

/**
 * @author ManJav
 *
 */
public class ExchangeHandler extends BaseClientRequestHandler 
{
	public ExchangeHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
    	trace("exchange:", params.getDump());
    	// provide init data
		Game game = ((Game)sender.getSession().getProperty("core"));
		int type = params.getInt("type");
		int now = (int)Instant.now().getEpochSecond();

		int hardsConfimed = 0;
		if(params.containsKey("hards"))
			hardsConfimed = params.getInt("hards");

		// call exchanger and update database
		boolean succeed = exchange(game, type, now, hardsConfimed);
		params.putBool("succeed", succeed);
		params.putInt("now", now);
		if( !succeed )
		{
			send("exchange", params, sender);
			return;
		}

		// return chest rewards as params
		ExchangeItem item = game.exchanger.items.get(type);
		if( item.outcomes != null )
		{
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

	public boolean exchange(Game game, int type, int now, int hardsConfimed)
	{
		ExchangeItem item = game.exchanger.items.get(type);

		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

		trace(type, now);
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
		trace("type:", type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes==null?"":item.outcomes.keys().length, " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

		// Run db queries
		try
		{
			trace(UserManager.updateResources(getParentExtension(), game.player, mapChangeCallback.updates));
			trace(UserManager.insertResources(getParentExtension(), game.player, mapChangeCallback.inserts));
			if( item.category == ExchangeType.S_30_CHEST || item.category == ExchangeType.S_20_SPECIALS || item.category == ExchangeType.CHEST_CATE_110_BATTLES || item.category == ExchangeType.CHEST_CATE_120_OFFERS )
				trace(UserManager.updateExchange(getParentExtension(), type, game.player.id, item.expiredAt, item.numExchanges, item.outcome));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}