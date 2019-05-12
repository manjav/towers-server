package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.units.Unit;

import java.util.Iterator;
import java.util.Map;

public class TouchDownEndCalculator extends EndCalculator
{
    public int round = 1;
    public TouchDownEndCalculator(BattleRoom room)
    {
        super(room);
    }

    @Override
    public boolean check()
    {
        Unit unit = checkUnitPassed();
        if( unit == null )
            return false;
        room.trace("unit passed " + unit.id);
        round ++;
        scores[unit.side] ++;
        room.battleField.requestReset();
        room.sendNewRoundResponse(unit.side, unit.id);
        for (int s : scores)
            if( s > 2 )
                return true;

        return false;
    }

    Unit checkUnitPassed()
    {
        Unit u;
        Iterator<Map.Entry<Object, Unit>> iterator = room.battleField.units._map.entrySet().iterator();
        while( iterator.hasNext() )
        {
           u = iterator.next().getValue();
           if( (u.side == 0 && u.y <= room.battleField.field.tileMap.tileHeight) || (u.side == 1 && u.y >= BattleField.HEIGHT - room.battleField.field.tileMap.tileHeight) )
               return u;
        }
        return null;
    }
}