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
	public static int FIRST_PAGE_SIZE = 10;
	public static int PAGE_SIZE = 100;

	private List<RankData> allUsers;

	public RankRequestHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));
		IMap<Integer, RankData> users = NPCTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), game, getParentExtension());

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

        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, PAGE_SIZE);
		Collection<RankData> result = users.values(pagingPredicate);

		int i = 0;
		for (RankData r : result)
		{
			if( i >= FIRST_PAGE_SIZE )
				break;
			players.addSFSObject( getRankSFS(r) );
			i ++;
		}

		if( game.player.get_arena( playerRD.point ) != arenaIndex )
		{
			params.putSFSArray("list", players);
			send("rank", params, sender);
			return;
		}

		// find near me
		allUsers = new ArrayList();
		allUsers.addAll(result);
		int index = indexOf(result, playerRD.id, playerRD.point);
		if( index > FIRST_PAGE_SIZE-1 )
		{
			players.addSFSObject( new SFSObject() );
			addNearsToPlayers(players, index, 0);
		}
		else if ( index == -1 )
		{
			players.addSFSObject( new SFSObject() );
			index = findMe(playerRD, users, pagingPredicate);
			if( index > -1 )
				addNearsToPlayers(players, index, pagingPredicate.getPage());
		}
		allUsers.clear();
		params.putSFSArray("list", players);
		send("rank", params, sender);
	}

	private int findMe(RankData playerRD, IMap<Integer, RankData> users, PagingPredicate pagingPredicate)
	{
		pagingPredicate.nextPage();
		//trace(pagingPredicate.getPage(), PAGE_SIZE);
		Collection<RankData> result = users.values(pagingPredicate);
		if(result.size() < PAGE_SIZE )
			return -1;

		allUsers.addAll(result);
		int index = indexOf(result, playerRD.id, playerRD.point);
		if ( index == -1 )
			return findMe(playerRD, users, pagingPredicate);

		return index;
	}

	private void addNearsToPlayers(SFSArray players, int index, int pageIndex)
	{
		int playerIndex = pageIndex * PAGE_SIZE + index;

		if( playerIndex > PAGE_SIZE+1 || (pageIndex==0 && playerIndex > FIRST_PAGE_SIZE+1))
			addToPlayer(playerIndex-2, players );

		if( playerIndex > PAGE_SIZE || (pageIndex==0 && playerIndex > FIRST_PAGE_SIZE))
			addToPlayer( playerIndex-1, players );

		addToPlayer( playerIndex, players );

		if( playerIndex < allUsers.size()-1 )
			addToPlayer( playerIndex+1, players );
		if( playerIndex < allUsers.size()-2 )
			addToPlayer( playerIndex+2, players );
	}

	private ISFSObject getRankSFS(RankData r) {
		SFSObject player = new SFSObject();
		player.putInt("i", r.id);
		player.putText("n", r.name);
		player.putInt("p", r.point);
		//player.putInt("x", r.xp);
		return player;
	}
	private void addToPlayer(int index, SFSArray players)
	{
		ISFSObject p = getRankSFS(allUsers.get(index));
		p.putInt("s",  index);
		players.addSFSObject(p);
	}

	private int indexOf(Collection<RankData> ranks, int id, int point)
	{
		Iterator<RankData> iter = ranks.iterator();
		RankData lastElement = iter.next();
		while(iter.hasNext()) {
			lastElement = iter.next();
		}
		if( lastElement.point > point )
			return -1;

		int i = 0;
		for (RankData r : ranks) {
			if (r.id == id)
				return i;
			i ++;
		}
		return -1;
	}
}