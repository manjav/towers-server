package com.gerantech.towers.sfs.challenges.handlers;
import com.gt.Commands;
import com.gt.callbacks.MapChangeCallback;
import com.gt.utils.ChallengeUtils;
import com.gt.utils.ExchangeUtils;
import com.gt.data.ChallengeSFS;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
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
        ChallengeSFS challenge = ChallengeUtils.getInstance().get(params.getInt("id"));
        challenge.representAttendees();

        ExchangeUtils manager = ExchangeUtils.getInstance();
        ExchangeItem ei = new ExchangeItem(null);
        ei.type = ExchangeType.C121_MAGIC;
        ei.requirements = new IntIntMap();
        ei.outcomes = challenge.base.getRewardByAttendee(game.player.id);
        int rewardType = ei.outcomes.keys()[0];
        if( ResourceType.isBook( rewardType ) )
            ei.outcomes.set(rewardType, game.player.get_arena(0));


        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        response =  manager.process(game, ei, now, 0, mapChangeCallback);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(Commands.CHALLENGE_COLLECT, params, sender);
            return;
        }

        params.putSFSArray("rewards", manager.getRewards(mapChangeCallback));
        send(Commands.CHALLENGE_COLLECT, params, sender);
    }
}