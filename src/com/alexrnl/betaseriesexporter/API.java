package com.alexrnl.betaseriesexporter;

/**
 * Class which defines the string used by the API.
 * @author Alex
 */
public final class API {

	/**
	 * Constructor #1.<br />
	 * Default private constructor to avoid creating an instance of the class anywhere.
	 */
	private API() {
	}

	/**
	 * The key for the api
	 */
	public static final String	KEY					= "4b4b65a6071d";

	/**
	 * The key parameters
	 */
	public static final String	KEY_PARAM			= "key";

	/**
	 * The user-agent used
	 */
	public static final String	USER_AGENT			= "BetaSeriesExporter";

	/**
	 * The parameter for the user-agent
	 */
	public static final String	USER_AGENT_PARAM	= "user-agent";

	/**
	 * The host of the service
	 */
	public static final String	HOST				= "https://api.betaseries.com";

	/**
	 * The method for retrieving the episodes of a member
	 */
	public static final String	MEMBER_EPISODES		= "members/episodes/all.xml";

	/**
	 * The method for login to the application
	 */
	public static final String	LOGIN_PAGE			= "members/auth.xml";

	/**
	 * The method for logging out
	 */
	public static final String	LOGOUT_PAGE			= "members/destroy.xml";

	/**
	 * The parameter for the login
	 */
	public static final String	LOGIN				= "login";

	/**
	 * The parameter for the password
	 */
	public static final String	PASSWORD			= "password";

	/**
	 * The parameter token
	 */
	public static final String	TOKEN				= "token";

	/**
	 * The tag code which indicate if everything was fine during the request.
	 */
	public static final String	CODE_OK				= "code";

	/**
	 * The tag errors
	 */
	public static final String	ERRORS				= "errors";

	/**
	 * The tag error
	 */
	public static final String	ERROR				= "error";

	/**
	 * The tag for the episode
	 */
	public static final String	EPISODE				= "episode";

	/**
	 * The tag for the show
	 */
	public static final String	SHOW				= "show";

	/**
	 * The tag for the episode number (SxxExx)
	 */
	public static final String	NUMBER				= "number";

	/**
	 * The tag for the global number of the episode
	 */
	public static final String	GLOBAL_NUMBER		= "global";

	/**
	 * The tag for the episode title
	 */
	public static final String	TITLE				= "title";

	/**
	 * The parameter view
	 */
	public static final String	VIEW				= "view";

	/**
	 * The value for the next episode only
	 */
	public static final String	NEXT				= "next";

	/**
	 * The tag name for the error content
	 */
	public static final String	ERROR_CONTENT		= "content";
}
