package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gerantech.towers.sfs.utils.ExchangeManager;
import com.gt.data.ChallengeSFS;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.socials.Challenge;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;

public class ChallengeJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)sender.getSession().getProperty("core");

        // check already joined in a challenge
        if( ChallengeUtils.getInstance().getChallengesOfAttendee(params.getInt("type"), game.player, false).size() > 0 )
        {
            sendResponse(sender, params, MessageTypes.RESPONSE_ALREADY_SENT);
            return;
        }

        // check player has requirements
        int response = ExchangeManager.getInstance().process(game, Challenge.getExchangeItem(params.getInt("type"), game.player.get_arena(0)), 0, 0);
        if( response != MessageTypes.RESPONSE_SUCCEED )
        {
            sendResponse(sender, params, response);
            return;
        }

        // create and join challenge
        ChallengeSFS challenge = ChallengeUtils.getInstance().join(params.getInt("type"), game.player.id, game.player.nickName, (int) Instant.now().getEpochSecond());
        if( challenge == null )
        {
            sendResponse(sender, params, MessageTypes.RESPONSE_NOT_FOUND);
            return;
        }

        params.putSFSObject("challenge", challenge);
        sendResponse(sender, params, response);
    }

    private void sendResponse(User sender, ISFSObject params, int response)
    {
        params.putInt("response", response);
        send(Commands.CHALLENGE_JOIN, params, sender);
    }
}