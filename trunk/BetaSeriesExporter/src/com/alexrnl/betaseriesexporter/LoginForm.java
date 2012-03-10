package com.alexrnl.betaseriesexporter;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The class which generate the login form.
 * @author Alex
 */
public final class LoginForm {
	/**
	 * Class which allow to validate the form by hitting the ENTER key.
	 * @author Alex
	 */
	public static class ReturnValidateKeyListener implements KeyListener {

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyTyped (final KeyEvent e) {}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyPressed (final KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER && button != null)
				button.doClick();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyReleased (final KeyEvent e) {}
	}

	private static Logger			lg	= Logger.getLogger(LoginForm.class.getName());

	private static JFrame			frame;
	private static JTextField		login;
	private static JPasswordField	password;
	private static JButton			button;
	private static QueryManager		api;
	private static String			token;

	/**
	 * Constructor #1.<br />
	 * Default constructor declared to avoid creating an instance of the class anywhere.
	 */
	private LoginForm() {
		
	}
	
	/**
	 * Build and show a login form
	 * @param apiQM the reference to the object managing the queries to the api.
	 * @param icon the path icon of the window.
	 * @return the token matching the user's account
	 */
	public static String getToken (final QueryManager apiQM, final String icon) {
		LoginForm.api = apiQM;
		token = null;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				buildGui(icon);
			}
		});

		// Waiting for user information
		while (token == null)
			synchronized (lg) {
				try {
					lg.wait(200);
				} catch (final InterruptedException e) {
					lg.warning("Error while waiting for connection information (" + e.getMessage()
							+ ").");
					return null;
				}
			}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run () {
				frame.dispose();
			}
		});
		return token;
	}

	/**
	 * Build the simple form to login to BetaSeries
	 * @param icon the path to the icon
	 */
	private static void buildGui (final String icon) {
		frame = new JFrame("Se connecter � BetaSeries");
		final Container pane = new JPanel(new GridBagLayout());

		login = new JTextField(20);
		password = new JPasswordField(20);
		login.addKeyListener(new ReturnValidateKeyListener());
		password.addKeyListener(new ReturnValidateKeyListener());
		button = new JButton("Connection");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (final ActionEvent e) {
				login();
			}
		});

		final GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(new JLabel("Compte : "), c);
		c.gridy = 1;
		pane.add(new JLabel("Mot de passe : "), c);
		c.gridx = 1;
		c.gridy = 0;
		pane.add(login, c);
		c.gridy = 1;
		pane.add(password, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		pane.add(button, c);

		try {
			frame.setIconImage(ImageIO.read(new File(icon)));
		} catch (final IOException | NullPointerException e) {
			lg.warning("Error while loading icon (" + e.getMessage() + ")");
		}
		
		frame.setContentPane(pane);
		frame.validate();
		frame.setMinimumSize(new Dimension(380, 120));
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/**
	 * Computes the MD5 of a string.
	 * @param text the text to hash
	 * @return the hash of the text
	 */
	public static String getMD5 (final String text) {
		try {
			final StringBuffer buffer = new StringBuffer();
			final MessageDigest msgDigest = MessageDigest.getInstance("MD5");
			msgDigest.update(text.getBytes("UTF-8"));
			final byte[] digest = msgDigest.digest();
			for (final byte element : digest) {
				int value = element;
				if (value < 0) {
					value += 256;
				}
				if (value <= 14) {
					buffer.append("0" + Integer.toHexString(value));
				} else {
					buffer.append(Integer.toHexString(value));
				}
			}
			return buffer.toString();
		} catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
			lg.warning("Error while computing MD5 (" + e.getMessage() + ").");
			return null;
		}
	}

	/**
	 * Computes the MD5 of a char sequence.
	 * @param text the text to hash
	 * @return the hash of the text
	 */
	private static String getMD5 (final char[] text) {
		return getMD5(new String(text));
	}

	/**
	 * Login to the BetaSeries account
	 */
	private static void login () {
		if (login == null || password == null ||
				login.getText().isEmpty() || password.getPassword().length == 0)
			return;
		final Map<String, String> params = new HashMap<>();
		params.put(API.LOGIN, login.getText());
		params.put(API.PASSWORD, getMD5(password.getPassword()));
		final Document doc = api.execute(API.LOGIN_PAGE, params);

		if (doc == null || QueryManager.hasError(doc)) {
			token = null;
			lg.warning("Connection to account has failed: "
					+ QueryManager.getTextValue(
							(Element) doc.getElementsByTagName(API.ERRORS).item(0),
							API.ERROR_CONTENT));
			JOptionPane.showMessageDialog(frame,
					"�chec de connection � BetaSeries, v�rifiez vos identifiants.",
					"Erreur de connection", JOptionPane.ERROR_MESSAGE);
		}

		token = QueryManager.getTextValue((Element) doc.getFirstChild(), API.TOKEN);
		synchronized (lg) {
			lg.notify();
		}
	}

}
