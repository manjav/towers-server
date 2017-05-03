import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class AddReqHandler extends BaseClientRequestHandler 
{

	public AddReqHandler() {}

	/* (non-Javadoc)
	 * @see com.smartfoxserver.v2.extensions.IClientRequestHandler#handleClientRequest(com.smartfoxserver.v2.entities.User, com.smartfoxserver.v2.entities.data.ISFSObject)
	 */
	public void handleClientRequest(User sender, ISFSObject params)
    {
        // Get the client parameters
        int n1 = params.getInt("n1");
        int n2 = params.getInt("n2");
         
        // Create a response object
        ISFSObject resObj = SFSObject.newInstance(); 
        resObj.putInt("res", n1 + n2);
         
        // Send it back
        send("add", resObj, sender);
    }
}