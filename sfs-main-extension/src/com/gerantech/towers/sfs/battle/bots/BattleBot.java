package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Building;
import com.gt.towers.buildings.Place;
import com.gt.towers.constants.BuildingType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.constants.TroopType;
import com.gt.towers.utils.PathFinder;
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

    // bot behaviours
    int favoriteBuilding;// type of improvement
    boolean coveringBehaviour;
    boolean waitForPowerfull;// bot will moved to waiting for improvement and powreful

    int sampleTime;

    int timeFactor;
    int battleRatio = 0;
    int lastTarget = -1;
    int repeatetiveTarget = -1;
    float placeRatio = 1;
    double troopsDivision;
    PlaceList allPlaces;
    SFSObject chatPatams;
    PlaceList robotPlaces;
    PlaceList playerPlaces;
    ConcurrentHashMap<Integer, ScheduledPlace> sceduledfighters;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
        timeFactor = Math.min(8, Math.max(2, 10 - battleField.difficulty ) );
        troopsDivision = Math.max(0.3, Math.min(0.9, battleField.difficulty * 0.4));
        if( battleField.games.get(0).player.tutorialMode == 1 && battleField.games.get(0).player.get_battleswins() < 2 )
            troopsDivision = 0.15;

        allPlaces = battleField.getPlacesByTroopType(TroopType.NONE, true);

        chatPatams = new SFSObject();
        chatPatams.putLong("ready", battleField.now + 15000);

        // covering behaviour
        double random = Math.random();
        coveringBehaviour = random > 0.2;

        // favorite building
        random = Math.random();
        if( random > 0.8 )
            favoriteBuilding = 31;
        else if( random > 0.4 )
            favoriteBuilding = 21;
        else
            favoriteBuilding = 11;

        // stop attack if bot needs to improvement
        random = Math.random();
        waitForPowerfull = random > 0.3;

        ext.trace("p-point:" + battleField.games.get(0).player.resources.get(ResourceType.POINT), "b-point:"+ battleField.games.get(1).player.resources.get(ResourceType.POINT), " coveringBehaviour:", coveringBehaviour, " favoriteBuilding:", favoriteBuilding, " waitForPowerfull:", waitForPowerfull, " winStreak:" + battleField.games.get(0).player.resources.get(ResourceType.WIN_STREAK), "difficulty:" + battleField.difficulty, "timeFactor:" + timeFactor, "troopsDivision:" + troopsDivision);
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
        if( !coveringBehaviour || battleField.difficulty < 5 || coverPoint < 0 )
            return;

        Place target = battleField.places.get(coverPoint);
        if( battleField.getPlacesByTroopType(TroopType.T_1, false).size() < 3 )
        {
            coverPoint = -1;
            return;
        }

        /*double[] destinations = new double[allPlaces.size()];
        for ( Map.Entry<Object, Troop> e : battleField.troops.entrySet() )
        {
            Troop t = e.getValue();
            if( !t.disposed )
                destinations[ t.destination.index ] += (t.type == TroopType.T_0 ? -t.health : t.health);
        }

        int step = destinations.length - 1;
        int criticalPlace = -1;
        double criticalState = 0;
        while( step >= 0 )
        {
            if( destinations[step] < criticalState )// && isNeighbor(allPlaces.get(step), false) )
            {
                criticalState = destinations[step];
                criticalPlace = step;
            }
            step --;
        }
        if( criticalPlace > -1 && isNeighbor(allPlaces.get(step), false) )
        {
            ext.trace("cp", criticalPlace, "  =>", criticalState);

          if(  criticalState < 0 )
                fightToPlace(allPlaces.get(criticalPlace), -criticalState, 200);
        }*/

        // only neighbor places covered ...
        if( !isNeighbor(target, false) )
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

        if( totalPowers < target.building.getPower() * 0.8 )
        {
            coverPoint = -1;
            return;
        }
        fightToPlace(target, totalPowers, target.building.troopType == TroopType.T_1 ? 500 : 3500);//ext.trace(target.index, "covered by", totalPowers);
    }

    /**
     * find and fight weakest index
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
            if( isNeighbor(allPlaces.get(step), true) )
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
        // random index
        if( samePriorities.size() > 0 )
            weakestPlace = samePriorities.get((int) Math.floor(Math.random() * samePriorities.size()));
        if( weakestPlace > -1 )
            fightToPlace(allPlaces.get(weakestPlace), 0, 0);
    }

    void fightToPlace(Place target, double forceTargetHealth, long delay)
    {
        if( repeatetiveTarget % 100 == target.index )
            repeatetiveTarget += 100;
        else
            repeatetiveTarget = target.index;

        fightersPower = 0;
        targetHealth = forceTargetHealth > 0 ? forceTargetHealth : target.building.getPower();

        // estimate powers of sides
        playerPlaces = battleField.getPlacesByTroopType(TroopType.T_0, false);
        robotPlaces = battleField.getPlacesByTroopType(TroopType.T_1, false);

        // end battle
        if( playerPlaces.size() == 0 || robotPlaces.size() == 0 )
            return;

        placeRatio = (float)playerPlaces.size() / (float)robotPlaces.size();
        float improveRatio = estimateImproveMode(playerPlaces) / estimateImproveMode(robotPlaces);

        // stop fighting if needs to improvement
        int improveIndex = -2;
        if( forceTargetHealth == 0 && robotPlaces.size() > 1 && playerPlaces.size() > 1 && improveRatio >= 1 )
        {
            improveIndex = improveAll(robotPlaces, true);
            if( waitForPowerfull && improveIndex > -1 )
                return;
        }

        List<Integer> findingPath = new ArrayList<>();
        List<Integer> candidatedfighters = new ArrayList();
        if( sceduledfighters == null )
            sceduledfighters = new ConcurrentHashMap();

        addFighters(target, findingPath, candidatedfighters, forceTargetHealth);
        findingPath.clear();

        // if( battleField.games.get(0).player.admin )
        // ext.trace("target:" + target.index, " target_type:" + target.building.type, " numFighters:" + candidatedfighters.size(), " fightersPower:" + fightersPower, " targetHealth:" + targetHealth );
        boolean firstToFight = candidatedfighters.size() > 0 && ((fightersPower / targetHealth > 1.2) || forceTargetHealth > 0 || robotPlaces.size() < 2 || playerPlaces.size() < 2 );
        if( !firstToFight && repeatetiveTarget > 1000 )
            return;

        if( !firstToFight && improveIndex == -1 && playerPlaces.size() < 3 )
            firstToFight = true;

        if( firstToFight )
            scheduleFighters(target, candidatedfighters, forceTargetHealth, delay);
        else if( improveIndex == -2 )
            improveAll(robotPlaces, true);// improve if bot have been frozen.

        //ext.trace("target:" + target.index, "covered with " + forceTargetHealth, "numFighters:" + sceduledfighters.size(), forceTargetHealth, "improveRatio", improveRatio, "improveIndex", improveIndex);
        chatStarting(robotPlaces.size() - playerPlaces.size());
    }

    void addFighters(Place place, List<Integer> findingPath, List<Integer> candidatedfighters, double forceTargetHealth)
    {
        if( findingPath.indexOf(place.index) > -1 || (fightersPower >= targetHealth) )
            return;
        findingPath.add(place.index);
        //ext.trace( index.building.troopType, sceduledfighters.containsKey(index.index) , index.index , coverPoint);

        if( place.building.troopType == TroopType.T_1 && (placeRatio < 0.4 || !sceduledfighters.containsKey(place.index)) && place.index != coverPoint && (forceTargetHealth <= 0 || !hasEnemyNeighbor(place)) )
        {
            double placePower = estimatePower(place.building, troopsDivision);
            //ext.trace(index.index, "placePower", placePower, "fightersPower", fightersPower, "troopsDivision", troopsDivision);
            fightersPower += placePower;
            candidatedfighters.add(place.index);
        }

        // transform neighbors
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while (step >= 0)
        {
            addFighters(placeLinks.get(step), findingPath, candidatedfighters, forceTargetHealth);
            step --;
        }
    }

    void scheduleFighters(Place target, List<Integer> candidatedfighters, double forceTargetHealth, long delay)
    {
        lastTarget = target.index;
        Iterator<Integer> iterator;
        long maxDelay = delay + 0;

        // estimate max time distance
        if( forceTargetHealth == 0 )
        {
            iterator = candidatedfighters.iterator();
            while ( iterator.hasNext() )
                maxDelay = Math.max(estimateRushTime(battleField.places.get(iterator.next()), target), maxDelay);
            maxDelay += battleField.interval + 1;
        }

        iterator = candidatedfighters.iterator();
        while ( iterator.hasNext() )
        {
            Integer p = iterator.next();
            //ext.trace(p, battleField.now, battleField.now + maxDelay - estimateRushTime(battleField.places.get(p), target));
            sceduledfighters.put(p, new ScheduledPlace(p, target.index,battleField.now + maxDelay - estimateRushTime(battleField.places.get(p), target)));
        }
        candidatedfighters.clear();
    }

    void updateFightingProcess()
    {
        if( sceduledfighters == null || sceduledfighters.size() == 0 )
        {
            if( coverPoint == -2 )
                coverPoint = -1;
            return;
        }

        // shoot every places in determined times
        Iterator<Map.Entry<Integer, ScheduledPlace>> iterator = sceduledfighters.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer, ScheduledPlace> entry = iterator.next();
            ScheduledPlace sPlace = entry.getValue();
            Place p = battleField.places.get(sPlace.index);
            //ext.trace("index", sPlace.index, "target", sPlace.target, "troopType", p.building.troopType, "fightTime", sPlace.fightTime, "now", battleField.now);
            if( p == null )
                continue;
            if( p.building.troopType == TroopType.T_1 )
            {
                if( battleField.now > sPlace.fightTime )
                {
                    SFSArray _fighters = new SFSArray();
                    _fighters.addInt(sPlace.index);
                    battleRoom.fight(_fighters, sPlace.target, true, troopsDivision);
                    sceduledfighters.remove(sPlace.index);
                }
            }
            else
            {
                sceduledfighters.remove(sPlace.index);
            }
        }
    }

    int improveAll(PlaceList places, boolean oneImprove)
    {
        int step = places.size() - 1;
        int ret = -1;
        while ( step >= 0 )
        {
            ret = improve(places.get(step), false);
            if( ret > -1 && ret < 100 && oneImprove )
                return ret;
            step --;
        }
        return ret;
    }
    int improve(Place place, boolean needPopulation)
    {
        if( battleField.difficulty < 2 )
            return -1;

        // if( index.fightTime > -1 )
        // {
        //  ext.trace(index.index + " exists in fighters...");
        //return false;
        // }
        int improveType = 0;
        if( place.building.type == BuildingType.B01_CAMP )
        {
            if( place.getLinks(TroopType.T_0).size() > 0 )
            {
                if( robotPlaces.size() <= playerPlaces.size() + 2 )
                    improveType = BuildingType.B41_CRYSTAL;
                else
                    improveType = favoriteBuilding;
            }
            else
            {
                improveType = favoriteBuilding;
            }
        }
        else
        {
            improveType = place.building.type + 1;
        }

        int ret = -1;
        if( place.building.improve(improveType%10>1?BuildingType.IMPROVE:improveType) )
            ret = place.index;
        else if( battleField.games.get(1).player.buildings.exists(improveType) )
            ret = place.index + 100;
        return ret;
    }


    void chatStarting(int battleRatio)
    {
        if( battleField.map.isQuest || battleField.games.get(0).player.inTutorial() )
            return;

        // verbose bot threshold
        if( chatPatams.getLong("ready") > battleField.now || Math.random() > 0.1 )
            return;

        //ext.trace(this.battleRatio, battleRatio);
        if( battleRatio != this.battleRatio )
        {
            chatPatams.putInt("t", StickerType.getRandomStart(battleRatio, battleField.games.get(0)));
            chatPatams.putInt("tt", 1);
            chatPatams.putLong("ready", (long) (battleField.now + Math.random() * 2500 + 500));
        }
        this.battleRatio = battleRatio;
    }

    public void chatAnswering(ISFSObject params)
    {
        if( chatPatams.getLong("ready") > battleField.now || Math.random() < 0.2 )
            return;

        int answer = StickerType.getAnswer( params.getInt("t") );
        if( answer <= -1 )
            return;

        chatPatams.putInt("t", answer);
        chatPatams.putInt("tt", 1);
        chatPatams.putInt("wait", 0);
        chatPatams.putLong("ready", (long) (battleField.now + Math.random() * 2500 + 2500));
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
    boolean isNeighbor(Place place, boolean excludeBot)
    {
        if( excludeBot && place.building.troopType == TroopType.T_1 )
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
        double ret = (32000 / path.get(step).building.troopSpeed) * PathFinder.getDistance(fighter, path.get(step)) + fighter.building.troopRushGap;
        while ( step > 0 )
        {
            //ext.trace("==>", path.get(step).index, path.get(step-1).index, path.get(step).building.get_troopSpeed(), PathFinder.getDistance(path.get(step), path.get(step - 1)) , path.get(step).building.get_exitGap());
            ret += (32000 / path.get(step).building.troopSpeed) * PathFinder.getDistance(path.get(step), path.get(step - 1)) + path.get(step).building.troopRushGap;
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
        float sum = 0;
        int i = 0;
        while ( i < size )
        {
            sum += places.get(i).building.type == BuildingType.B01_CAMP ? 0 : places.get(i).building.improveLevel;
            i ++;
        }
        return sum / size;
    }
   /* double estimateHealth(Place index) {
        return index.building.getPower() * (1.1 + battleField.difficulty * 0.05);
    }*/

    double priority(Place place) {
        return place.building.getPower() + (place.building.troopType + 1) * 0.5;
    }
}