package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Place;
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

import java.util.*;

/**
 * Created by ManJav on 1/25/2018.
 */
public class BattleBot{
	
    public int dangerousPoint = -1;
    public ISFSArray offenders;

    protected double targetHealth;
    protected double fightersPower;
    protected final SFSExtension extension;
    protected final BattleRoom battleRoom;
    protected final BattleField battleField;

    private int sampleTime;
    private int timeFactor;
    private int battleRatio = 0;
    private double lastStickerTime;
    private PlaceList allPlaces;
    private Timer chatTimer;
    private int lastTarget = -1;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ArrayList<Game> registeredPlayers = (ArrayList)battleRoom.getParentRoom().getProperty("registeredPlayers");
		
        extension = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
        timeFactor = Math.max(1, 8 - battleField.difficulty );
        extension.trace("p-point:" + registeredPlayers.get(0).player.resources.get(ResourceType.POINT), "b-point:"+ registeredPlayers.get(1).player.resources.get(ResourceType.POINT), " winStreak:" + registeredPlayers.get(0).player.resources.get(ResourceType.WIN_STREAK), "difficulty:" + battleField.difficulty, "timeFactor:" + timeFactor);

        allPlaces = battleField.getPlacesByTroopType(TroopType.NONE, true);
    }

    public void doAction()
    {
        int _sampleTime = (int) Math.floor(battleField.now % timeFactor);
        if( timeFactor == 1 || ( _sampleTime == 0 && _sampleTime != sampleTime ) )
            tryAction();

        sampleTime = _sampleTime;
    }

    void tryAction()
    {
        cover();

        /**
         * transform and fight troops to empty places
         */
        int step = allPlaces.size() - 1;
        int weakestPlace = -1;
        double mostPriority = 1000;
        List <Integer> samePriorities = new ArrayList();
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
	
    /**
     * cover for defense main places
     */
    void cover()
    {
        if( dangerousPoint < 0 )
            return;

        Place target = battleField.places.get(dangerousPoint);
        if( target.building.troopType != TroopType.T_1 )
            return;

        int step = offenders.size() - 1;
        double totalPowers = 0;
        while( step >= 0 )
        {
            totalPowers += battleField.places.get(offenders.getInt(step)).building.getPower();
            step --;
        }

        fightToPlace(target, totalPowers);
        dangerousPoint = -2;
    }
	
    void fightToPlace(Place target, double forceTargetHealth)
    {
        fightersPower = 0;
        targetHealth = forceTargetHealth > 0 ? forceTargetHealth : estimateHealth(target);
				
        // estimate powers of sides
        PlaceList playerPlaces = battleField.getPlacesByTroopType(TroopType.T_0, false);
        PlaceList robotPlaces = battleField.getPlacesByTroopType(TroopType.T_1, false);

		// end battle
        if( playerPlaces.size() == 0 || robotPlaces.size() == 0 )
            return;
		
        IntList fightersCandidates = new IntList();
        HashMap fighters = new HashMap();
        addFighters(target, fightersCandidates, fighters);

        extension.trace("target: " + target.index, "size: " + fighters.size(), " fightersPower: " + fightersPower, " targetHealth: " + targetHealth);
        if( fighters.size() > 0 && (fightersPower >= targetHealth || battleField.getPlacesByTroopType(TroopType.T_1, true).size() <= 1) )
            scheduleFighters(target, fighters);
		
         startChating(robotPlaces.size() - playerPlaces.size());
   }

    void addFighters(Place place, IntList fightersCandidates, HashMap fighters)
    {
        if( fightersCandidates.indexOf(place.index) > -1 || (fightersPower >= targetHealth && battleField.now < battleField.getTime(1)) )
            return;
        fightersCandidates.push(place.index);

        if( place.building.troopType == TroopType.T_1 && !fighters.containsKey(place.index) && place.fightTime == -1 )
        {
            Place card = battleField.deckBuildings.get(4);
            double placePower = place.building.getPower();
            double cardPower = estimateCardPower(card);
            //extension.trace(place.index, "placePower", placePower, "cardPower", cardPower, fightersPower);

            if( placePower >= cardPower )
            {
                fightersPower += placePower;
                fighters.put(place.index, new ScheduledPlace(place));
            }
            else if( place.building.transform(card.building) )
            {
                fightersPower += cardPower;
                fighters.put(place.index, new ScheduledPlace(place));
            }
        }

        // transform neighbors
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while (step >= 0)
        {
            addFighters(placeLinks.get(step), fightersCandidates, fighters);
            step --;
        }
    }

    void scheduleFighters(Place target, HashMap fighters)
    {
        lastTarget = target.index;
        dangerousPoint = -1;
        long maxDelay = 0;
		
		// estimate max time distance
        Iterator<Map.Entry<Integer, ScheduledPlace>> iterator = fighters.entrySet().iterator();
        while (iterator.hasNext())
        {
            ScheduledPlace sPlace = iterator.next().getValue();
            sPlace.place.fightTime = estimateRushTime(sPlace.place, target);
            if( maxDelay < sPlace.place.fightTime )
                maxDelay = sPlace.place.fightTime;
        }

        // shoot every places in determined times
        iterator = fighters.entrySet().iterator();
        while (iterator.hasNext())
        {
            ScheduledPlace sPlace = iterator.next().getValue();
           // extension.trace("fight", sPlace.place.index + " -> target: " + target.index, "delay:", maxDelay, sPlace.fightTime, maxDelay - sPlace.fightTime + sPlace.place.building.deployTime);
            sPlace.timer = new Timer();
            sPlace.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if( sPlace.place.building.troopType == TroopType.T_1 && sPlace.place.fightTime > 0 )
                    {
                        SFSArray _fighters = new SFSArray();
                        _fighters.addInt(sPlace.place.index);
                        battleRoom.fight(_fighters, target.index, true);
                    }
                    sPlace.dispose();
                    fighters.remove(sPlace.place.index);
                    //extension.trace("remove", sPlace.place.index, "fighters.size: " + fighters.size());
                }
            }, maxDelay - sPlace.place.fightTime + sPlace.place.building.deployTime);
        }
    }

    private void startChating(int battleRatio)
    {
        if( battleField.map.isQuest )
            return;

        //extension.trace(this.battleRatio, battleRatio);
        if( chatTimer == null && battleRatio != this.battleRatio && battleField.now - lastStickerTime > 10 && Math.random() < 0.2 && battleField.now > battleField.startAt + 30 )
        {
            SFSObject stickerParams = new SFSObject();
            stickerParams.putInt("t", StickerType.getRandomStart(battleRatio));
            stickerParams.putInt("tt", 1);
            chatTimer = new Timer();
            chatTimer.schedule(new TimerTask() {
                @Override
                public void run()
                {
                    battleRoom.sendSticker(null, stickerParams);
                    lastStickerTime = battleField.now;
                    chatTimer.cancel();
                    chatTimer = null;
                }
            }, (long) (Math.random() * 1000 + 500));
        }
        this.battleRatio = battleRatio;
    }

    public void answerChat(ISFSObject params)
    {
        if( chatTimer == null && Math.random() > 0.5 )
        {
            int answer = StickerType.getRandomAnswer( params.getInt("t") );
            if( answer > -1 )
            {
                ISFSObject stickerParams = new SFSObject();
                stickerParams.putInt("t", answer);
                stickerParams.putInt("tt", 1);
                stickerParams.putInt("wait", 0);

                chatTimer = new Timer();
                chatTimer.schedule(new TimerTask() {
                    @Override
                    public void run()
                    {
                        battleRoom.sendSticker(null, stickerParams);
                        chatTimer.cancel();
                        chatTimer = null;
                    }
                }, (long) (Math.random() * 2500 + 1500));
            }
        }
    }

    // tools
    boolean isNeighbor(Place place)
    {
        if( place.building.troopType == TroopType.T_1 )
            return false;
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        return placeLinks.size() > 0;
    }

    int estimateRushTime(Place fighter, Place target)
    {
        PlaceList path = PathFinder.find(fighter, target, allPlaces);
        if( path == null )
            return -1;

        int step = path.size() - 1;
        double ret = fighter.building.troopSpeed * PathFinder.getDistance(fighter, path.get(step)) + fighter.building.troopRushGap;
        while ( step > 0 )
        {
            //extension.trace("==>", path.get(step).index, path.get(step-1).index, path.get(step).building.get_troopSpeed(), PathFinder.getDistance(path.get(step), path.get(step - 1)) , path.get(step).building.get_exitGap());
            ret += path.get(step).building.troopSpeed * PathFinder.getDistance(path.get(step), path.get(step - 1)) + path.get(step).building.troopRushGap;
            step --;
        }
        return (int) Math.round(ret);
    }

    double estimateCardPower(Place place) {
        return place.building.troopsCount * place.building.troopPower;
    }

    double estimateHealth(Place place) {
        return (place.building.get_population() + place.building.get_health());
    }

    double priority(Place place)
    {
        //if (robotCastle != null && place.getLinks(TroopType.T_1).indexOf(robotCastle) > -1)
        //    return 0.1f;
        if( battleField.now > battleField.getTime(2) && place.mode > 0 )
            return 0.1f;
        return estimateHealth(place) / (place.mode + 1);
    }
}