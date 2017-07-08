package com.gerantech.towers.sfs.handlers;

import java.io.Serializable;
import java.util.UUID;

import com.gerantech.towers.sfs.TowerExtension;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class RankRequestHandler extends BaseClientRequestHandler 
{
	public RankRequestHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
	{

		//		try {
		//			SFSArray ranklist = UserManager.getRankTable(getParentExtension(), params.getInt("arena"), 50);
		//		} catch (Exception e) {
		//			trace(e.getMessage());
		//		}

		//trace(getParentExtension());
		Config config = new Config("aaa");
				
		NetworkConfig network = config.getNetworkConfig(); 
		JoinConfig join = network.getJoin();
		join.getMulticastConfig().setEnabled(false);
		HazelcastInstance instance = Hazelcast.getOrCreateHazelcastInstance(config);

		//HazelcastInstance instance = ((TowerExtension)getParentExtension()).getHazelCast();//Hazelcast.getOrCreateHazelcastInstance(config);
		IMap<Integer, RankData> users = instance.getMap("users");
		
		trace("users.size():", users.size());
		if(users.size() == 0)
		{
			for ( int i=0; i<200; i++)
				users.put(i , new RankData("ali", RandomPicker.getInt(12, 56), 12));
			//return;
		}
		
		//for ( Integer k:users.keySet())
		//	trace("users:", k, users.get(k).age);
	//	trace("user:", instance.getMap("users").get(0));
		trace("user:", users.get(0));

//        // a predicate to filter out active young ones
//		EntryObject eo = new PredicateBuilder().getEntryObject();
//		Predicate sqlQuery = eo.get("point").between(18, 20).and(eo.get("xp").equal(12));
//		
//        // a comparator which helps to sort in descending order of point field
//        Comparator<Map.Entry> descendingComparator = new Comparator<Map.Entry>() {
//            public int compare(Map.Entry e1, Map.Entry e2) {
//            	RankData s1 = (RankData) e1.getValue();
//            	RankData s2 = (RankData) e2.getValue();
//                return s2.getPoint() - s1.getPoint();
//            }
//        };
//
//        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, 30);
//		Collection<RankData> result1 = users.values(pagingPredicate);
//		trace("");
//		for (RankData r : result1)
//			trace(r.getPoint());
		//instance.shutdown();
		//send("rank", params, sender);
	}
	
	public static class RanksData implements Serializable 
	{
		//private static final long serialVersionUID = 1L;
	    private static final long serialVersionUID = UUID.randomUUID().version();
		private final String name;
		private final int point;
		private final int xp;
		
		public RanksData(String name, int point, int xp)
		{
		    super();
			this.name = name;
			this.point = point;
			this.xp = xp;
		}
		
		public int getPoint()
		{
			return point;
		}
		public int getXp()
		{
			return xp;
		}
		public String getName()
		{
			return name;
		}
	}
}