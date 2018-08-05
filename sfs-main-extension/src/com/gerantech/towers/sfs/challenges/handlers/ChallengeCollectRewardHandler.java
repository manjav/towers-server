package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gerantech.towers.sfs.utils.ExchangeManager;
import com.gt.challenges.ChallengeData;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;

public class ChallengeCollectRewardHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game)sender.getSession().getProperty("core"));
        int now = (int) Instant.now().getEpochSecond();
        int response = ChallengeUtils.getInstance().collectReward(params.getInt("id"), game.player.id, now);
        params.putInt("response", response);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(Commands.CHALLENGE_COLLECT, params, sender);
            return;
        }

        // add rewards based on rank
        ChallengeData challenge = ChallengeUtils.getInstance().get(params.getInt("id"));
        ExchangeManager manager = ExchangeManager.getInstance();
        ExchangeItem ei = new ExchangeItem(null);
        ei.type = ExchangeType.C121_MAGIC;
        ei.requirements = new IntIntMap();
        ei.outcomes = new IntIntMap();
        ei.outcomes.set(challenge.base.getRewardByRank(1), game.player.get_arena(0));

        response =  manager.process(game, ei, now, 0);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(Commands.CHALLENGE_COLLECT, params, sender);
            return;
        }

        params.putSFSArray("rewards", manager.getRewards());
        send(Commands.CHALLENGE_COLLECT, params, sender);
    }
}