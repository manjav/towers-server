package com.gerantech.towers.sfs.handlers;

import java.time.Instant;

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
		int hardsConfimed = 0;
    	int type = params.getInt("type");
    	if(params.containsKey("hards"))
    		hardsConfimed = params.getInt("hards");

		Exchanger exchanger = ((Game)sender.getSession().getProperty("core")).exchanger;
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		ExchangeItem item = exchanger.items.get(type);
		int now = (int)Instant.now().getEpochSecond();

		if(ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST)
			item.outcomes = exchanger.getChestOutcomes(type);

		// define new outcomes for inert query
		IntIntMap updateMap = new IntIntMap();
		IntIntMap insertMap = new IntIntMap();
		int[] outk = item.outcomes.keys();
		int r = 0;
		while ( r < outk.length )
		{
			if ( player.resources.exists(outk[r]) )
				updateMap.set(outk[r], 0);
			else
				insertMap.set(outk[r], item.outcomes.get(outk[r]));
			//trace(r, outk[r], 0 );
			r ++;
		}

		Boolean succeed = exchanger.exchange(item, now, hardsConfimed);
		params.putBool("succeed", succeed);
		params.putInt("now", now);
		if( !succeed )
		{
			send("exchange", params, sender);
			return ;
		}

		// logs .....
		/*int[] reqs = item.requirements.keys();
		for(int i = 0; i<reqs.length; i++)
			trace("requirements", reqs[i], item.requirements.get(reqs[i]));
		int[] outs = item.outcomes.keys();
		for(int o = 0; o<outs.length; o++)
			trace("outcomes", outs[o], item.outcomes.get(outs[o]));*/
		trace("type:", type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes.keys().length, " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

		if(ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST)
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

		int outcome = ExchangeType.getCategory(type) == ExchangeType.S_20_SPECIALS ? item.outcomes.keys()[0] : 0;

		// add reqs keys to query
		outk = item.requirements.keys();
		r = 0;
		while ( r < outk.length ) {
			updateMap.set(r, 0);
			r ++;
		}

		// update database
		try {
			trace(UserManager.updateResources(getParentExtension(), player, updateMap));
			trace(UserManager.insertResources(getParentExtension(), player, insertMap));
			trace(UserManager.updateExchange(getParentExtension(), type, player.id, item.expiredAt, item.numExchanges, outcome));
		} catch (Exception e) {
			trace(e.getMessage());
		}
		
		send("exchange", params, sender);
    }
	
}