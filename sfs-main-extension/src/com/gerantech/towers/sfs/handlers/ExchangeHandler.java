package com.gerantech.towers.sfs.handlers;

import java.time.Instant;

import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gerantech.towers.sfs.utils.ExchangeManager;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.exchanges.ExchangeItem;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import sun.plugin2.message.Message;

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
		ExchangeManager manager = ExchangeManager.getInstance();
		int response = manager.process(game, type, now,  params.containsKey("hards") ?  params.getInt("hards") : 0);
		params.putInt("response", response);
		params.putInt("now", now);
		if( response != MessageTypes.RESPONSE_SUCCEED )
		{
			send("exchange", params, sender);
			return;
		}

		// return chest rewards as params
		ExchangeItem item = game.exchanger.items.get(type);

		SFSArray sfsRewards = new SFSArray();
		int[] outKeys = manager.mapChangeCallback.all.keys();
		for (int i : outKeys)
		{
			if( manager.mapChangeCallback.all.get(i) <= 0 )
				continue;

			SFSObject so = new SFSObject();
			so.putInt("t", i);
			so.putInt("c",  manager.mapChangeCallback.all.get(i));
			sfsRewards.addSFSObject( so );
		}
		if( sfsRewards.size() > 0 )
			params.putSFSArray("rewards", sfsRewards);
		manager.mapChangeCallback = null;

		// return new outcome
		if( item.category == ExchangeType.C110_BATTLES )
			params.putInt("nextOutcome", item.outcome);

		send("exchange", params, sender);
	}
}