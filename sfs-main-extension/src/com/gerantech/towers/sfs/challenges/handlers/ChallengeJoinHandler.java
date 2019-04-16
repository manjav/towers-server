package com.gerantech.towers.sfs.challenges.handlers;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.data.ChallengeSFS;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.socials.Challenge;
import com.gt.utils.ChallengeUtils;
import com.gt.utils.ExchangeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

import java.time.Instant;

public class ChallengeJoinHandler extends BBGClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)sender.getSession().getProperty("core");

        // check already joined in a challenge
        if( ChallengeUtils.getInstance().getChallengesOfAttendee(params.getInt("type"), game.player, false).size() > 0 )
        {
            send(Commands.CHALLENGE_JOIN, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }

        // check player has requirements
        int response = ExchangeUtils.getInstance().process(game, Challenge.getExchangeItem(params.getInt("type"), Challenge.getJoinRequiements(params.getInt("type")), game.player.get_arena(0)), 0, 0);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            send(Commands.CHALLENGE_JOIN, response, params, sender);
            return;
        }

        // create and join challenge
        ChallengeSFS challenge = ChallengeUtils.getInstance().join(params.getInt("type"), game.player.id, game.player.nickName, (int) Instant.now().getEpochSecond());
        if( challenge == null )
        {
            send(Commands.CHALLENGE_JOIN, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        params.putSFSObject("challenge", challenge);
        send(Commands.CHALLENGE_JOIN, response, params, sender);
    }
}