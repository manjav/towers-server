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

        // Add joinMe request handler
		addRequestHandler("fight", AutoJoinerHandler.class);
	    
		// Add add request handler
		addRequestHandler("add", AddReqHandler.class);
    }
}