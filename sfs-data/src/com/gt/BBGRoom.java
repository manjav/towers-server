package com.gt;

import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.core.ISFSEventParam;
import com.smartfoxserver.v2.core.SFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.managers.IUserManager;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.exceptions.SFSRoomException;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.util.IPlayerIdGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BBGRoom implements Room
{
    public static final int USER_TYPE_PLAYER = 0;
    public static final int USER_TYPE_SPECTATOR = 1;
    public static final int USER_TYPE_NPC = 2;

    private int id;
    private User owner;
    private List<User> users;
    private SFSExtension ext;
    private SmartFoxServer smartfox;
    private CreateRoomSettings settings;
    private Map<Integer, Integer> types;

    public BBGRoom() {
        super();
    }
    public void init(int id, CreateRoomSettings settings) {
        this.id = id;
        this.settings = settings;
        this.users = new ArrayList<>();
        this.types = new ConcurrentHashMap<>();
        this.smartfox = SmartFoxServer.getInstance();
        this.ext = (SFSExtension) smartfox.getZoneManager().getZoneByName("towers").getExtension();

    }
    protected void send(String cmdName, ISFSObject params, List<User> recipients) {
        ext.send(cmdName, params, recipients, false);
    }
    protected void send(String cmdName, ISFSObject params, User recipient) {
        ext.send(cmdName, params, recipient, false);
    }
    public void trace(Object... args) {
        ext.trace(args);
    }
    public void trace(ExtensionLogLevel level, Object... args) {
        ext.trace(level, args);
    }
    public Game getGame(User user)
    {
        return (Game) user.getSession().getProperty("core");
    }

    // property
    public boolean containsProperty(Object key) {
        return this.settings.getRoomProperties().containsKey(key);
    }
    public void setProperty(Object key, Object value) {
        this.settings.getRoomProperties().put(key, value);
    }
    public Object getProperty(Object key) {
        return this.settings.getRoomProperties().get(key);
    }
    public ConcurrentMap<Object, Object> getProperties() {
        return (ConcurrentMap<Object, Object>) this.settings.getRoomProperties();
    }
    public int getPropertyAsInt(Object key) {
        return (int) this.getProperty(key);
    }
    public boolean getPropertyAsBool(Object key) {
        return (boolean) this.getProperty(key);
    }
    public String getPropertyAsString(Object key) {
        return (String) this.getProperty(key);
    }
    public void removeProperty(Object key) {
        this.settings.getRoomProperties().remove(key);
    }

    public User getUserById(int i) {
        return null;
    }
    public int getId() {
        return this.id;
    }
    public String getGroupId() {
        return settings.getGroupId();
    }
    public void setGroupId(String s) {
        settings.setGroupId(s);
    }
    public String getName() {
        return settings.getName();
    }
    public void setName(String s) {
        settings.setName(s);
    }
    public String getPassword() {
        return settings.getPassword();
    }
    public void setPassword(String s) {
        settings.setPassword(s);
    }
    public boolean isPasswordProtected() {
        return false;
    }
    public boolean isPublic() {
        return false;
    }
    public int getCapacity() {
        return 0;
    }
    public void setCapacity(int i, int i1) {

    }
    public int getMaxUsers() {
        return settings.getMaxUsers();
    }
    public void setMaxUsers(int i) {
        settings.setMaxUsers(i);
    }
    public int getMaxSpectators() {
        return settings.getMaxSpectators();
    }
    public void setMaxSpectators(int i) {
        settings.setMaxSpectators(i);
    }
    public int getMaxRoomVariablesAllowed() {
        return settings.getMaxVariablesAllowed();
    }
    public void setMaxRoomVariablesAllowed(int i) {
        settings.setMaxVariablesAllowed(i);
    }
    public User getOwner() {
        return this.owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
    public RoomSize getSize() {
        return null;
    }
    public IUserManager getUserManager() {
        return null;
    }
    public void setUserManager(IUserManager iUserManager) {

    }
    public Zone getZone() {
        return ext.getParentZone();
    }
    public void setZone(Zone zone) {

    }
    public boolean isDynamic() {
        return settings.isDynamic();
    }
    public boolean isGame() {
        return settings.isGame();
    }
    public boolean isHidden() {
        return settings.isHidden();
    }
    public void setDynamic(boolean b) {
        settings.setDynamic(b);
    }
    public void setGame(boolean b) {
        settings.setGame(b);
    }
    public void setGame(boolean b, Class<? extends IPlayerIdGenerator> aClass) {
    }
    public void setHidden(boolean b) {
        settings.setHidden(b);
    }
    public void setFlags(Set<SFSRoomSettings> set) {
    }
    public void setFlag(SFSRoomSettings sfsRoomSettings, boolean b) {
    }
    public boolean isFlagSet(SFSRoomSettings sfsRoomSettings) {
        return false;
    }
    public SFSRoomRemoveMode getAutoRemoveMode() {
        return settings.getAutoRemoveMode();
    }
    public void setAutoRemoveMode(SFSRoomRemoveMode sfsRoomRemoveMode) {
        settings.setAutoRemoveMode(sfsRoomRemoveMode);
    }
    public boolean isEmpty() {
        return this.users.size() == 0;
    }
    public boolean isFull()
    {
        return this.users.size() >= settings.getMaxUsers();
    }
    public boolean isActive() {
        return false;
    }
    public void setActive(boolean b) {

    }
    public ISFSExtension getExtension() {
        return null;
    }
    public void setExtension(ISFSExtension isfsExtension) {

    }

    public RoomVariable getVariable(String s) {
        return null;
    }
    public List<RoomVariable> getVariables() {
        return null;
    }
    public void setVariable(RoomVariable roomVariable, boolean b) throws SFSVariableException {

    }
    public void setVariable(RoomVariable roomVariable) throws SFSVariableException {

    }
    public void setVariables(List<RoomVariable> list, boolean b) {
    }
    public void setVariables(List<RoomVariable> list) {

    }
    public List<RoomVariable> getVariablesCreatedByUser(User user) {
        return null;
    }
    public List<RoomVariable> removeVariablesCreatedByUser(User user) {
        return null;
    }
    public List<RoomVariable> removeVariablesCreatedByUser(User user, boolean b) {
        return null;
    }
    public void removeVariable(String s) {

    }
    public boolean containsVariable(String s) {
        return false;
    }
    public int getVariablesCount() {
        return 0;
    }

    public boolean isPlayer(User user){
        return types.containsKey(user.getId()) && types.get(user.getId()) == USER_TYPE_PLAYER;
    }
    public boolean isSpectator(User user) {
        return types.containsKey(user.getId()) && types.get(user.getId()) == USER_TYPE_SPECTATOR;
    }
    public boolean isNPC(User user) {
        return types.containsKey(user.getId()) && types.get(user.getId()) == USER_TYPE_NPC;
    }
    public int getUserType(User user) {
        return  this.types.get(user.getId());
    }
    public List<User> getUsersByType(int type) {
        List<User> ret = new ArrayList<>();
        for (int i=0; i < this.users.size(); i++)
            if( type == -1 || (this.types.containsKey(this.users.get(i).getId()) && this.types.get(this.users.get(i).getId()) == type) )
                ret.add(this.users.get(i));
        return ret;
    }
    public User getUserByName(String userName)    {
        for (int i=0; i < this.users.size(); i++)
            if( this.users.get(i).getName() == userName )
                return this.users.get(i);
        return null;
    }
    public User getUserBySession(ISession iSession) {
        return null;
    }
    public User getUserByPlayerId(int playerId) {
        for (int i=0; i < this.users.size(); i++)
            if( getGame(this.users.get(i)).player.id == playerId )
                return this.users.get(i);
        return null;
    }
    public List<User> getUserList() {
        return users;
    }
    public List<User> getPlayersList()
    {
        return getUsersByType(USER_TYPE_PLAYER);
    }
    public List<User> getSpectatorsList() {
        return getUsersByType(USER_TYPE_SPECTATOR);
    }
    public List<ISession> getSessionList() {
        return null;
    }
    public ISFSArray getUserListData() {
        return null;
    }
    public ISFSArray getRoomVariablesData(boolean b) {
        return null;
    }
    public void addUser(User user, boolean b) throws SFSJoinRoomException {
        addUser(user, USER_TYPE_PLAYER);
    }
    public void addUser(User user) throws SFSJoinRoomException {
        addUser(user, USER_TYPE_PLAYER);
    }
    public int addUser(User user, int type) {
        int index = this.users.indexOf(user);
        if( index > -1 )
        {
            trace(ExtensionLogLevel.ERROR, user.getName() + " is already joint in " + this.settings.getName() + ".");
        }
        else
        {
            index = users.size();
            this.users.add(user);
            this.types.put(user.getId(), type);

            Map<ISFSEventParam, Object> userParams = new HashMap<>();
            userParams.put(SFSEventParam.USER, user);
            userParams.put(SFSEventParam.ROOM, this);
            userParams.put(SFSEventParam.ZONE, getZone());
            smartfox.getEventManager().dispatchEvent(new SFSEvent(SFSEventType.USER_JOIN_ROOM, userParams));
        }
        return index;
    }
    public void removeUser(User user)
    {
        leave(user);
    }
    public int leave(User user) {
        if( this.users.size() == 0 )
        {
            trace(ExtensionLogLevel.ERROR, "Battle room " + this.settings.getName() + " is empty.");
            return MessageTypes.RESPONSE_NOT_FOUND;
        }

        if( !this.users.contains(user) )
        {
            trace(ExtensionLogLevel.ERROR, user.getName() + " is not exists in " + this.settings.getName() + ".");
            return MessageTypes.RESPONSE_NOT_FOUND;
        }

        this.users.remove(user);
        this.types.remove(user.getId());

        Map<ISFSEventParam, Object> userParams = new HashMap<>();
        userParams.put(SFSEventParam.ZONE, getZone());
        userParams.put(SFSEventParam.USER, user);
        userParams.put(SFSEventParam.ROOM, this);
        smartfox.getEventManager().dispatchEvent(new SFSEvent(SFSEventType.USER_LEAVE_ROOM, userParams));
        return MessageTypes.RESPONSE_SUCCEED;
    }
    public boolean containsUser(User user) {
        return users.contains(user);
    }
    public boolean containsUser(String s) {
        return false;
    }

    public ISFSArray toSFSArray(boolean b) {
        return null;
    }
    public void switchPlayerToSpectator(User user) throws SFSRoomException {

    }
    public void switchSpectatorToPlayer(User user) throws SFSRoomException {

    }
    public boolean isUseWordsFilter() {
        return false;
    }
    public void setUseWordsFilter(boolean b) {

    }
    public long getLifeTime() {
        return 0;
    }
    public String getDump() {
        return null;
    }
    public void destroy()    {
    }
    public String getPlayerIdGeneratorClassName() {
        return null;
    }
    public boolean isAllowOwnerInvitations() {
        return false;
    }
    public void setAllowOwnerInvitations(boolean b) {

    }
    public String toString() {
        return String.format("[ BBGRoom: %s, Id: %s, Group: %s, isGame: %s ]", new Object[] { this.getName(), this.getId(), this.getGroupId(), this.isGame() });
    }
}