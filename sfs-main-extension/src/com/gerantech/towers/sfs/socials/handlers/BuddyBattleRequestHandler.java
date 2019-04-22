package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.utils.BanUtils;
import com.gt.utils.BattleUtils;
import com.gt.utils.InboxUtils;
import com.gt.utils.OneSignalUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.util.Arrays;

/**
 * Created by ManJav on 9/4/2017.
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
                Room room = BattleUtils.getInstance().make(sender, params.getInt("m"), 0, 2);
                BattleUtils.getInstance().join(sender, room, "");
                params.putInt("bid", room.getId());
                params.putInt("s", player.id);
                params.putUtfString("sn", player.nickName);
                sendResponse(params);
            }
            else
            {
                OneSignalUtils.getInstance().send(player.nickName + " تو رو به رقابت دوستانه دعوت می کنه ", null, objectUserId);
                params.putShort("bs", (short) 4);
                sendResponse(params);
            }
        }
        else if( battleState == STATE_BATTLE_STARTED )
        {
            User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
            Room room = getParentExtension().getParentZone().getRoomById(params.getInt("bid"));
            BattleUtils.getInstance().join(sender, room, "");
            if( subjectUser != null )
                send(Commands.BUDDY_BATTLE, params, Arrays.asList(sender, subjectUser));
        }
        else
        {
            Room room = getParentExtension().getParentZone().getRoomById(params.getInt("bid"));
            if( room != null )
            {
                params.putInt("c", player.id);
                getApi().removeRoom(room);
            }
            sendResponse(params);

            // Send requested battle to subjectUser's inbox
            if( player.id != params.getInt("o") )
                InboxUtils.getInstance().send(0, player.nickName + " تو رو به مبارزه دعوت کرد. ", BanUtils.SYSTEM_ID, params.getInt("o"),"");
        }
    }

    private void sendResponse(ISFSObject params)
    {
        User subjectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("s") + "");
        if (subjectUser != null)
            send(Commands.BUDDY_BATTLE, params, subjectUser);

        User objectUser = getParentExtension().getParentZone().getUserManager().getUserByName(params.getInt("o") + "");
        if (objectUser != null)
            send(Commands.BUDDY_BATTLE, params, objectUser);
    }
}