package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.handlers.LobbyInfoHandler;
import com.gerantech.towers.sfs.socials.handlers.PublicMessageHandler;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.time.Instant;

/**
 * Created by ManJav on 2/1/2018.
 */
public class BaseLobbyRoom extends SFSExtension
{
    protected Room lobby;
    protected Game game;
    protected Short mode;
    protected ISFSArray messages;

    public void init()
    {
        lobby = getParentRoom();
        addRequestHandler(Commands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
    }

    public void handleClientRequest(String requestId, User sender, ISFSObject params) {
        if( requestId.equals(Commands.LOBBY_PUBLIC_MESSAGE) )
            organizeMessage(sender, params, true);
        super.handleClientRequest(requestId, sender, params);
    }

    protected void organizeMessage(User sender, ISFSObject params, boolean alreadyAdd)
    {
        // Add time and user-id to message
        game = ((Game) sender.getSession().getProperty("core"));
        if( !params.containsKey("m") )
            params.putShort("m", (short) MessageTypes.M0_TEXT);
        params.putInt("u", (int) Instant.now().getEpochSecond());
        params.putInt("i", game.player.id);
        params.putText("s", game.player.nickName);

        mode = params.getShort("m");
        messages = messageQueue();
        // Max 30 len message queue
        while (messages.size() > 30)
            messages.removeElementAt(0);

        if( mode == MessageTypes.M0_TEXT )
        {
            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if (last != null && last.getShort("m") == MessageTypes.M0_TEXT && last.getInt("i") == game.player.id) {
                params.putUtfString("t", last.getUtfString("t") + "\n" + params.getUtfString("t"));
                messages.removeElementAt(messages.size() - 1);
            }
        }
        if( alreadyAdd )
            messages.addSFSObject(params);
    }
    protected ISFSArray messageQueue ()
    {
        return lobby.getVariable("msg").getSFSArrayValue();
    }
}