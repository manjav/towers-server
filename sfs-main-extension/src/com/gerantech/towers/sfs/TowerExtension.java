package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.BattleAutoJoinHandler;
import com.gerantech.towers.sfs.handlers.BuildingUpgradeHandler;
import com.gerantech.towers.sfs.handlers.CafeBazaarVerificationHandler;
import com.gerantech.towers.sfs.handlers.ExchangeHandler;
import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.gerantech.towers.sfs.handlers.OauthHandler;
import com.gerantech.towers.sfs.handlers.RankRequestHandler;
import com.gerantech.towers.sfs.handlers.SelectNameRequestHandler;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
/**
 * @author ManJav
 */
public class TowerExtension extends SFSExtension
{
	private HazelcastInstance _hazelcast;

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
	
        // Add exchange handler
		addRequestHandler("exchange", ExchangeHandler.class);
		
        // Add socials open authentication handler
		addRequestHandler("oauth", OauthHandler.class);
		
        // Add in app billing verification handler
		addRequestHandler("verify", CafeBazaarVerificationHandler.class);
	}
	
	public HazelcastInstance getHazelCast()
	{
		if(_hazelcast != null)
			return _hazelcast;
		Config hazelcastCfg = new Config("aaa");
		
//		Map<String, MapConfig> mcs = cfg.getMapConfigs();
//		MapConfig mc = new MapConfig();
//		mc.setInMemoryFormat(InMemoryFormat.OBJECT);
//		mcs.putIfAbsent("mc", mc);
//		for(String s:mcs.keySet())
//			trace("MapConfig ", s);
				
		//NetworkConfig network = config.getNetworkConfig(); 
		//JoinConfig join = network.getJoin();
		//join.getMulticastConfig().setEnabled(false);

//		MapConfig mapCfg = new MapConfig();
//		mapCfg.setAsyncBackupCount(1);
//		mapCfg.setName("users");
//		mapCfg.setInMemoryFormat(InMemoryFormat.BINARY);
//		hazelcastCfg.addMapConfig(mapCfg);
//		
		trace("_______________________ NEW HAZELCAST INSTANCE _____________________");
		_hazelcast = Hazelcast.newHazelcastInstance(hazelcastCfg);
		return _hazelcast;
	}
}