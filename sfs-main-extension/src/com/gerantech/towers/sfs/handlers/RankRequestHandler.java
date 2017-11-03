package com.gerantech.towers.sfs.handlers;

import java.util.*;

import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
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

		IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
		RankData rd = new RankData(game.player.id, game.player.nickName,  game.player.get_point(), game.player.resources.get(ResourceType.BATTLES_COUNT_WEEKLY));
		if( users.containsKey(game.player.id))
			users.replace(game.player.id, rd);
		else
			users.put(game.player.id, rd);
		
		RankData playerRD = users.get(game.player.id);
		int arenaIndex = params.getInt("arena"); //game.player.get_arena( playerRD.point ) ;//params.getInt("arena");

		SFSArray players = new SFSArray();
		if( users.size() == 0 )
		{
			params.putSFSArray("list", players);
			send("rank", params, sender);
			return;
		}

        // a predicate to filter out champions in selected arena
		EntryObject eo = new PredicateBuilder().getEntryObject();

		// get arena max and min point
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
			players.addSFSObject( toSFS(r) );
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
			
			if( arenaIndex < 3 )
			{
				addFakeRankData(players, users, playerRD, game, arenaIndex);
			}
			else
			{
				index = findMe(playerRD, users, pagingPredicate);
				if( index > -1 )
					addNearsToPlayers(players, index, pagingPredicate.getPage());
			}
		}
		allUsers.clear();
		params.putSFSArray("list", players);
		send("rank", params, sender);
	}

	private void addFakeRankData(SFSArray players, IMap<Integer, RankData> users, RankData playerRD, Game game, int arenaIndex)
	{
		List<RankData> fakedRanks = RankingUtils.getInstance().getFakedNearMeRanking(users, playerRD.id, 0, 0);
		float rankingRatio = (1 - ( (float)(playerRD.point -  game.arenas.get(arenaIndex).min) / (game.arenas.get(arenaIndex).max - game.arenas.get(arenaIndex).min) ) );
		int playerFakeRanking = (int)(rankingRatio * ( (10 - arenaIndex) * 1000 ) - playerRD.xp);
		fakedRanks.add(fakedRanks.size()/2, playerRD);

		Collections.sort(fakedRanks, new Comparator<RankData>() {
			@Override
			public int compare(RankData rhs, RankData lhs) {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				return rhs.point - lhs.point;
			}
		});

		int c = -2;
		for (RankData r : fakedRanks)
		{
			ISFSObject p = toSFS(r);
			p.putInt("s", playerFakeRanking + c);
			players.addSFSObject(p);
			c ++;
		}
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

	private ISFSObject toSFS(RankData r) {
		SFSObject player = new SFSObject();
		player.putInt("i", r.id);
		player.putText("n", r.name);
		player.putInt("p", r.point);
		//player.putInt("x", r.xp);
		return player;
	}
	private void addToPlayer(int index, SFSArray players)
	{
		ISFSObject p = toSFS(allUsers.get(index));
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