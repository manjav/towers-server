package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Building;
import com.gt.towers.buildings.Place;
import com.gt.towers.constants.BuildingType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.constants.TroopType;
import com.gt.towers.utils.PathFinder;
import com.gt.towers.utils.lists.IntList;
import com.gt.towers.utils.lists.PlaceList;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 1/25/2018.
 */
public class BattleBot
{
    public int coverPoint = -1;
    public ISFSArray offenders;
    protected double targetHealth;

    protected double fightersPower;
    protected final SFSExtension ext;

    protected final BattleRoom battleRoom;
    protected final BattleField battleField;
    int sampleTime;

    int timeFactor;
    int battleRatio = 0;
    int lastTarget = -1;
    int repeatetiveTarget = -1;
    double troopsDivision;
    PlaceList allPlaces;
    SFSObject chatPatams;
    ConcurrentHashMap<Integer, ScheduledPlace> fighters;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ArrayList<Game> registeredPlayers = (ArrayList)battleRoom.getParentRoom().getProperty("registeredPlayers");

        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
        timeFactor = Math.min(6, Math.max(2, 10 - battleField.difficulty ) );
        troopsDivision = Math.max(0.3, Math.min(0.9, battleField.difficulty * 0.4));
        if( registeredPlayers.get(0).player.get_battleswins() < 2 )
            troopsDivision = 0.15;
        ext.trace("p-point:" + registeredPlayers.get(0).player.resources.get(ResourceType.POINT), "b-point:"+ registeredPlayers.get(1).player.resources.get(ResourceType.POINT), " winStreak:" + registeredPlayers.get(0).player.resources.get(ResourceType.WIN_STREAK), "difficulty:" + battleField.difficulty, "timeFactor:" + timeFactor, "troopsDivision:" + troopsDivision);

        allPlaces = battleField.getPlacesByTroopType(TroopType.NONE, true);

        chatPatams = new SFSObject();
        chatPatams.putLong("ready", battleField.now + 20000);
    }

    public void update()
    {
        int _sampleTime = (int) Math.floor((battleField.now/1000) % timeFactor);
        if( timeFactor == 1 || ( _sampleTime == 0 && _sampleTime != sampleTime ) )
            fightToWeakestPlace();

        sampleTime = _sampleTime;
        cover();
        updateChatProcess();
        updateFightingProcess();
    }

    /**
     * cover for defence main places
     */
    void cover()
    {
        if( battleField.difficulty < 5 || coverPoint < 0 )
            return;

        Place target = battleField.places.get(coverPoint);
        if( target.building.troopType != TroopType.T_1 || battleField.getPlacesByTroopType(TroopType.T_1, false).size() < 3 )
        {
            coverPoint = -1;
            return;
        }

        coverPoint = -2;
        int step = offenders.size() - 1;
        double totalPowers = 0;
        while( step >= 0 )
        {
            totalPowers += estimatePower( battleField.places.get(offenders.getInt(step)).building, 0.6);
            step --;
        }

        if( totalPowers < estimateHealth(target) * 0.8 )
        {
            coverPoint = -1;
            return;
        }

        fightToPlace(target, totalPowers);
    }

    /**
     * find and fight weakest place
     */
    void fightToWeakestPlace()
    {
        // wait for covering
        if( coverPoint == -2 )
            return;

        int step = allPlaces.size() - 1;
        int weakestPlace = -1;
        double mostPriority = 1000;
        List<Integer> samePriorities = new ArrayList();
        while (step >= 0)
        {
            if( isNeighbor(allPlaces.get(step)) )
            {
                double pr = priority(allPlaces.get(step));
                if( mostPriority >= pr )
                {
                    if( mostPriority == pr )
                        samePriorities.add(step);
                    else
                        samePriorities.clear();

                    if( samePriorities.size() == 0)
                        samePriorities.add(step);

                    mostPriority = pr;
                }
            }
            step --;
        }
        // random place
        if( samePriorities.size() > 0 )
            weakestPlace = samePriorities.get((int) Math.floor(Math.random() * samePriorities.size()));
        if( weakestPlace > -1 )
            fightToPlace(allPlaces.get(weakestPlace), 0);
    }

    void fightToPlace(Place target, double forceTargetHealth)
    {
        if( repeatetiveTarget % 100 == target.index )
            repeatetiveTarget += 100;
        else
            repeatetiveTarget = target.index;


        fightersPower = 0;
        targetHealth = forceTargetHealth > 0 ? forceTargetHealth : estimateHealth(target);

        // estimate powers of sides
        PlaceList playerPlaces = battleField.getPlacesByTroopType(TroopType.T_0, false);
        PlaceList robotPlaces = battleField.getPlacesByTroopType(TroopType.T_1, false);
        float playerImproveRatio = estimateImproveMode(playerPlaces);
        float robotImproveRatio = estimateImproveMode(robotPlaces);

        // end battle
        if( playerPlaces.size() == 0 || robotPlaces.size() == 0 )
            return;

        // check stop fighting if needs to improvement
        if( playerPlaces.size() >= 2 && robotImproveRatio < playerImproveRatio )
        {
            //ext.trace(estimateImproveMode(robotPlaces), estimateImproveMode(playerPlaces) );
            if( improveAll(robotPlaces, false, true) )
                return;
        }

        IntList fightersCandidates = new IntList();
        if( fighters == null )
            fighters = new ConcurrentHashMap<Integer, ScheduledPlace>();
        addFighters(target, fightersCandidates);

        //ext.trace("target:" + target.index, " numFighters:" + fighters.size(), " fightersPower:" + fightersPower, " targetHealth:" + targetHealth, fighters.size() > 0 && (fightersPower >= targetHealth || forceTargetHealth > 0) );
        boolean firstToFight = fighters.size() > 0 && (fightersPower >= targetHealth || forceTargetHealth > 0 );
        if( firstToFight )
            scheduleFighters(target);
        else if( !improveAll(robotPlaces, firstToFight, false))
            scheduleFighters(target);
        //ext.trace("target:" + target.index, "covered with " + forceTargetHealth, "numFighters:" + fighters.size(), forceTargetHealth);

        if( !firstToFight && repeatetiveTarget > 1000 )
            return;

        startChating(robotPlaces.size() - playerPlaces.size());
    }

    void addFighters(Place place, IntList fightersCandidates)
    {
        if( fightersCandidates.indexOf(place.index) > -1 || (fightersPower >= targetHealth && battleField.now < battleField.getTime(1)) )
            return;
        fightersCandidates.push(place.index);

        if( place.building.troopType == TroopType.T_1 && !fighters.containsKey(place.index) && place.index != coverPoint)
        {
            double placePower = estimatePower(place.building, hasEnemyNeighbor(place) ? 0.2 : troopsDivision);
            //ext.trace(place.index, "placePower", placePower, "cardPower", cardPower, fightersPower);

            fightersPower += placePower;
            fighters.put(place.index, new ScheduledPlace(place));
        }

        // transform neighbors
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while (step >= 0)
        {
            addFighters(placeLinks.get(step), fightersCandidates);
            step --;
        }
    }

    void scheduleFighters(Place target)
    {
        lastTarget = target.index;

        // estimate max time distance
        Iterator<Map.Entry<Integer, ScheduledPlace>> iterator = fighters.entrySet().iterator();
        while (iterator.hasNext())
        {
            ScheduledPlace sPlace = iterator.next().getValue();
            if( sPlace.fightTime > -1 )
            {
                sPlace.fightTime = battleField.now + estimateRushTime(sPlace.place, target);
                sPlace.target = target.index;
            }
        }
    }

    private void updateFightingProcess()
    {
        if( fighters == null || fighters.size() == 0 )
        {
            if( coverPoint == -2 )
                coverPoint = -1;
            return;
        }

        // shoot every places in determined times
        Iterator<Map.Entry<Integer, ScheduledPlace>> iterator = fighters.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer, ScheduledPlace> entry = iterator.next();
            ScheduledPlace sPlace = entry.getValue();
            //ext.trace("index", sPlace.place.index, "target", sPlace.target, "troopType", sPlace.place.building.troopType, "fightTime", sPlace.fightTime);
            if( sPlace.place.building.troopType == TroopType.T_1 )
            {
                if( sPlace.fightTime > battleField.now )
                {
                    SFSArray _fighters = new SFSArray();
                    _fighters.addInt(sPlace.place.index);
                    battleRoom.fight(_fighters, sPlace.target, true, troopsDivision);
                    sPlace.dispose();
                    fighters.remove(sPlace.place.index);
                }
            }
            else
            {
                sPlace.dispose();
                fighters.remove(sPlace.place.index);
            }
        }
    }


    boolean improveAll(PlaceList places, boolean oneImprove, Boolean hasCard)
    {
        int step = places.size() - 1;
        boolean ret = false;
        while ( step >= 0 )
        {
            if( improve(places.get(step), true, hasCard) )
                ret = true;
            if( ret && oneImprove)
                return true;
            step --;
        }
        return ret;
    }
    boolean improve(Place place, boolean needPopulation, Boolean hasCard)
    {
        if( battleField.difficulty < 2 )
            return false;

       // if( place.fightTime > -1 )
       // {
          //  ext.trace(place.index + " exists in fighters...");
            //return false;
       // }

        int improveType = 0;
        if ( place.building.type == BuildingType.B01_CAMP )
        {
            double rand = Math.random();
            if( needPopulation )
                improveType = BuildingType.B11_BARRACKS;
            else if (rand > 0.9)
                improveType = BuildingType.B41_CRYSTAL;
            else if (rand > 0.8)
                improveType = BuildingType.B31_HEAVY;
            else if (rand > 0.7)
                improveType = BuildingType.B21_RAPID;
            else
                improveType = BuildingType.B11_BARRACKS;
        }
        else
        {
            improveType = place.building.type + 1;
        }

        if( hasCard )
        {
            place.building.improve(improveType%10>1?BuildingType.IMPROVE:improveType);
            return battleField.games.get(1).player.buildings.exists(improveType);
        }
        return place.building.improve(improveType%10>1?BuildingType.IMPROVE:improveType);
    }

    void startChating(int battleRatio)
    {
        if( battleField.map.isQuest )
            return;

        // verbose bot threshold
        if( chatPatams.getLong("ready") > battleField.now || Math.random() > 0.1 )
            return;

        //ext.trace(this.battleRatio, battleRatio);
        if( battleRatio != this.battleRatio )
        {
            chatPatams.putInt("t", StickerType.getRandomStart(battleRatio));
            chatPatams.putInt("tt", 1);
            chatPatams.putLong("u", (long) (battleField.now + Math.random() * 2500 + 500));
        }
        this.battleRatio = battleRatio;
    }

    public void answerChat(ISFSObject params)
    {
        if( chatPatams.getLong("ready") > battleField.now || Math.random() < 0.2 )
            return;

        int answer = StickerType.getRandomAnswer( params.getInt("t") );
        if( answer <= -1 )
            return;

        chatPatams.putInt("t", answer);
        chatPatams.putInt("tt", 1);
        chatPatams.putInt("wait", 0);
        chatPatams.putLong("u", (long) (battleField.now + Math.random() * 3500 + 1500));
    }

    void updateChatProcess()
    {
        if( chatPatams.getLong("ready") > battleField.now || !chatPatams.containsKey("t") )
            return;

        battleRoom.sendSticker(null, chatPatams);
        chatPatams.removeElement("t");
        chatPatams.putLong("ready", battleField.now + 10000);
    }

    // tools
    boolean isNeighbor(Place place)
    {
        if( place.building.troopType == TroopType.T_1 )
            return false;
        return place.getLinks(TroopType.T_1).size() > 0;
    }

    boolean hasEnemyNeighbor(Place place)
    {
        if( place.building.troopType != TroopType.T_1 )
            return false;
        return place.getLinks(TroopType.T_0).size() > 0;
    }

    long estimateRushTime(Place fighter, Place target)
    {
        PlaceList path = PathFinder.find(fighter, target, allPlaces);
        if( path == null )
            return -1;

        int step = path.size() - 1;
        double ret = fighter.building.troopSpeed * PathFinder.getDistance(fighter, path.get(step)) + fighter.building.troopRushGap;
        while ( step > 0 )
        {
            //ext.trace("==>", path.get(step).index, path.get(step-1).index, path.get(step).building.get_troopSpeed(), PathFinder.getDistance(path.get(step), path.get(step - 1)) , path.get(step).building.get_exitGap());
            ret += path.get(step).building.troopSpeed * PathFinder.getDistance(path.get(step), path.get(step - 1)) + path.get(step).building.troopRushGap;
            step --;
        }
        return Math.round(ret);
    }

    double estimatePower(Building building, double troopsDivision) {
        return building.getPower() * troopsDivision;
    }
    float estimateImproveMode(PlaceList places)
    {
        int size = places.size();
        if( size == 0 )
            return  0;
        int sum = 0;
        int i = 0;
        while ( i < size )
        {
            sum += places.get(i).building.type == BuildingType.B01_CAMP ? 0 : places.get(i).building.improveLevel ;
            i ++;
        }
        return sum / size;
    }
    double estimateHealth(Place place) {
        return place.building.getPower() * (1.1 + battleField.difficulty * 0.05);
    }

    double priority(Place place) {
        return estimateHealth(place) + (place.building.troopType + 1) * 0.5 + ( lastTarget == place.index ? -0.5 : 0 );
    }
}