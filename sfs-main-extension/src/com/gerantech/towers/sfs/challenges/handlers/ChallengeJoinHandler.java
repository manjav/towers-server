package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gt.challenges.ChallengeSFS;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.*;

public class ChallengeJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        // check joining time
        LocalDateTime lt = LocalDateTime.now();
        int now = (int) Instant.now().getEpochSecond();
        ISFSObject response = new SFSObject();
        if( lt.getHour() > com.gt.towers.socials.Challenge.START_HOUR )
        {
            response.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
            sendResponse(response, sender);
            return;
        }

        // check already joined in a challenge
        Player player = ((Game)sender.getSession().getProperty("core")).player;
        if( ChallengeUtils.getInstance().getChallengesOfAttendee(0, player.id).size() > 0 )
        {
            response.putInt("response", MessageTypes.RESPONSE_ALREADY_SENT);
            sendResponse(response, sender);
            return;
        }

        int startAt = now - lt.getHour() * 3600 - lt.getMinute() * 60 - lt.getSecond() + com.gt.towers.socials.Challenge.START_HOUR * 3600;
        ChallengeSFS challenge = ChallengeUtils.getInstance().findWaitingChallenge(0, now);
        if( challenge == null )
            challenge = ChallengeUtils.getInstance().create(0, 50, startAt, now, player);
        else
            ChallengeUtils.getInstance().join(challenge, player, now, true);

        response.putInt("response", MessageTypes.RESPONSE_SUCCEED);
        response.putSFSObject("challenge", challenge);
        sendResponse(response, sender);
    }

    private void sendResponse(ISFSObject response, User sender)
    {
        send(Commands.CHALLENGE_JOIN, response, sender);
    }
}