package com.gerantech.towers.sfs.handlers;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.utils.maps.IntArenaMap;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class RankRequestHandler extends BaseClientRequestHandler 
{
	public RankRequestHandler() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void handleClientRequest(User sender, ISFSObject params)
	{
		IMap<Integer, RankData> users =  Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");

		IntArenaMap arenas = ((Game)sender.getSession().getProperty("core")).arenas;
		int arenaIndex = params.getInt("arena");
		SFSArray players = new SFSArray();

		trace("users.size():", users.size());
		if(users.size() == 0)
		{
			params.putSFSArray("list", players);
			send("rank", params, sender);
			return;
		}

        // a predicate to filter out champions in selected arena
		EntryObject eo = new PredicateBuilder().getEntryObject();
		Predicate sqlQuery = eo.get("point").between(arenas.get(arenaIndex).min, arenas.get(arenaIndex).max);//.and(eo.get("xp").equal(12));
		
        // a comparator which helps to sort in descending order of point field
        Comparator<Map.Entry> descendingComparator = new Comparator<Map.Entry>() {
            public int compare(Map.Entry e1, Map.Entry e2) {
            	RankData s1 = (RankData) e1.getValue();
            	RankData s2 = (RankData) e2.getValue();
                return s2.point - s1.point;
            }
        };

        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, 30);
		Collection<RankData> result1 = users.values(pagingPredicate);

		SFSObject player = null;
		for (RankData r : result1)
		{
			player = new SFSObject();
			player.putInt("i", r.id);
			player.putText("n", r.name);
			player.putInt("p", r.point);
			player.putInt("x", r.xp);
			players.addSFSObject(player);
		}
		params.putSFSArray("list", players);
		send("rank", params, sender);
	}
	
}