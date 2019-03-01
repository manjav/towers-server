package com.gerantech.towers.sfs.quests;

import com.gt.Commands;
import com.gt.utils.QuestsUtils;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class QuestRewardCollectHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	Game game = (Game) sender.getSession().getProperty("core");
		int response = QuestsUtils.getInstance().collectReward(game, params.getInt("id"));
		if( response == MessageTypes.RESPONSE_SUCCEED )
			params.putSFSObject("quest", QuestsUtils.toSFS(game.player.quests.__get(game.player.quests.length - 1)));
		params.putInt("response", response);
		send(Commands.QUEST_REWARD_COLLECT, params, sender);
    }
}