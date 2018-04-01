package com.gerantech.towers.sfs.callbacks;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.Troop;
import com.gt.towers.interfaces.ITroopHitCallback;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;
import haxe.root.Array;

import java.util.List;

/**
 * Created by ManJav on 4/1/2018.
 */
public class HitTroopCallback implements ITroopHitCallback
{
    private final SFSExtension room;

    public HitTroopCallback(ISFSExtension room)
    {
        this.room = (SFSExtension) room;
    }


    @Override
    public void hit(int defenderId, List<Troop> troops)
    {
        String log = "hit defenderId:" + defenderId;
        for (int i = 0; i <troops.size() ; i++)
            log += "[ troopId:" + troops.get(i).id + " troopHealth:" + troops.get(i).health  +(i == troops.size()-1 ? " ]":" ,  ");

        room.trace(log);
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
