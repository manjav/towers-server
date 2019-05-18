package com.gerantech.towers.sfs;

import com.gerantech.towers.sfs.administration.JoinSpectatorHandler;
import com.gerantech.towers.sfs.administration.PlayersGetHandler;
import com.gerantech.towers.sfs.administration.RestoreHandler;
import com.gerantech.towers.sfs.administration.SearchInChatsHandler;
import com.gerantech.towers.sfs.administration.ban.*;
import com.gerantech.towers.sfs.administration.issues.IssueGetHandler;
import com.gerantech.towers.sfs.administration.issues.IssueReportHandler;
import com.gerantech.towers.sfs.administration.issues.IssueTrackHandler;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.battle.handlers.*;
import com.gerantech.towers.sfs.challenges.handlers.ChallengeCollectRewardHandler;
import com.gerantech.towers.sfs.challenges.handlers.ChallengeGetAllHandler;
import com.gerantech.towers.sfs.challenges.handlers.ChallengeJoinHandler;
import com.gerantech.towers.sfs.challenges.handlers.ChallengeUpdateHandler;
import com.gerantech.towers.sfs.handlers.*;
import com.gerantech.towers.sfs.inbox.*;
import com.gerantech.towers.sfs.quests.QuestInitializeHandler;
import com.gerantech.towers.sfs.quests.QuestRewardCollectHandler;
import com.gerantech.towers.sfs.socials.handlers.*;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.Commands;
import com.gt.utils.BanUtils;
import com.gt.utils.DBUtils;
import com.gt.utils.LobbyUtils;
import com.gt.utils.UtilBase;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.time.Instant;

/**
 * @author ManJav
 */
public class TowerExtension extends SFSExtension
{
	public void init()
    {
    	UtilBase.setExtension(this);
		UtilBase.setBattleClass(BattleRoom.class);

		// Add server event handlers
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, JoinZoneEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleJointHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleUsersExitHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleUsersExitHandler.class);

		// Add startBattle request handler
		addRequestHandler(Commands.BATTLE_START, BattleRequestStartHandler.class);
		addRequestHandler(Commands.BATTLE_CANCEL, BattleRequestCancelHandler.class);

		addRequestHandler(Commands.BATTLE_LEAVE, BattleLeaveRequestHandler.class);
		addRequestHandler(Commands.BATTLE_SUMMON_UNIT, BattleSummonRequestHandler.class);
		addRequestHandler(Commands.BATTLE_SEND_STICKER, BattleStickerRequestHandler.class);

		// Add billing upgrade handler
		addRequestHandler(Commands.CARD_UPGRADE, CardUpgradeHandler.class);
		addRequestHandler(Commands.CARD_NEW, CardNewHandler.class);

        // Add rank handler
		addRequestHandler(Commands.RANK, RankRequestHandler.class);
		addRequestHandler(Commands.SELECT_NAME, SelectNameRequestHandler.class);
		addRequestHandler(Commands.EXCHANGE, ExchangeHandler.class);
		addRequestHandler(Commands.OAUTH, OauthHandler.class);
		addRequestHandler(Commands.VERIFY_PURCHASE, PurchaseVerificationHandler.class);
		addRequestHandler(Commands.CHANGE_DECK, ChangeDeckHandler.class);
		addRequestHandler(Commands.PREFS, UserPrefsRequestHandler.class);
		addRequestHandler(Commands.REGISTER_PUSH, RegisterPushHandler.class);

		// Social handlers
		addRequestHandler(Commands.LOBBY_DATA, LobbyDataHandler.class);
		addRequestHandler(Commands.LOBBY_JOIN, LobbyJoinHandler.class);
		addRequestHandler(Commands.LOBBY_LEAVE, LobbyLeaveHandler.class);
		addRequestHandler(Commands.LOBBY_CREATE, LobbyCreateHandler.class);
		addRequestHandler(Commands.LOBBY_PUBLIC, LobbyPublicRequestHandler.class);
		addRequestHandler(Commands.LOBBY_REMOVE, LobbyRemoveHandler.class);

		addRequestHandler(Commands.BUDDY_ADD, BuddyAddRequestHandler.class);
		addRequestHandler(Commands.BUDDY_REMOVE, BuddyRemoveRequestHandler.class);
		addRequestHandler(Commands.BUDDY_BATTLE, BuddyBattleRequestHandler.class);

		addRequestHandler(Commands.PROFILE, ProfileRequestHandler.class);

		addRequestHandler(Commands.INBOX_GET_THREADS, InboxGetThreadsHandler.class);
		addRequestHandler(Commands.INBOX_GET_RELATIONS, InboxGetRelationsHandler.class);
		addRequestHandler(Commands.INBOX_OPEN, InboxOpenHandler.class);
		addRequestHandler(Commands.INBOX_CONFIRM, InboxConfirmHandler.class);
		addRequestHandler(Commands.INBOX_BROADCAST, InboxBroadcastMessageHandler.class);

		// administration handlers
		addRequestHandler(Commands.ISSUE_REPORT, IssueReportHandler.class);
		addRequestHandler(Commands.ISSUE_GET, IssueGetHandler.class);
		addRequestHandler(Commands.ISSUE_TRACK, IssueTrackHandler.class);
		addRequestHandler(Commands.RESTORE, RestoreHandler.class);
		addRequestHandler(Commands.BAN, BanHandler.class);
		addRequestHandler(Commands.BANNED_DATA_GET, GetBannedDataHandler.class);
		addRequestHandler(Commands.OFFENDER_DATA_GET, GetOffenderDataHandler.class);
		addRequestHandler(Commands.INFRACTIONS_GET, InfractionsGetHandler.class);
		addRequestHandler(Commands.INFRACTIONS_DELETE, InfractionsDeleteHandler.class);
		addRequestHandler(Commands.PLAYERS_GET, PlayersGetHandler.class);
		addRequestHandler(Commands.SEARCH_IN_CHATS, SearchInChatsHandler.class);
		addRequestHandler("resetalllobbies", ResetLobbiesHandler.class);
		addRequestHandler("spectateBattles", JoinSpectatorHandler.class);

		addRequestHandler(Commands.CHALLENGE_JOIN, ChallengeJoinHandler.class);
		addRequestHandler(Commands.CHALLENGE_UPDATE, ChallengeUpdateHandler.class);
		addRequestHandler(Commands.CHALLENGE_GET_ALL, ChallengeGetAllHandler.class);
		addRequestHandler(Commands.CHALLENGE_COLLECT, ChallengeCollectRewardHandler.class);

		addRequestHandler(Commands.QUEST_INIT, QuestInitializeHandler.class);
		addRequestHandler(Commands.QUEST_REWARD_COLLECT, QuestRewardCollectHandler.class);
	}

	@Override
	public Object handleInternalMessage(String cmdName, Object params)
	{
		trace(cmdName, params);
		if( cmdName.equals("setumtime") )
			return (LoginEventHandler.UNTIL_MAINTENANCE = (int)Instant.now().getEpochSecond() + Integer.parseInt((String) params)) + ";;";
		else if( cmdName.equals("ban") )
			return BanUtils.getInstance().checkOffends((String) params);
		else if( cmdName.equals("servercheck") )
			return "OK HAHAHA.";
		else if( cmdName.equals("resetkeylimit") )
			return DBUtils.getInstance().resetDailyBattles();
		else if( cmdName.equals("resetlobbiesactiveness") )
			return LobbyUtils.getInstance().resetActivities();
		else if( cmdName.equals("getplayernamebyic") )
			return PasswordGenerator.getIdAndNameByInvitationCode((String) params);
		//else if( cmdName.equals("custom") )
		//	return LobbyUtils.getInstance().moveActiveness();
		
		return null;
	}
}