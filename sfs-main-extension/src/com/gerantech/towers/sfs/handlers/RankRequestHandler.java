package com.gerantech.towers.sfs.handlers;

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
	public static int PAGE_SIZE = 50;

	public RankRequestHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));

		IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
		RankData rd = new RankData(game.player.nickName,  game.player.get_point(), game.player.getResource(ResourceType.BATTLES_WEEKLY), game.player.getResource(ResourceType.STARS_WEEKLY));
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
		Set<Map.Entry<Integer, RankData>> result = users.entrySet(pagingPredicate);

		int i = 0;
		int playerIndex = -1;
		int lastHeroPoint = -1;
		for (Map.Entry<Integer, RankData> p: result)
		{
			players.addSFSObject(toSFS(p.getKey(), p.getValue()));
			lastHeroPoint = p.getValue().point;

			if( p.getKey() == game.player.id )
				playerIndex = i;
			i++;
		}

		// fake ranking if not exists in tops
		if( playerIndex == -1 )
		{
			players.addSFSObject( new SFSObject() );

			float rankingRatio = (float) (1 - Math.log(1 + (1.7183 * ((float) playerRD.point / (float) lastHeroPoint))));
			int playerFakeRanking = Math.round(rankingRatio * users.size() * 0.5f) + PAGE_SIZE;

			ISFSObject p = toSFS(game.player.id, playerRD);
			p.putInt("s", playerFakeRanking);
			players.addSFSObject(p);
		}

		params.putSFSArray("list", players);
		send("rank", params, sender);
	}

	private ISFSObject toSFS(int id, RankData r)
	{
		SFSObject player = new SFSObject();
		player.putInt("i", id);
		player.putText("n", r.name);
		player.putInt("p", r.point);
		return player;
	}

}