package com.alexrnl.betaseriesexporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The main class of the project.<br />
 * Allow a member of BetaSeries.com to log in and export its episodes list.
 * 
 * @author Alex
 */
public final class Launcher {
	private static Logger		lg					= Logger.getLogger(Launcher.class.getName());

	private static final String	CONFIGURATION_FILE	= "conf/configuration.xml";
	private static final String DEFAULT_DATE_FORMAT = "EEEE d MMMM yyyy à HH'h'mm";

	private static QueryManager	api					= null;
	private static Properties	configuration		= null;
	private static String		newLine				= System.lineSeparator();
	
	/**
	 * Constructor #1.<br />
	 * Default constructor declared to avoid creating an instance of the class anywhere.
	 */
	private Launcher() {
	}
	
	/**
	 * Launcher of the application.
	 * @param args the arguments from the command line.
	 */
	public static void main (final String args[]) {
		lg.info("Starting program");
		// Loading configuration
		configuration = new Properties();
		try {
			configuration.loadFromXML(new FileInputStream(CONFIGURATION_FILE));
		} catch (final IOException e) {
			lg.severe("Could not load configuration (" + e.getMessage() + ")");
			JOptionPane.showMessageDialog(null, "Le fichier de configuration " + CONFIGURATION_FILE +
					" n'a pas pu être chargé.", "Fichier de configuration", JOptionPane.ERROR_MESSAGE);
		}
		
		if (!configuration.isEmpty())
			setLookAndFeel();
		
		// Building the query manager
		final Map<String, String> compulsoryParams = new HashMap<>();
		compulsoryParams.put(API.KEY_PARAM, API.KEY);
		compulsoryParams.put(API.USER_AGENT_PARAM, API.USER_AGENT);
		api = new QueryManager(API.HOST, compulsoryParams);

		// Retrieve the token for the user
		final String token = LoginForm.getToken(api, configuration.getProperty("icon"));
		if (token == null || token.isEmpty()) {
			JOptionPane.showMessageDialog(null,
					"Vous devez être connecté à BetaSeries pour utiliser" + "cette application",
					"Erreur", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// The request for the episodes
		final Map<String, String> params = new HashMap<>();
		params.put(API.VIEW, API.NEXT);
		params.put(API.TOKEN, token);
		final Document doc = api.execute(API.MEMBER_EPISODES, params);

		if (doc == null || QueryManager.hasError(doc)) {
			final String errorMessage = QueryManager.getTextValue((Element) doc.getElementsByTagName(API.ERRORS)
					.item(0), API.ERROR_CONTENT);
			JOptionPane.showMessageDialog(null, errorMessage, "Erreur de communication",
					JOptionPane.ERROR_MESSAGE);
			logout(token);
			return;
		}

		// Building the list of episodes from the documents
		final List<String> nextEpisodes = createListEpisodes(doc);

		// Generating the file
		final StringBuilder output = new StringBuilder("Prochains épisodes à regarder:");
		for (final String episode : nextEpisodes) {
			output.append(newLine + "\t" + episode);
		}
		// Append the date
		final String dateFormat = configuration.isEmpty() ? DEFAULT_DATE_FORMAT : configuration.getProperty("dateFormat");
		output.append(newLine + "Mis à jour le " + new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime()));
		final String episodesOutput = output.toString();
		lg.fine(episodesOutput);
		
		if (createFile(episodesOutput)) {
			JOptionPane.showMessageDialog(null, "Liste des épisodes exportés avec succès.", "Export réussi", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null, "Echec lors de l'écriture du fichier, consultez les logs ('%TEMP%\\java_bsexporter.log') " +
					"pour plus de détails.",
					"Export échoué", JOptionPane.ERROR_MESSAGE);
		}
		
		logout(token);
		lg.info("Exiting program.");
		
	}

	/**
	 * Sets the look and feel of the application
	 */
	private static void setLookAndFeel () {
		final String lookAndFeelName = configuration.getProperty("lookAndFeel");
		boolean lookAndFeelFound = false;
		for (final LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
			lg.fine(laf.getName());
			if (laf.getName().equals(lookAndFeelName)) {
				try {
					UIManager.setLookAndFeel(laf.getClassName());
					lookAndFeelFound = true;
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException e) {
					lg.warning("Could not set the look and feel " + laf.getName());
					lookAndFeelFound = false;
				}
			}
		}
		if (!lookAndFeelFound) {
			lg.warning("Could not find (or set) the look and feel " + lookAndFeelName
					+ ". Using default look and feel.");
		}
	}

	/**
	 * Logout the current token for the API.
	 * @param token the token to destroy.
	 */
	private static void logout (final String token) {
		// Preparing the parameters for logging out
		final Map<String, String> paramLogout = new HashMap<>();
		paramLogout.put(API.TOKEN, token);
	
		final Document doc = api.execute(API.LOGOUT_PAGE, paramLogout);
		if (QueryManager.hasError(doc))
			lg.warning("Failed do destroy connection token properly.");
	}

	/**
	 * Create a list with the list of episodes from a document.
	 * @param doc the document to parse.
	 * @return the list with the episodes information, nicely formatted.
	 */
	private static List<String> createListEpisodes (final Document doc) {
		final List<String> nextEpisodes = new ArrayList<>();
		final NodeList episodes = doc.getElementsByTagName(API.EPISODE);
		
		for (int currentEpisodeNb = 0; currentEpisodeNb < episodes.getLength(); ++currentEpisodeNb) {
			final Element currentEpisode = (Element) episodes.item(currentEpisodeNb);
			// Checking if the node is a 'real episode'
			if (currentEpisode == null || !currentEpisode.hasChildNodes()
					|| currentEpisode.getFirstChild().getNextSibling() == null)
				continue;
			
			// Retrieving values
			final String show = QueryManager.getTextValue(currentEpisode, API.SHOW);
			final String episodeNumber = QueryManager.getTextValue(currentEpisode, API.NUMBER);
			final String globalNumber = QueryManager.getTextValue(currentEpisode, API.GLOBAL_NUMBER);
			final String episodeName = QueryManager.getTextValue(currentEpisode, API.TITLE);
			
			// Building string
			final String display = show + " #" + globalNumber + " " + episodeNumber + " - "
					+ episodeName;
			lg.fine(display);
			nextEpisodes.add(display);
		}
		return nextEpisodes;
	}

	/**
	 * Creates the file with the content of the string.<br />
	 * The name of the file is configurable through the configuration file.
	 * @param content the content to write.
	 */
	private static boolean createFile (final String content) {
		boolean success = false;
		FileOutputStream fos = null;
		FileChannel channel = null;
		String fileName = null;
		
		try {
			fileName = configuration.getProperty("outputFile");
			if (fileName == null)
				fileName = JOptionPane.showInputDialog(null, "Fichier de configuration non chargé, veuillez spécifier le nom du fichier d'export :",
						"Fichier d'export", JOptionPane.QUESTION_MESSAGE);
			if (fileName == null)
				return false;
			fos = new FileOutputStream(new File(fileName));
			channel = fos.getChannel();
			final ByteBuffer buffer = ByteBuffer.allocate(content.length()*2);
			buffer.asCharBuffer().put(content);
			final CharsetEncoder cse = Charset.defaultCharset().newEncoder();
			final int nbWrite = channel.write(cse.encode(buffer.asCharBuffer()));
			if (nbWrite == content.length()) {
				lg.info("Wrote the all the data in the file successfully.");
				success = true;
			} else {
				lg.severe("Fail to write all the data: wrote=" + nbWrite + "; expected=" + content.length());
			}
			
		} catch (final IOException e) {
			lg.severe("Cannot write to file (" + e.getMessage() + ")");
			JOptionPane.showMessageDialog(null, "Écriture dans le fichier " + fileName + "impossible." +
					newLine + "Cause : " + e.getMessage(), "Erreur d'écriture", JOptionPane.ERROR_MESSAGE);
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (final IOException e) {
					lg.warning("Cannot close file (" + e.getMessage() + ")");
				}
		}
		return success;
	}
}
