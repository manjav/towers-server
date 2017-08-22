package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.*;
import com.gerantech.towers.sfs.handlers.friendship.AddFriendRequestHandler;
import com.gerantech.towers.sfs.handlers.friendship.RemoveFriendRequestHandler;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
/**
 * @author ManJav
 */
public class TowerExtension extends SFSExtension
{
	public ExchangeHandler exchangeHandler;
	//private HazelcastInstance _hazelcast;

	public void init()
    {
		// Add user login handler
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);

        // Add startBattle request handler
		addRequestHandler("startBattle", BattleAutoJoinHandler.class);
		
        // Add billing upgrade handler
		addRequestHandler("buildingUpgrade", BuildingUpgradeHandler.class);
		
        // Add rank handler
		addRequestHandler("rank", RankRequestHandler.class);

		// Add select name handler
		addRequestHandler("selectName", SelectNameRequestHandler.class);

		// Add select name handler
		addRequestHandler("bugReport", BugReportRequestHandler.class);

		// Add exchange handler
		exchangeHandler = new ExchangeHandler();
		addRequestHandler("exchange", exchangeHandler);
		
        // Add socials open authentication handler
		addRequestHandler("oauth", OauthHandler.class);

        // Add in app billing verification handler
		addRequestHandler("verify", CafeBazaarVerificationHandler.class);

		// Register push panels to db
		addRequestHandler("registerPush", RegisterPushHandler.class);

		addRequestHandler("addFriend", AddFriendRequestHandler.class);
		addRequestHandler("removeFriend", RemoveFriendRequestHandler.class);

		addRequestHandler("profile", ProfileRequestHandler.class);
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