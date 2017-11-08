package com.gerantech.towers.sfs.administration;

import com.gerantech.towers.sfs.battle.handlers.BattleRoomLeaveRequestHandler;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SpectateRoom extends SFSExtension
{
	private int numUsers = 0;
	private Room room;
	private Timer timer;

	public void init() 
	{
		room = getParentRoom();
		addRequestHandler("leave", BattleRoomLeaveRequestHandler.class);
		List<RoomVariable> listOfVars = new ArrayList();
		listOfVars.add( new SFSRoomVariable("rooms", SFSArray.newInstance()) );
		sfsApi.setRoomVariables(null, room, listOfVars);

    	timer = new Timer();
    	timer.schedule(new TimerTask() {

			@Override
		public void run() {
			ISFSArray reservedRooms = room.getVariable("rooms").getSFSArrayValue();

			SFSArray vars = SFSArray.newInstance();
			String uvar = "";
			SFSObject rvar;
			List<Room> rooms = getParentZone().getRoomListFromGroup(room.getName());
			for ( Room r : rooms )
			{
				rvar = new SFSObject();
				rvar.putInt("id", r.getId());
				rvar.putText("name", r.getName());
				rvar.putInt("startAt", (Integer)r.getProperty("startAt"));
				uvar = "";
				for ( User u : r.getUserList() )
					uvar += u.getName() + ",";
				rvar.putText("users", uvar);
				vars.addSFSObject(rvar);
			}

			if( isChanged(reservedRooms, vars) ) {
				List<RoomVariable> listOfVars = new ArrayList();
				listOfVars.add(new SFSRoomVariable("rooms", vars));
				sfsApi.setRoomVariables(null, room, listOfVars);
			}

			}


		}, 0, 500);
		trace(room.getName(), "created.");
	}

	private boolean isChanged(ISFSArray reservedRooms, SFSArray newRooms) {
		int nu = room.getUserList().size();
		if( numUsers != nu )
		{
			numUsers = nu;
			return true;
		}
		if( reservedRooms.size() != newRooms.size() )
			return true;
		//trace(reservedRooms.size(), newRooms.size());

		for (int i = 0; i < reservedRooms.size(); i++)
			if( !reservedRooms.getSFSObject(i).getText("users").equals(newRooms.getSFSObject(i).getText("users")) )
				return true;
		return false;
	}

	public void destroy()
	{
		clearAllHandlers();
		if(timer != null)
			timer.cancel();
		timer = null;
		trace(room.getName(), "destroyed.");
		super.destroy();
	}
}