package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.Challenge;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class ChallengeJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        LocalDateTime lt = LocalDateTime.now();
        int hour = lt.getHour();  // current hour
        int now = (int) Instant.now().getEpochSecond();
        ISFSObject response = new SFSObject();
        if( hour < com.gt.towers.socials.Challenge.JOIN_HOUR || hour > com.gt.towers.socials.Challenge.START_HOUR )
        {
            response.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
            sendResponse(response, sender);
            return;
        }

        lt = LocalDateTime.of(lt.getYear(), lt.getMonth(), lt.getDayOfMonth(), com.gt.towers.socials.Challenge.START_HOUR, 0);
        Player player = ((Game)sender.getSession().getProperty("core")).player;
        Challenge challenge = ChallengeUtils.getInstance().findWaitingChallenge(0, now);
        if( challenge == null )
            challenge = ChallengeUtils.getInstance().create(0, 50, lt.atZone(ZoneId.systemDefault()).toEpochSecond(), player);
        else
            ChallengeUtils.getInstance().join(challenge, player, true);

        response.putInt("response", MessageTypes.RESPONSE_SUCCEED);
        response.putSFSObject("challenge", challenge);
        sendResponse(response, sender);
    }

    private void sendResponse(ISFSObject response, User sender)
    {
        send(Commands.CHALLENGE_JOIN, response, sender);
    }
}