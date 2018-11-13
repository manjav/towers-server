package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.units.Unit;
import com.smartfoxserver.v2.entities.data.SFSObject;
import java.util.Iterator;
import java.util.Map;

public class TouchDownEndCalculator extends EndCalculator
{
    private int round = 1;

    public TouchDownEndCalculator(BattleRoom roomClass)
    {
        super(roomClass);
    }

    @Override
    public boolean check()
    {
        Unit unit = checkUnitPassed();
        if( unit == null )
            return false;
        roomClass.trace("unit passed " + unit.id);
        round ++;
        scores[unit.side] ++;
        roomClass.battleField.requestReset();
        sendNewRoundResponse(unit.side, unit.id);
        for (int s : scores)
            if( s > 2 )
                return true;

        return false;
    }

    void sendNewRoundResponse(int winner, int unitId)
    {
        SFSObject params = new SFSObject();
        params.putInt("unitId", unitId);
        params.putInt("round", round);
        params.putInt("winner", winner);
        params.putInt("0", scores[0]);
        params.putInt("1", scores[1]);
        roomClass.send(Commands.BATTLE_NEW_ROUND, params, roomClass.getParentRoom().getUserList());
    }

    Unit checkUnitPassed()
    {
        Unit u;
        Iterator<Map.Entry<Object, Unit>> iterator = roomClass.battleField.units._map.entrySet().iterator();
        while( iterator.hasNext() )
        {
           u = iterator.next().getValue();
           if( (u.side == 0 && u.y <= roomClass.battleField.tileMap.tileHeight) || (u.side == 1 && u.y >= BattleField.HEIGHT - roomClass.battleField.tileMap.tileHeight) )
               return u;
        }
        return null;
    }
}