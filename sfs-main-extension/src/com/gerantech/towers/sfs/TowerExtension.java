package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.AddReqHandler;
import com.gerantech.towers.sfs.handlers.BattleAutoJoinHandler;
import com.gerantech.towers.sfs.handlers.CafeBazaarVerificationHandler;
import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
/**
 * @author ManJav
 *
 */
public class TowerExtension extends SFSExtension
{
    public void init()
    {
        //trace("Hello, this is my first SFS2X Extension!", Game.loginData.coreVersion);
        
		// Add user login handler
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);

        // Add startBattle request handler
		addRequestHandler("startBattle", BattleAutoJoinHandler.class);
		
        // Add in app billing verification handler
		addRequestHandler("verify", CafeBazaarVerificationHandler.class);
	    
		// Add add request handler
		addRequestHandler("add", AddReqHandler.class);
    }
}