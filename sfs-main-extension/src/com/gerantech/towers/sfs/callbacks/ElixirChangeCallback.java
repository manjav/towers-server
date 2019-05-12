package com.gerantech.towers.sfs.callbacks;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.callbacks.MapChangeCallback;
import com.gt.Commands;
import com.smartfoxserver.v2.entities.data.SFSObject;

public class ElixirChangeCallback extends MapChangeCallback
{
    private final BattleRoom room;

    public ElixirChangeCallback(BattleRoom room)
    {
        super();
        this.room = room;
    }

    @Override
    public void update(int side, int oldValue, int newValue)
    {
        SFSObject params = new SFSObject();
        params.putInt(side + "", newValue);
        room.getZone().getExtension().send(Commands.BATTLE_ELIXIR_UPDATE, params, room.getUserList());
    }
}
