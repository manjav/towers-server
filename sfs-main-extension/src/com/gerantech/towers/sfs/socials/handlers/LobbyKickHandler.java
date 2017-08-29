package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/28/2017.
 */
public class LobbyKickHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game) sender.getSession().getProperty("core")).player;
        Room room = getParentExtension().getParentRoom();
        LobbyRoom roomClass = (LobbyRoom) room.getExtension();trace(roomClass);
        User kickedUser = room.getUserByName(params.getInt("id").toString());
        if( kickedUser != null )
            getApi().leaveRoom(kickedUser, room, true, false);

        roomClass.sendComment((short) MessageTypes.M12_COMMENT_KICK, player.nickName, params.getUtfString("name"), (short)-1);// mode = leave
        LobbyRoomServerEventsHandler.removeUserFromRoomVar(getParentExtension(), room, params.getInt("id"));
        OneSignalUtils.send(getParentExtension(), params.getUtfString("name") + " متأسفانه " + player.nickName + " تو رو از دهکده اخراج کرد.", null, params.getInt("id"));
    }
}