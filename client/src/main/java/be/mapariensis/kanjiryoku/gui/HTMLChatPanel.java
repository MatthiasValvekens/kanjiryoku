package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.gui.utils.DummyResponseHandler;
import be.mapariensis.kanjiryoku.gui.utils.YesNoTask;
import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class HTMLChatPanel extends JPanel implements ChatInterface {
	private static final Logger log = LoggerFactory.getLogger(HTMLChatPanel.class);
	private final JEditorPane textPane;
	private final Executor promptThreads = Executors.newSingleThreadExecutor(); // ensure only one prompt can exist at a time
	private HTMLDocument document;
	private Element table;
	private final GUIBridge bridge;
	
	public static final String TABLE_ID = "chatTable";
	public static final String USERCOL_CLASS = "usercol";
	public static final String MESSAGECOL_CLASS = "messagecol";
	public static final String SERVER_CLASS = "server";
	public static final String SYSTEM_CLASS = "system";
	public static final String ERROR_CLASS = "error";
	public static final String ERROR_HEADER_CLASS = "error-header";
	public static final String KANJILINK_CLASS = "kanjilink";
	public static final String PRIVATE_MESSAGE_CLASS = "private-message";
	private final ServerResponseHandler dumpToChat = new DummyResponseHandler(this);	
	
	public HTMLChatPanel(GUIBridge bridge, String css) {
		this.bridge = bridge;
		HTMLEditorKit kit = new HTMLEditorKit();
		kit.getStyleSheet().addRule(css);
		
		textPane = new JEditorPane();
		textPane.setEditable(false);
		textPane.setEditorKit(kit);
		textPane.setContentType("text/html");
		documentSetup();
		textPane.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						AttributeSet anchorAttributes = (AttributeSet) e.getSourceElement().getAttributes().getAttribute(HTML.Tag.A);
						log.info("Clickety {}",anchorAttributes.getAttribute(HTML.Attribute.HREF));
					} catch (RuntimeException ex) {
						log.warn("Document structure is borked - could not process hyperlink event.");
					}
				}
			}
		});
		setLayout(new BorderLayout());
		add(textPane, BorderLayout.CENTER);
	}
	@Override
	public void displayServerMessage(String message) {
		append(wrap("p",Constants.SERVER_HANDLE,SERVER_CLASS), esc(message));
	}
	@Override
	public void displayGameMessage(String message) {
		append(wrap("p","*game*",SERVER_CLASS), clickableKanji(esc(message)));
	}
	@Override
	public void displayUserMessage(String from, String message, boolean broadcast) {
		CharSequence fromString = new StringBuilder().append('[').append(esc(from)).append(']');
		message = esc(message);
		if(!broadcast){
			fromString = wrap("p",fromString,PRIVATE_MESSAGE_CLASS);
			message = wrap("p",fromString,PRIVATE_MESSAGE_CLASS);
		}
		
		append(fromString,message);
	}

	@Override
	public void displayErrorMessage(int errorId, String message) {
		append(wrap("p",String.format("Error E%03d",errorId),ERROR_HEADER_CLASS),wrap("p",esc(message),ERROR_CLASS));
	}

	@Override
	public void displayErrorMessage(ClientServerException ex) {
		displayErrorMessage(ex.errorCode, ex.getMessage());
	}

	@Override
	public void displaySystemMessage(String message) {
		append(wrap("p","*system*",SYSTEM_CLASS), wrap("p",esc(message),SYSTEM_CLASS));
	}

	@Override
	public void yesNoPrompt(String question, NetworkMessage ifYes,
			NetworkMessage ifNo) {
		promptThreads.execute(new YesNoTask(this,bridge.getUplink(),question, ifYes,ifNo));
	}

	@Override
	public ServerResponseHandler getDefaultResponseHandler() {
		return dumpToChat;
	}
	
	private static String wrap(String tag,CharSequence content, String... classes) {
		StringBuilder sb = new StringBuilder(classes[0]);
		for(int i = 1; i<classes.length;i++) {
			sb.append(' ').append(classes[i]);
		}
		return String.format("<%s " +
				"class=\"%s\">%s</%s>",tag,sb,content,tag);
	}
	
	private static final String CSS_FORMAT_STRING = "<tr><td class=\""+USERCOL_CLASS+"\">%s</td><td class=\""+MESSAGECOL_CLASS+"\">%s</td></tr>"; 
	private synchronized void append(CharSequence usercol, String messagecol) {
		try {
			document.insertBeforeEnd(table, String.format(CSS_FORMAT_STRING,usercol,formatLinebreaks(messagecol)));
			// caret auto update doesn't work
			textPane.setCaretPosition(document.getLength());
		} catch(IOException | BadLocationException ex) {
			log.warn("Failed to append.",ex);
		}
	}
	private static CharSequence formatLinebreaks(CharSequence input) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i<input.length();i++) {
			char c = input.charAt(i);
			if(c!='\n') sb.append(c);
			else sb.append("<br/>");
		}
		return sb;
	}
	private static String clickableKanji(String input) {
		// locate kanji
		List<Integer> locations = new ArrayList<Integer>();
		locations.add(-1);
		for(int i = 0; i<input.length(); i++) {
			if(UnicodeBlock.of(input.charAt(i)) == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
				locations.add(i);
			}
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i<locations.size();i++) {
			int curLoc = locations.get(i);
			sb.append(input,locations.get(i-1)+1,curLoc);
			sb.append(clickableChar(input.charAt(curLoc)));
		}
		int lastloc = locations.get(locations.size()-1);
		if(lastloc < input.length() - 1) sb.append(input,lastloc+1,input.length()); // play it safe
		return sb.toString();
	}
	
	private static CharSequence clickableChar(char c) {
		return new StringBuilder().append("<a class=\"").append(KANJILINK_CLASS).append("\" href=\"").append(c).append("\">").append(c).append("</a>");
	}
	private static String esc(String s) {
		return StringEscapeUtils.escapeHtml4(s);
	}
	
	public void clear() {
		documentSetup();		
	}
	
	private void documentSetup() {
		document = (HTMLDocument) textPane.getEditorKit().createDefaultDocument();
		Element body = document.getElement(document.getDefaultRootElement(),StyleConstants.NameAttribute,HTML.Tag.BODY);
		try {
			document.insertBeforeEnd(body, "<table id=\""+TABLE_ID+"\"></table>");
		} catch (BadLocationException | IOException e) {
			log.warn("Failed to insert",e);
		}

		table = document.getElement(TABLE_ID);
		textPane.setDocument(document);
	}
}
