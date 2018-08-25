package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.data.LobbyData;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyEditHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game) sender.getSession().getProperty("core"));
        Room lobby = getParentExtension().getParentRoom();
        LobbyData lobbyData = (LobbyData)lobby.getProperty("data");

        int memberIndex = LobbyUtils.getInstance().getMemberIndex(lobbyData, game.player.id);
        if( memberIndex < 0 )
            return;
        if( lobbyData.getMembers().getSFSObject(memberIndex).getShort("pr") < DefaultPermissionProfile.MODERATOR.getId() )
            return;

        if( params.containsKey("max") )
            lobby.setMaxUsers(params.getInt("max"));

        if( params.containsKey("name") )    lobbyData.setName(params.getUtfString("name"));
        if( params.containsKey("bio") )     lobbyData.setBio(params.getUtfString("bio"));
        if( params.containsKey("pic") )     lobbyData.setEmblem(params.getInt("pic"));
        if( params.containsKey("max") )     lobbyData.setCapacity(params.getInt("max"));
        if( params.containsKey("min") )     lobbyData.setMinPoint(params.getInt("min"));
        if( params.containsKey("pri") )     lobbyData.setPrivacy(params.getInt("pri"));

        LobbyUtils.getInstance().save(             lobbyData,
                params.containsKey("name") ?    params.getUtfString("name") : null,
                params.containsKey("bio") ?     params.getUtfString("bio")  : null,
                params.containsKey("pic") ?     params.getInt("pic")        : -1,
                params.containsKey("max") ?     params.getInt("max")        : -1,
                params.containsKey("min") ?     params.getInt("min")        : -1,
                params.containsKey("pri") ?     params.getInt("pri")        : -1,
                null, null);

        ((LobbyRoom) lobby.getExtension()).sendComment((short) MessageTypes.M15_COMMENT_EDIT, game.player, "", (short)0);

        //params.putInt("response", RESPONSE_OK);
        //send(Commands.LOBBY_EDIT, params, sender);
    }

}
