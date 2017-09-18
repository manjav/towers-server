package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.*;
import com.gerantech.towers.sfs.socials.handlers.BuddyAddRequestHandler;
import com.gerantech.towers.sfs.socials.handlers.BuddyRemoveRequestHandler;
import com.gerantech.towers.sfs.socials.handlers.*;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
/**
 * @author ManJav
 */
public class TowerExtension extends SFSExtension
{
	public ExchangeHandler exchangeHandler;

	public void init()
    {
		// Add user server handlers
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, JoinZoneEventHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleUsersExitHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleUsersExitHandler.class);
		addEventHandler(SFSEventType.ROOM_REMOVED, BattlesRemovedHandler.class);

        // Add startBattle request handler
		addRequestHandler(Commands.START_BATTLE, BattleAutoJoinHandler.class);

        // Add billing upgrade handler
		addRequestHandler(Commands.BUILDING_UPGRADE, BuildingUpgradeHandler.class);
		
        // Add rank handler
		addRequestHandler(Commands.RANK, RankRequestHandler.class);

		// Add select name handler
		addRequestHandler(Commands.SELECT_NAME, SelectNameRequestHandler.class);

		// Add select name handler
		addRequestHandler(Commands.BUG_REPORT, BugReportRequestHandler.class);

		// Add exchange handler
		exchangeHandler = new ExchangeHandler();
		addRequestHandler(Commands.EXCHANGE, exchangeHandler);
		
        // Add socials open authentication handler
		addRequestHandler(Commands.OAUTH, OauthHandler.class);

        // Add in app billing verification handler
		addRequestHandler(Commands.VERIFY_PURCHASE, CafeBazaarVerificationHandler.class);

		addRequestHandler(Commands.RESTORE, RestoreRequestHandler.class);
		addRequestHandler(Commands.PREFS, UserPrefsRequestHandler.class);

		// Register push panels to db
		addRequestHandler(Commands.REGISTER_PUSH, RegisterPushHandler.class);

		// Social handlers
		addRequestHandler(Commands.LOBBY_DATA, LobbyDataHandler.class);
		addRequestHandler(Commands.LOBBY_JOIN, LobbyJoinHandler.class);
		addRequestHandler(Commands.LOBBY_LEAVE, LobbyLeaveHandler.class);
		addRequestHandler(Commands.LOBBY_CREATE, LobbyCreateHandler.class);

		addRequestHandler(Commands.BUDDY_ADD, BuddyAddRequestHandler.class);
		addRequestHandler(Commands.BUDDY_REMOVE, BuddyRemoveRequestHandler.class);
		addRequestHandler(Commands.BUDDY_BATTLE, BuddyBattleRequestHandler.class);

		addRequestHandler(Commands.PROFILE, ProfileRequestHandler.class);

		addRequestHandler("resetalllobbies", ResetLobbiesHandler.class);
	}
}