package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.*;
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
		// Add user login handler
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, JoinZoneEventHandler.class);

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

		// Register push panels to db
		addRequestHandler(Commands.REGISTER_PUSH, RegisterPushHandler.class);

		// Social handlers
		addRequestHandler(Commands.LOBBY_DATA, LobbyDataHandler.class);
		addRequestHandler(Commands.LOBBY_JOIN, LobbyJoinHandler.class);
		addRequestHandler(Commands.LOBBY_LEAVE, LobbyLeaveHandler.class);
		addRequestHandler(Commands.LOBBY_CREATE, LobbyCreateHandler.class);

		addRequestHandler(Commands.ADD_FRIEND, FriendsAddRequestHandler.class);
		addRequestHandler(Commands.REMOVE_FRIEND, FriendsRemoveRequestHandler.class);

		addRequestHandler(Commands.PROFILE, ProfileRequestHandler.class);
	}
	
//	public HazelcastInstance getHazelCast()
//	{
//		if(_hazelcast != null)
//			return _hazelcast;
//		Config hazelcastCfg = new Config("aaa");
//		
////		Map<String, MapConfig> mcs = cfg.getMapConfigs();
////		MapConfig mc = new MapConfig();
////		mc.setInMemoryFormat(InMemoryFormat.OBJECT);
////		mcs.putIfAbsent("mc", mc);
////		for(String s:mcs.keySet())
////			trace("MapConfig ", s);
//				
//		//NetworkConfig network = config.getNetworkConfig(); 
//		//JoinConfig join = network.getJoin();
//		//join.getMulticastConfig().setEnabled(false);
//
////		MapConfig mapCfg = new MapConfig();
////		mapCfg.setAsyncBackupCount(1);
////		mapCfg.setName("users");
////		mapCfg.setInMemoryFormat(InMemoryFormat.BINARY);
////		hazelcastCfg.addMapConfig(mapCfg);
////		
//		trace("_______________________ NEW HAZELCAST INSTANCE _____________________");
//		_hazelcast = Hazelcast.newHazelcastInstance(hazelcastCfg);
//		return _hazelcast;
//	}
}