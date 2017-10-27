package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.List;

public class BattleAutoJoinHandler extends BaseClientRequestHandler
{
    private int index;
    private Boolean isQuest;
    private Boolean hasExtraTime;
	private Room theRoom;

	public void handleClientRequest(User sender, ISFSObject params)
    {
        int now = (int)Instant.now().getEpochSecond();
        trace(now, LoginEventHandler.UNTIL_MAINTENANCE);
        if( now < LoginEventHandler.UNTIL_MAINTENANCE )
        {
            params.putInt("umt", LoginEventHandler.UNTIL_MAINTENANCE - now);
            send(Commands.START_BATTLE, params, sender);
            return;
        }

        index = params.getInt("i");
        isQuest = params.getBool("q");
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
        if( !isQuest )
        {
            int joinedRoomId = (Integer) user.getSession().getProperty("joinedRoomId");
            if( joinedRoomId > -1 )
                theRoom = getParentExtension().getParentZone().getRoomById(joinedRoomId);
            else
                theRoom = findWaitingBattlsRoom(user);
        }


        BattleUtils bu = BattleUtils.getInstance();
        if (theRoom == null)
            theRoom = bu.make(user, isQuest, index, 0, hasExtraTime);

        bu.join(user, theRoom, "");
    }

    private Room findWaitingBattlsRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        Game game = ((Game)user.getSession().getProperty("core"));
        Double arenaIndex =  Math.min(BattleUtils.arenaDivider, Math.floor(game.player.get_arena(0)/2)*2);
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        for (Room r : rList) {
            //trace("arena", r.getProperty("arena"), arenaIndex, ((int) r.getProperty("arena")) == arenaIndex.intValue(), ((int) r.getProperty("appVersion")) == game.appVersion );
            if ( !r.isFull() && !r.containsProperty("isFriendly") && (Integer) r.getProperty("state") == BattleRoom.STATE_WAITING && ((int) r.getProperty("arena")) == arenaIndex.intValue() )
                return r;
        }
        return null;
    }
}