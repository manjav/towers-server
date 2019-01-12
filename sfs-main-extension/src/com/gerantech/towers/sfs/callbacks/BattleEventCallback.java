package com.gerantech.towers.sfs.callbacks;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.GameObject;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.constants.CardTypes;
import com.gt.towers.events.BattleEvent;
import com.gt.towers.events.EventCallback;
import com.smartfoxserver.v2.entities.data.SFSObject;
import haxe.root.Array;

import java.util.Iterator;
import java.util.Map;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;
//    private boolean waitForConquest = true;

    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        Iterator<Map.Entry<Object, Unit>> iterator = battleRoom.battleField.units._map.entrySet().iterator();
        while( iterator.hasNext() )
        {
            iterator.next().getValue().eventCallback = this;
        }
    }

    @Override
    public void dispatch(int id, String type, Object data)
    {
        // only headquarter battle is alive
        if( battleRoom.battleField.state >= BattleField.STATE_4_ENDED || !battleRoom.battleField.field.type.equals(FieldData.TYPE_HEADQUARTER) )
            return;

        // when units disposed
        if( type == BattleEvent.STATE_CHANGE && (int) data == GameObject.STATE_8_DIPOSED )
        {
            if( battleRoom.battleField.units.get(id).card.type >= 201 )
            {
                int other = battleRoom.battleField.units.get(id).side == 0 ? 1 : 0;
                if( battleRoom.battleField.units.get(id).card.type == 201 )
                    battleRoom.endCalculator.scores[other] = Math.min(3, battleRoom.endCalculator.scores[other] + 3);
                if( CardTypes.isHero(battleRoom.battleField.units.get(id).card.type) )
                    battleRoom.endCalculator.scores[other] ++;

                SFSObject params = new SFSObject();
                params.putInt("winner", other);
                params.putInt("0", battleRoom.endCalculator.scores[0]);
                params.putInt("1", battleRoom.endCalculator.scores[1]);
                battleRoom.send(Commands.BATTLE_NEW_ROUND, params, battleRoom.getParentRoom().getUserList());
            }
        }
    }

    @Override
    public boolean __hx_deleteField(String s) {
        return false;
    }

    @Override
    public Object __hx_lookupField(String s, boolean b, boolean b1) {
        return null;
    }

    @Override
    public double __hx_lookupField_f(String s, boolean b) {
        return 0;
    }

    @Override
    public Object __hx_lookupSetField(String s, Object o) {
        return null;
    }

    @Override
    public double __hx_lookupSetField_f(String s, double v) {
        return 0;
    }

    @Override
    public double __hx_setField_f(String s, double v, boolean b) {
        return 0;
    }

    @Override
    public Object __hx_setField(String s, Object o, boolean b) {
        return null;
    }

    @Override
    public Object __hx_getField(String s, boolean b, boolean b1, boolean b2) {
        return null;
    }

    @Override
    public double __hx_getField_f(String s, boolean b, boolean b1) {
        return 0;
    }

    @Override
    public Object __hx_invokeField(String s, Array array) {
        return null;
    }

    @Override
    public void __hx_getFields(Array<String> array) {

    }
}
