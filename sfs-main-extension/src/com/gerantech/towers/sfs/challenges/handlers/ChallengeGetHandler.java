package com.gerantech.towers.sfs.challenges.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChallengeGetHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        params.putSFSObject("challenge", ChallengeUtils.getInstance().get(params.getInt("id")));
        send(Commands.CHALLENGE_GET, params, sender);
    }
}