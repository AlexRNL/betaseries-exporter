package com.alexrnl.betaseriesexporter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class in charge of the communication between the application and the API. <br />
 * Allow the definition of default parameter that will be sent with each query to the host.
 * 
 * @author Alex
 */
public class QueryManager {
	private static Logger				lg	= Logger.getLogger(QueryManager.class.getName());

	private final Map<String, String>	compulsoryParams;
	private String						host;

	/**
	 * Constructor #1.<br />
	 * Build a query manager with default parameters which will be sent along with each request.
	 * 
	 * @param host
	 *            the host server
	 * @param compulsoryParams
	 *            the parameters that should be sent with each request
	 */
	public QueryManager(final String host, final Map<String, String> compulsoryParams) {
		this.host = host;
		this.compulsoryParams = compulsoryParams;

		if (!this.host.startsWith("http")) {
			this.host = "http://" + this.host;
			lg.warning("Host does not starts with 'http' adding protocol to host.");
		}
	}

	/**
	 * Constructor #2.<br />
	 * Build a query manager with <b>no</b> default parameters.
	 * 
	 * @param host
	 *            the host server
	 * @see #QueryManager(String, Map)
	 */
	public QueryManager(final String host) {
		this(host, new HashMap<String, String>());
	}

	/**
	 * Add a compulsory parameter.<br />
	 * Overwrites the previous parameter if it is already present.
	 * 
	 * @param parameter
	 *            the name of the parameter.
	 * @param value
	 *            the value of the parameter.
	 * @throws IllegalArgumentException
	 *             if <code>parameter</code> or <code>value</code> is <code>null</code>.
	 */
	public void addParameter (final String parameter, final String value) {
		if (parameter == null || value == null) {
			throw new IllegalArgumentException("Cannot add a null parameter (or value) to"
					+ "the default parameters.");
		}
		compulsoryParams.put(parameter, value);
	}

	/**
	 * Execute the request for the given page, without any parameters (apart from the compulsory
	 * parameters defined in the {@link #QueryManager(String, Map) constructor}).
	 * 
	 * @param page
	 *            the page to query
	 * @return the XML document returned by the API
	 */
	public Document execute (final String page) {
		return execute(page, new HashMap<String, String>());
	}

	/**
	 * Execute the request for the given page with the <code>params</code> sent.
	 * @param page
	 *            the page to query.
	 * @param params
	 *            the parameters to transmit to the page.
	 * @return the XML document returned by the API.
	 */
	public Document execute (final String page, final Map<String, String> params) {
		params.putAll(compulsoryParams);

		final String url = host + "/" + page + (page.endsWith("xml") ? "" : ".xml") + "?"
				+ formatParamForRequest(params);
		lg.info("formatted url request: " + url);

		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
		} catch (final ParserConfigurationException e) {
			JOptionPane.showMessageDialog(null, "La connection à l'API a échoué.\nCause : " + e.getMessage(),
					"Erreur de communication", JOptionPane.ERROR_MESSAGE);
		} catch (final SAXException e) {
			JOptionPane.showMessageDialog(null, "La connection à l'API a échoué.\nCause : " + e.getMessage(),
					"Erreur de communication", JOptionPane.ERROR_MESSAGE);
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "La connection à l'API a échoué.\nCause : " + e.getMessage(),
					"Erreur de communication", JOptionPane.ERROR_MESSAGE);
		}
		return doc;
	}

	/**
	 * Format the parameters for a request to the API.<br />
	 * Formatting: <code>param1=value1&amp;param2=value2&amp;<i>[...]</i>&amp;paramN=valueN</code>
	 * 
	 * @param params
	 *            the parameters.
	 * @return a formatted string with the parameters.
	 */
	private static String formatParamForRequest (final Map<String, String> params) {
		if (params == null || params.isEmpty()) {
			throw new IllegalArgumentException("Cannot format parameters: map is null or empty.");
		}

		String parameters = "";
		for (final Entry<String, String> currentEntry : params.entrySet()) {
			parameters += currentEntry.getKey() + "=" + currentEntry.getValue() + "&";
		}
		if (!parameters.isEmpty() && parameters.endsWith("&")) {
			return parameters.substring(0, parameters.length() - 1);
		} else {
			return parameters;
		}
	}


	/**
	 * Check if the request has been correctly executed.
	 * @param doc the document
	 * @return <code>true</code> if the query to the API has not been successful.
	 */
	public static boolean hasError (final Document doc) {
		lg.fine("Checking document for errors");
		Node node = doc.getFirstChild().getFirstChild();

		while (node != null) {
			if (node.getNodeName().equals(API.CODE_OK)) {
				lg.fine("'" + API.CODE_OK + "' node found.");
				if (!node.getTextContent().equals("1")) {
					lg.info("'" + API.CODE_OK + "' was wrong: " + node.getTextContent());
					return true;
				}
			}
			if (node.getNodeName().equals(API.ERRORS)) {
				lg.fine("'" + API.ERRORS + "' node found.");
				Node errors = node.getFirstChild();
				while (errors != null) {
					if (errors.getNodeName().equals(API.ERROR)) {
						lg.info("'" + API.ERROR + "' exists...");
						return true;
					}
					errors = errors.getNextSibling();
				}
			}
			node = node.getNextSibling();
		}
		lg.fine("Code was OK and no error were found.");
		return false;
	}

	/**
	 * Retrieve the content of XML tag in an element.
	 * i.e for <employee><name>John</name></employee> xml snippet if
	 * the element points to employee node and tagName is 'name' I will return John
	 * @param element the element
	 * @param tagName the tag name to retrieve
	 * @return the text in the tag name of the element
	 */
	public static String getTextValue (final Element element, final String tagName) {
		String textVal = null;
		final NodeList nodeList = element.getElementsByTagName(tagName);
		if (nodeList != null && nodeList.getLength() > 0) {
			final Element el = (Element) nodeList.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}

	/**
	 * Calls {@link #getTextValue(Element, String)} and returns a integer value
	 * @param ele the element to parse.
	 * @param tagName the tag name to find.
	 * @return the integer value of the text in between the <code>tagName</code> tag.
	 */
	public static int getIntValue (final Element ele, final String tagName) {
		return Integer.parseInt(getTextValue(ele, tagName));
	}
	
}
