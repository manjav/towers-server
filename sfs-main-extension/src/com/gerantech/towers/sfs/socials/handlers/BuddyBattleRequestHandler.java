package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.handlers.BattleAutoJoinHandler;
import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.Arrays;

/**
 * Created by Babak on 9/4/2017.
 */
public class BuddyBattleRequestHandler extends BaseClientRequestHandler
{
    private static final int STATE_REQUEST = 0;
    private static final int STATE_BATTLE_STARTED = 1;


    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game) sender.getSession().getProperty("core")).player;
        short battleState = params.getShort("bs");
        if ( battleState == STATE_REQUEST )
        {
            Integer objectUserId = params.getInt("o");
            User objectUser = getParentExtension().getParentZone().getUserManager().getUserByName(objectUserId + "");
            if( objectUser != null )
            {
                Room room = BattleAutoJoinHandler.makeNewRoom(getApi(), getParentExtension().getParentZone(), sender, false, 0);
                room.setProperty("isFriendly", true);
                BattleAutoJoinHandler.join(getApi(), sender, room, "");
                params.putInt("bid", room.getId());
                params.putInt("s", player.id);
                params.putUtfString("sn", player.nickName);
                sendRresponse(params);
            }
            else
            {
                OneSignalUtils.send(getParentExtension(), player.nickName + " تو رو به رقابت دوستانه دعوت می کنه", "", objectUserId);
                params.putShort("bs", (short) 4);
                sendRresponse(params);
            }
        }
        else if( battleState == STATE_BATTLE_STARTED )
        {
            User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
            Room room = getParentExtension().getParentZone().getRoomById(params.getInt("bid"));
            BattleAutoJoinHandler.join(getApi(), sender, room, "");
            if( subjectUser != null )
                send(Commands.BUDDY_BATTLE, params, Arrays.asList(sender, subjectUser));
        }
        else
        {trace(params.getDump());
            Room room = getParentExtension().getParentZone().getRoomById(params.getInt("bid"));
            if( room != null ) {
                params.putInt("c", player.id);
                getApi().removeRoom(room);
            }
            sendRresponse(params);
        }
    }

    private void sendRresponse(ISFSObject params)
    {
        User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
        if (subjectUser != null)
            send(Commands.BUDDY_BATTLE, params, subjectUser);

        User objectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("o") + "");
        if (objectUser != null)
            send(Commands.BUDDY_BATTLE, params, objectUser);
    }
}