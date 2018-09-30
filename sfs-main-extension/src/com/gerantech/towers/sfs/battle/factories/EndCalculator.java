package com.gerantech.towers.sfs.battle.factories;
import com.gerantech.towers.sfs.battle.BattleRoom;
public class EndCalculator
{
    protected final BattleRoom roomClass;
    public int[] scores = new int[2];
    public EndCalculator(BattleRoom roomClass)
    {
        this.roomClass = roomClass;
    }
    public boolean check()
    {
        return false;
    }
    public float ratio()
    {
        if( scores[0] == 0 && scores[1] == 0 )
            return 1;
       return (float) scores[0] / (float) scores[1];
    }
}
