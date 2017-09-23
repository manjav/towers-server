package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.List;

public class BattleAutoJoinHandler extends BaseClientRequestHandler
{
    private int index;
	private Boolean isQuest;
	private Room theRoom;

	public void handleClientRequest(User sender, ISFSObject params)
    {
        try
        {
            index = params.getInt("i");
            isQuest = params.getBool("q");
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
        catch(Exception err) {  err.printStackTrace(); }
    }
 
	private void joinUser(User user) throws SFSException
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
            theRoom = bu.make(user, isQuest, index, 0);

        bu.join(user, theRoom, "");
    }

    private Room findWaitingBattlsRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        int arenaIndex =  Math.min(BattleUtils.arenaDivider, ((Game)user.getSession().getProperty("core")).player.get_arena(0));
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        for (Room r : rList)
            if (!r.isFull() && !r.containsProperty("isFriendly") && (Integer) r.getProperty("state") == BattleRoom.STATE_WAITING && ((int) r.getProperty("arena")) == arenaIndex)
                return r;
        return null;
    }
}