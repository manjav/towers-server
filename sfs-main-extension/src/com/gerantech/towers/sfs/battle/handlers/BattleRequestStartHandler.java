package com.gerantech.towers.sfs.battle.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.gerantech.towers.sfs.battle.BattleUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.List;

public class BattleRequestStartHandler extends BaseClientRequestHandler
{
    private int index;
    private Boolean isOperation;
    private Boolean hasExtraTime;
	private Room theRoom;

	public void handleClientRequest(User sender, ISFSObject params)
    {
        int now = (int)Instant.now().getEpochSecond();
        if( now < LoginEventHandler.UNTIL_MAINTENANCE )
        {
            params.putInt("umt", LoginEventHandler.UNTIL_MAINTENANCE - now);
            send(Commands.BATTLE_START, params, sender);
            return;
        }

        index = params.getInt("i");
        isOperation = params.getBool("q");
        hasExtraTime = params.containsKey("e");
        if( params.containsKey("su") )
        {
            index -= 100000;
            theRoom = getParentExtension().getParentZone().getRoomById(index);
            if( theRoom != null )
                BattleUtils.getInstance().join (sender, theRoom, params.getText("su"));
            return;
        }
        joinUser(sender);
    }
 
	private void joinUser(User user)
    {
        if( !isOperation )
        {
            int joinedRoomId = (Integer) user.getSession().getProperty("joinedRoomId");
            if( joinedRoomId > -1 )
                theRoom = getParentExtension().getParentZone().getRoomById(joinedRoomId);
            else
                theRoom = findWaitingBattlsRoom(user);
        }

        BattleUtils bu = BattleUtils.getInstance();
        if( theRoom == null )
            theRoom = bu.make(user, isOperation, index, 0, hasExtraTime);

        bu.join(user, theRoom, "");
    }

    private Room findWaitingBattlsRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        int arenaIndex = ((Game) user.getSession().getProperty("core")).player.get_arena(0);
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        Room room = null;
        try {
          for(int r=0; r<rList.size(); r++)
            {
                room = rList.get(r);

                if (!room.isFull() && !room.containsProperty("isFriendly") && (int) room.getProperty("state") == BattleRoom.STATE_WAITING && ((int) room.getProperty("arena")) == arenaIndex)
                    return room;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            if( room != null )
                trace("isFriendly:"+room.containsProperty("isFriendly"),  "state:"+room.getProperty("state"), "arena:"+room.getProperty("arena"));
        }
        return null;
    }
}