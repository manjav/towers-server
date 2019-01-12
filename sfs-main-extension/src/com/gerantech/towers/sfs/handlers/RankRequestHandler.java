package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.*;

/**
 * @author ManJav
 *
 */
public class RankRequestHandler extends BaseClientRequestHandler 
{
	//public static int FIRST_PAGE_SIZE = 10;
	public static int PAGE_SIZE = 50;

	private List<RankData> allUsers;
	public RankRequestHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));

		IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
		RankData rd = new RankData(game.player.nickName,  game.player.get_point(), game.player.getResource(ResourceType.R14_BATTLES_WEEKLY), game.player.getResource(ResourceType.R18_STARS_WEEKLY));
		if( users.containsKey(game.player.id))
			users.replace(game.player.id, rd);
		else
			users.put(game.player.id, rd);
		
		RankData playerRD = users.get(game.player.id);
		SFSArray players = new SFSArray();
		if( users.size() == 0 )
		{
			params.putSFSArray("list", players);
			send("rank", params, sender);
			return;
		}

        // a comparator which helps to sort in descending order of point field
        Comparator<Map.Entry> descendingComparator = (e1, e2) -> {
			RankData s1 = (RankData) e1.getValue();
			RankData s2 = (RankData) e2.getValue();
			return s2.point - s1.point;
		};

        PagingPredicate pagingPredicate = new PagingPredicate(descendingComparator, PAGE_SIZE);
		Collection<RankData> result = users.values(pagingPredicate);

		Iterator<RankData> iterator = result.iterator();
		int lastHeroPoint = -1;
		while( true )
		{
			RankData r = iterator.next();
			players.addSFSObject(toSFS(game.player.id, r));
			if( !iterator.hasNext() )
			{
				lastHeroPoint = r.point;
				break;
			}
		}

		// find near me
		allUsers = new ArrayList();
		allUsers.addAll(result);
		int index = indexOf(result, game.player.id, playerRD.point);
		/*if( index > PAGE_SIZE - 1 )
		{
			players.addSFSObject( new SFSObject() );
			addNearsToPlayers(players, index, 0);
		}
		else */if( index == -1 )
		{
			players.addSFSObject( new SFSObject() );
			
			/*if( arenaIndex < 3 )
			{*/
				addFakeRankData(players, users, game.player.id, playerRD, lastHeroPoint);//, game, arenaIndex);
		/*	}
			else
			{
				index = findMe(playerRD, users, pagingPredicate);
				if( index > -1 )
					addNearsToPlayers(players, index, pagingPredicate.getPage());
			}*/
		}
		//allUsers.clear();
		params.putSFSArray("list", players);
		send("rank", params, sender);
	}

	private void addFakeRankData(SFSArray players, IMap<Integer, RankData> users, int id,  RankData playerRD, int lastHeroPoint)
	{
		//List<RankData> fakedRanks = RankingUtils.getInstance().getFakedNearMeRanking(users, playerRD.id, 0, 0);
		float rankingRatio = (float) (1 - Math.log( 1 + ( 1.7183 * ( (float)playerRD.point / (float)lastHeroPoint) ) ));
		int playerFakeRanking = Math.round(rankingRatio * users.size() * 0.5f) + PAGE_SIZE;
		//fakedRanks.add(fakedRanks.size()/2, playerRD);

		ISFSObject p = toSFS(id, playerRD);
		p.putInt("s", playerFakeRanking);
		players.addSFSObject(p);

		/*Collections.sort(fakedRanks, new Comparator<RankData>() {
			@Override
			public int compare(RankData rhs, RankData lhs) {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				return rhs.point - lhs.point;
			}
		});

		int c = -2;
		for( RankData r : fakedRanks )
		{
			ISFSObject p = toSFS(r);
			p.putInt("s", playerFakeRanking + c);
			players.addSFSObject(p);
			c ++;
		}*/
	}

	/*private int findMe(RankData playerRD, IMap<Integer, RankData> users, PagingPredicate pagingPredicate)
	{
		pagingPredicate.nextPage();
		//trace(pagingPredicate.getPage(), PAGE_SIZE);
		Collection<RankData> result = users.values(pagingPredicate);
		if( result.size() < PAGE_SIZE )
			return -1;

		allUsers.addAll(result);
		int index = indexOf(result, playerRD.id, playerRD.point);
		if( index == -1 )
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
	private void addToPlayer(int index, SFSArray players)
	{
		ISFSObject p = toSFS(allUsers.get(index));
		p.putInt("s",  index);
		players.addSFSObject(p);
	}

	*/
	private ISFSObject toSFS(int id, RankData r)
	{
		SFSObject player = new SFSObject();
		player.putInt("i", id);
		player.putText("n", r.name);
		player.putInt("p", r.point);
		return player;
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

		/*int i = 0;
		for (RankData r : ranks) {
			if (r.id == id)
				return i;
			i ++;
		}
		return -1;*/
		return 0;
	}
}