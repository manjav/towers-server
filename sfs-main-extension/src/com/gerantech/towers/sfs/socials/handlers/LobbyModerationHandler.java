package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

import static sun.audio.AudioPlayer.player;

/**
 * Created by ManJav on 8/28/2017.
 */
public class LobbyModerationHandler extends BaseClientRequestHandler
{
    private Game game;
    private Room lobby;
    private LobbyRoom roomClass;
    private ISFSArray all;
    private ISFSObject targetMember;
    private ISFSObject modMember;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        game = ((Game) sender.getSession().getProperty("core"));
        lobby = getParentExtension().getParentRoom();
        roomClass = (LobbyRoom) lobby.getExtension();

        all = lobby.getVariable("all").getSFSArrayValue();
        targetMember = null;
        modMember = null;
        int allSize = all.size();
        for (int i = 0; i < allSize; i++) {
            if( all.getSFSObject(i).getInt("id").equals(params.getInt("id")) )
                targetMember = all.getSFSObject(i);
            else if( all.getSFSObject(i).getInt("id").equals(game.player.id) )
                modMember = all.getSFSObject(i);
        }

        boolean succeed = false;
        if( targetMember == null || modMember == null ) {
            params.putBool("succeed", succeed);
            send(Commands.LOBBY_MODERATION, params, sender);
            return;
        }

        if( params.getShort("pr") == MessageTypes.M12_COMMENT_KICK )
            succeed = kick(params.getInt("id"), params.getUtfString("name"));
        else if( params.getShort("pr") == MessageTypes.M13_COMMENT_PROMOTE )
            succeed = promote(params.getInt("id"), params.getUtfString("name"));
        else if( params.getShort("pr") == MessageTypes.M14_COMMENT_DEMOTE )
            succeed = demote(params.getInt("id"), params.getUtfString("name"));

        params.putBool("succeed", succeed);
        send(Commands.LOBBY_MODERATION, params, sender);
    }

    private boolean kick(Integer targetId, String targetName)
    {
        if( modMember.getShort("pr") <= targetMember.getShort("pr") )
            return false;

        User targetUser = lobby.getUserByName(targetId.toString());
        if( targetUser != null )
            getApi().leaveRoom(targetUser, lobby, true, false);

        roomClass.sendComment((short) MessageTypes.M12_COMMENT_KICK, game.player.nickName, targetName, (short)-1);// mode = leave
        LobbyUtils.getInstance().removeUser(lobby, targetId);
        OneSignalUtils.send(getParentExtension(), targetName + " متأسفانه " + game.player.nickName + " تو رو از دهکده اخراج کرد.", null, targetId);
        return true;
    }

    private boolean promote(Integer targetId, String targetName)
    {
         if( modMember.getShort("pr") <= targetMember.getShort("pr")+1 || targetMember.getShort("pr") >= DefaultPermissionProfile.MODERATOR.getId() )
             return false;

        targetMember.putShort("pr", (short) (targetMember.getShort("pr")+1));
        LobbyUtils.getInstance().setMembersVariable(lobby, all);
        roomClass.sendComment((short) MessageTypes.M13_COMMENT_PROMOTE, game.player.nickName, targetName, targetMember.getShort("pr"));// mode = leave
        LobbyUtils.getInstance().save(lobby);
        OneSignalUtils.send(getParentExtension(), "تبریک " + targetName + "، تو توسط " + game.player.nickName + " ریش سپید شدی!", null, targetId);
        return true;
    }

    private boolean demote(Integer targetId, String targetName)
    {
        if( modMember.getShort("pr") <= targetMember.getShort("pr") || targetMember.getShort("pr") < DefaultPermissionProfile.MODERATOR.getId() )
            return false;

        targetMember.putShort("pr", (short) (targetMember.getShort("pr")-1));
        LobbyUtils.getInstance().setMembersVariable(lobby, all);
        roomClass.sendComment((short) MessageTypes.M14_COMMENT_DEMOTE, game.player.nickName, targetName, targetMember.getShort("pr"));// mode = leave
        LobbyUtils.getInstance().save(lobby);
        OneSignalUtils.send(getParentExtension(), targetName + "، " + game.player.nickName + " درجه تو رو به سرباز کاهش داد. ", null, targetId);
        return true;
    }
}