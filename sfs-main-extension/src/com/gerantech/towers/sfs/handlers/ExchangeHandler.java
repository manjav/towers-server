package com.gerantech.towers.sfs.handlers;

import java.time.Instant;

import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.ExchangeDonateItem;
import com.gt.towers.exchanges.ExchangeItem;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

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

		// call exchanger and update database
		boolean succeed = exchange(game, type, now,  params.containsKey("hards") ?  params.getInt("hards") : 0);
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
		if( item == null )
		{
			trace(ExtensionLogLevel.ERROR, "Exchange item not found in exchanger.");
			return false;
		}
		return exchange(game, item, now, hardsConfimed);
	}

	public boolean exchange(Game game, ExchangeItem item, int now, int hardsConfimed)
	{

		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		game.player.resources.changeCallback = mapChangeCallback;
		Boolean succeed = false;
		try {
			succeed = game.exchanger.exchange(item, now, hardsConfimed);
		} catch (Exception e) { e.printStackTrace(); }
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
		trace("Exchange => type:", item.type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes==null?"":item.outcomes.keys().length, " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

		// Run db queries
		DBUtils dbUtils = DBUtils.getInstance();
		try
		{
			dbUtils.updateResources(game.player, mapChangeCallback.updates);
			dbUtils.insertResources(game.player, mapChangeCallback.inserts);
			if( item.isChest() )
				dbUtils.updateExchange(item.type, game.player.id, item.expiredAt, item.numExchanges, item.outcome);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean exchange(Game game, ExchangeDonateItem item, int now, int hardsConfimed)
	{


		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		game.player.resources.changeCallback = mapChangeCallback;
		Boolean succeed = false;
		try {
			succeed = game.exchanger.exchangeDonate(item, now, hardsConfimed);
		} catch (Exception e) { e.printStackTrace(); }
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
		trace("Exchange => type:", item.type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes==null?"":item.outcomes.keys().length, " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

		// Run db queries
		DBUtils dbUtils = DBUtils.getInstance();
		try
		{
			dbUtils.updateResources(game.player, mapChangeCallback.updates);
			dbUtils.insertResources(game.player, mapChangeCallback.inserts);
			if( item.isChest() )
				dbUtils.updateExchange(item.type, game.player.id, item.expiredAt, item.numExchanges, item.outcome);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}