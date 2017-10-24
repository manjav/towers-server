package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyEditHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game) sender.getSession().getProperty("core"));
        Room lobby = getParentExtension().getParentRoom();
        LobbyRoom roomClass = (LobbyRoom) lobby.getExtension();

        try {
            SFSRoomVariable var = null;
            var = new SFSRoomVariable("bio", params.getUtfString("bio"), false, true, false);
            var.setHidden(true);
            lobby.setVariable(var);
            var = new SFSRoomVariable("pic", params.getInt("pic"),       false, true, false);
            var.setHidden(true);
            lobby.setVariable(var);
            var = new SFSRoomVariable("min", params.getInt("min"),       false, true, false);
            var.setHidden(true);
            lobby.setVariable(var);
        } catch (SFSVariableException e) { e.printStackTrace(); }

        LobbyUtils.getInstance().save(lobby);
        roomClass.sendComment((short) 15, game.player.nickName, "", (short)0);

        //params.putInt("response", RESPONSE_OK);
        //send(Commands.LOBBY_EDIT, params, sender);
    }

}
