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
		
		Boolean succeed = exchanger.exchange(item, now, hardsConfimed);
		params.putBool("succeed", succeed);
		params.putInt("now", now);
		if( !succeed )
		{
			send("exchange", params, sender);
			return ;
		}

		// logs .....
		int[] reqs = item.requirements.keys();
		for(int i = 0; i<reqs.length; i++)
			trace("requirements", reqs[i], item.requirements.get(reqs[i]));
		int[] outs = item.outcomes.keys();
		for(int o = 0; o<outs.length; o++)
			trace("outcomes", outs[o], item.outcomes.get(outs[o]));
		trace(type, item.expiredAt, now, hardsConfimed, succeed, item.numExchanges);

		if(ExchangeType.getCategory(type) == ExchangeType.S_30_CHEST)
		{
    		SFSArray sfsRewards = new SFSArray();
    		for (int i : item.outcomes.keys())
    		{
	    		SFSObject so = new SFSObject();
	    		so.putInt("t", i);
	    		so.putInt("c", item.outcomes.get(i));
	    		sfsRewards.addSFSObject( so );
    		}
    		params.putSFSArray("rewards", sfsRewards);
		}
		
		// update database
		//add reqs to resources
        IntIntMap resources = item.outcomes;
        for(int r:item.requirements.keys())
        	resources.set(r, 0);
		try {
			UserManager.updateResources(getParentExtension(), player, resources.keys());
			UserManager.updateExchange(getParentExtension(), type, player.id, now+ExchangeType.getCooldown(type), item.numExchanges, 0);
		} catch (Exception e) {
			trace(e.getMessage());
		}
		
		send("exchange", params, sender);
    }
	
}