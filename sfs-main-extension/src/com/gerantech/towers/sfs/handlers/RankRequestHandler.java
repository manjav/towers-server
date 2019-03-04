package com.gerantech.towers.sfs.handlers;

import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.utils.RankingUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ManJav
 *
 */
public class RankRequestHandler extends BaseClientRequestHandler 
{
	public static int PAGE_SIZE = 100;

	public RankRequestHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = ((Game)sender.getSession().getProperty("core"));

		Map<Integer, RankData> users = RankingUtils.getInstance().getUsers();
		RankData rd = new RankData(game.player.nickName,  game.player.get_point());
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
		Comparator<? super Map.Entry<Integer, RankData>> descendingComparator2 = (e1, e2) -> e2.getValue().point - e1.getValue().point;
		List<Map.Entry<Integer, RankData>> result = users.entrySet().stream().parallel().sorted(descendingComparator2).limit(PAGE_SIZE).collect(Collectors.toList());

		long milis = Instant.now().toEpochMilli();

		int i = 0;
		int playerIndex = -1;
		int lastHeroPoint = -1;
		for (Map.Entry<Integer, RankData> p : result)
		{
			players.addSFSObject(toSFS(p.getKey(), p.getValue()));
			lastHeroPoint = p.getValue().point;

			if( p.getKey() == game.player.id )
				playerIndex = i;
			i++;
		}
		trace("===>", Instant.now().toEpochMilli() - milis);

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