package com.gerantech.towers.sfs;
import com.gerantech.towers.sfs.handlers.AddReqHandler;
import com.gerantech.towers.sfs.handlers.BattleRequestsHandler;
import com.gerantech.towers.sfs.handlers.IABHandler;
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
      //  trace("Hello, this is my first SFS2X Extension!");
        
		// Add user login handler
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);

        // Add startBattle request handler
		addRequestHandler("startBattle", BattleRequestsHandler.class);
		
        // Add in app billing verification handler
		addRequestHandler("verify", IABHandler.class);
	    
		// Add add request handler
		addRequestHandler("add", AddReqHandler.class);
    }
}