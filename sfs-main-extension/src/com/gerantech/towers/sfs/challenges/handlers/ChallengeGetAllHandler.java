package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChallengeGetAllHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game)sender.getSession().getProperty("core")).player;
        params.putSFSArray("challenges", ChallengeUtils.getInstance().getChallengesOfAttendee(-1, player.id));

        send(Commands.CHALLENGE_GET_ALL, params, sender);
    }
}