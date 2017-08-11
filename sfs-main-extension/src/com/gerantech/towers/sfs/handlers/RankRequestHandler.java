package com.gerantech.towers.sfs.handlers;

import java.util.*;

import com.gerantech.towers.sfs.utils.NPCTools;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
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
	private ArrayList allUsers;

	public RankRequestHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		IMap<Integer, RankData> users = NPCTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), game);

		users.putIfAbsent(game.player.id, new RankData(game.player.id, game.player.nickName, game.player.get_point(), game.player.get_xp()));
	//	int playerId = params.getInt("pId");
		RankData playerRD = users.get(game.player.id);
		int arenaIndex = params.getInt("arena"); //game.player.get_arena( playerRD.point ) ;//params.getInt("arena");

		SFSArray players = new SFSArray();
		if( users.size() == 0 )
		{
			params.putSFSArray("list", players);
			send("rank", params, sender);
			return;
		}

		/*for(RankData u:users.values())
			trace("user:", u.name, u.id, u.point);*/

        // a predicate to filter out champions in selected arena
		EntryObject eo = new PredicateBuilder().getEntryObject();
		Predicate sqlQuery = eo.get("point").between(game.arenas.get(arenaIndex).min, game.arenas.get(arenaIndex).max);//.and(eo.get("xp").equal(12));
		
        // a comparator which helps to sort in descending order of point field
        Comparator<Map.Entry> descendingComparator = new Comparator<Map.Entry>() {
            public int compare(Map.Entry e1, Map.Entry e2) {
            	RankData s1 = (RankData) e1.getValue();
            	RankData s2 = (RankData) e2.getValue();
                return s2.point - s1.point;
            }
        };

        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, 8);
		Collection<RankData> result = users.values(pagingPredicate);

		for (RankData r : result)
			players.addSFSObject( getRankSFS(r) );

		// find near me
		if ( game.player.get_arena( playerRD.point ) == arenaIndex && indexOf(result, playerRD.id) == -1 )
		{
			allUsers = new ArrayList<RankData>();
			allUsers.addAll(result);
			
			players.addSFSObject( new SFSObject() );
			List<RankData> nears = getNearMe(playerRD, users, pagingPredicate);

			for (RankData rd : nears)
			{
				ISFSObject p = getRankSFS(rd);
				p.putInt("s",  allUsers.indexOf(rd));
				players.addSFSObject(p);
			}
			allUsers.clear();
		}
		params.putSFSArray("list", players);
		send("rank", params, sender);
	}

	private ISFSObject getRankSFS(RankData r) {
		SFSObject player = new SFSObject();
		player.putInt("i", r.id);
		player.putText("n", r.name);
		player.putInt("p", r.point);
		//player.putInt("x", r.xp);
		return player;
	}
	private List<RankData> getNearMe(RankData playerRD, IMap<Integer, RankData> users, PagingPredicate pagingPredicate)
	{
		pagingPredicate.nextPage();
		trace(pagingPredicate.getPage(), pagingPredicate.getPageSize());
		Collection<RankData> result = users.values(pagingPredicate);
		allUsers.addAll(result);
		int index = indexOf(result, playerRD.id);
		if ( index == -1 && result.size() == pagingPredicate.getPageSize() )
			return getNearMe(playerRD, users, pagingPredicate);

		int playerIndex = pagingPredicate.getPage() * pagingPredicate.getPageSize() + index;
		List<RankData> ret = new ArrayList<>() ;

		if( playerIndex > pagingPredicate.getPageSize()+1 )
			ret.add( (RankData) allUsers.get(playerIndex-2) );
		if( playerIndex > pagingPredicate.getPageSize() )
			ret.add( (RankData) allUsers.get(playerIndex-1) );

		ret.add((RankData) allUsers.get(playerIndex));

		if( playerIndex < allUsers.size()-1 )
			ret.add( (RankData) allUsers.get(playerIndex+1) );
		if( playerIndex < allUsers.size()-2 )
			ret.add( (RankData) allUsers.get(playerIndex+2) );

		return ret;
	}

	private int indexOf(Collection<RankData> ranks, int id)
	{
		int i = 0;
		for (RankData r : ranks) {
			if (r.id == id)
				return i;
			i ++;
		}
		return -1;
	}
}