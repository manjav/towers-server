package com.gerantech.towers.sfs.quests;

import com.gt.Commands;
import com.gt.utils.QuestsUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class QuestInitializeHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	Game game = (Game) sender.getSession().getProperty("core");
		QuestsUtils.getInstance().insertNewQuests(game.player);
		params.putSFSArray("quests", QuestsUtils.toSFS(game.player.quests));
		send(Commands.QUEST_INIT, params, sender);
    }
}