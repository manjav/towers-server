package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.utils.ExchangeUtils;
import com.gt.callbacks.MapChangeCallback;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
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

		// call exchanger and update database
		ExchangeUtils manager = ExchangeUtils.getInstance();
		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		int response = manager.process(game, type, now,  params.containsKey("hards") ?  params.getInt("hards") : 0, mapChangeCallback);
		params.putInt("response", response);
		params.putInt("now", now);
		if( response != MessageTypes.RESPONSE_SUCCEED )
		{
			send(Commands.EXCHANGE, params, sender);
			return;
		}

		// return book rewards as params
		ISFSArray sfsRewards = manager.getRewards(mapChangeCallback);
		if( sfsRewards.size() > 0 )
			params.putSFSArray("rewards", sfsRewards);

		send(Commands.EXCHANGE, params, sender);
	}
}