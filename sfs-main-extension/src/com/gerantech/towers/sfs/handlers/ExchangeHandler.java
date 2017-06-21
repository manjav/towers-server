package com.gerantech.towers.sfs.handlers;

import java.time.Instant;

import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.exchanges.Exchanger;
import com.gt.towers.utils.maps.Bundle;
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
    	int bundleType = (int)params.getInt("type");
		Exchanger exchanger = ((Game)sender.getSession().getProperty("core")).get_exchanger();
		Player player = ((Game)sender.getSession().getProperty("core")).get_player();
		ExchangeItem exchangeItem = exchanger.bundlesMap.get(bundleType);
		Long now = Instant.now().toEpochMilli();
		
		Boolean succeed = ExchangeType.getCategory(bundleType) != ExchangeType.S_30_CHEST || exchangeItem.expiredAt < now;
		trace(bundleType, exchangeItem.expiredAt<now);
		
		if(!succeed)
		{
			params.putBool("succeed", succeed);
			send("exchange", params, sender);
			return ;
		}

		succeed = exchanger.exchange(bundleType);
		params.putBool("succeed", succeed);
		if(!succeed)
		{
			send("exchange", params, sender);
			return ;
		}
		
		if(ExchangeType.getCategory(bundleType) == ExchangeType.S_30_CHEST)
		{
			Bundle rewards = exchanger.instantiateChest(bundleType);
			player.get_resources().increaseMap(rewards);
			try {
				
	    		SFSArray sfsRewards = new SFSArray();
	    		for (int i : rewards.keys())
	    		{
		    		SFSObject so = new SFSObject();
		    		so.putInt("type", i);
		    		so.putInt("count", rewards.get(i));
		    		sfsRewards.addSFSObject( so );
	    		}
				params.putSFSArray("rewards", sfsRewards);
				params.putLong("expiredAt", now+ExchangeType.getCooldown(bundleType));
				
				UserManager.updateResources(getParentExtension(), player, rewards.keys());
				UserManager.updateExchange(getParentExtension(), bundleType, player.get_id(), now+ExchangeType.getCooldown(bundleType), 0, 0);
			} catch (Exception e) {
				trace(e.getMessage());
			}
		}

		int[] reqs = exchangeItem.requirements.keys();
		for(int i = 0; i<reqs.length; i++)
			trace("requirements", reqs[i], exchangeItem.requirements.get(reqs[i]));
		
		int[] outs = exchangeItem.outcomes.keys();
		for(int o = 0; o<outs.length; o++)
			trace("outcomes", outs[o], exchangeItem.outcomes.get(outs[o]));
			
		send("exchange", params, sender);
    }
	
}