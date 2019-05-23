package com.gerantech.towers.sfs.socials;

import com.gt.Commands;
import com.gerantech.towers.sfs.socials.handlers.LobbyReportHandler;
import com.gerantech.towers.sfs.socials.handlers.PublicMessageHandler;
import com.gt.data.LobbySFS;
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
    protected Game game;
    protected int mode;
    protected Room lobby;
    protected LobbySFS data;
    protected ISFSArray messages;

    public void init()
    {
        lobby = getParentRoom();
        data = (LobbySFS) lobby.getProperty("data");
        addRequestHandler(Commands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
        addRequestHandler(Commands.LOBBY_REPORT, LobbyReportHandler.class);
    }

    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        try {
            if( requestId.equals(Commands.LOBBY_PUBLIC_MESSAGE) )
                organizeMessage(sender, params, true);
            super.handleClientRequest(requestId, sender, params);
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected void organizeMessage(User sender, ISFSObject params, boolean alreadyAdd)
    {
        // Add time and user-id to message
        if( sender != null )
        {
            game = ((Game) sender.getSession().getProperty("core"));

            params.putInt("i", game.player.id);
            params.putUtfString("s", game.player.nickName);
        }

        params.putInt("u", (int) Instant.now().getEpochSecond());
        if( !params.containsKey("m") )
            params.putInt("m", MessageTypes.M0_TEXT);
        mode = params.getInt("m");
        messages = messageQueue();

        // Max 30 len message queue
        while (messages.size() > 30)
            messages.removeElementAt(0);

        if( mode == MessageTypes.M0_TEXT )
        {
            if( params.getUtfString("t").length() > 160 )
                params.putUtfString("t", params.getUtfString("t").substring(0, 160) + " ...");
            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if( last != null && last.getInt("m") == MessageTypes.M0_TEXT && last.getInt("i") == game.player.id )
            {
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