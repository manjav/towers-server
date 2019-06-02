package com.gerantech.towers.sfs.handlers;

import com.gt.utils.ChallengeUtils;
import com.gt.utils.LobbyUtils;
import com.gt.utils.RankingUtils;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

/**
 * @author ManJav
 *
 */
public class ServerReadyHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent event)
    {
try {
		getParentExtension().getParentZone().setProperty("startTime", System.currentTimeMillis());

		// load all settings
		//RankingUtils.getInstance().fillStatistics();
		RankingUtils.getInstance().fillActives();
		LobbyUtils.getInstance().loadAll();
		ChallengeUtils.getInstance().loadAll();

		getParentExtension().getParentZone().removeProperty("startTime");
} catch (Exception | Error e) { e.printStackTrace(); }
	}
}