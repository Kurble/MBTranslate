package me.brammie.mbtranslate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mbserver.api.CommandExecutor;
import com.mbserver.api.CommandSender;
import com.mbserver.api.MBServerPlugin;
import com.mbserver.api.Manifest;
import com.mbserver.api.events.EventHandler;
import com.mbserver.api.events.Listener;
import com.mbserver.api.events.PlayerChatEvent;
import com.mbserver.api.events.WorldSaveEvent;
import com.mbserver.api.game.Player;

@Manifest( name = "MBTranslate", config = LanguageStore.class )
public class MBTranslate extends MBServerPlugin implements Listener, CommandExecutor {
	
	private LanguageStore languages;
	private Map<String, String> supportedLanguages = new HashMap<String, String>();
	private boolean valid;
	
	DocumentBuilderFactory dbFactory;
	DocumentBuilder documentBuilder;
	
	public void onEnable() {
		saveConfig();
		this.languages = getConfig();		
		
		this.dbFactory = DocumentBuilderFactory.newInstance();
		try {
			this.documentBuilder = this.dbFactory.newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.getPluginManager().registerEventHandler(this);
		this.getPluginManager().registerCommand("language", new String[]{
				"setlanguage",
				"setlang",
				"lang"
		},  this);
		
		String apiRequest = "https://translate.yandex.net/api/v1.5/tr/getLangs" +
				"?key="+languages.apiKey+
				"&ui=en";
		
		try {
			URL url = new URL(apiRequest);
			
			if (documentBuilder != null) {
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				
				Document doc = documentBuilder.parse(connection.getInputStream(), "");
				NodeList langs = doc.getElementsByTagName("Item");
				for (int i=0; i<langs.getLength(); ++i) {
					Node lang = langs.item(i);
					
					if (lang != null) {
						String key = 	lang.getAttributes().getNamedItem("key").getNodeValue();
						String value = 	lang.getAttributes().getNamedItem("value").getNodeValue();
						
						supportedLanguages.put(key, value);
					}
				}
			}
			
			valid = true;
		} catch (Exception e) {
			Logger.getLogger( "Minebuilder" ).info("Failed to use yandex key. Is the maximum reached? Is it blocked? If you don't have a key please set one.");
		}
	}
	
	public void onDisable() {
		if (valid)
			saveConfig();
	}
	
	@EventHandler
	public void onSave(WorldSaveEvent e) {
	   if (e.getWorld() == getServer().getMainWorld() && valid) {
	      saveConfig();
	   }
	}
	
	private String translate(String name, String msg, String to) {
		try {
			String apiRequest = "https://translate.yandex.net/api/v1.5/tr/translate" +
					"?key="+languages.apiKey+
					"&lang="+to+
					"&text="+msg.replace(' ', '+');
			
			URL url = new URL(apiRequest);
			
			if (documentBuilder != null) {				
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				Document doc = documentBuilder.parse(connection.getInputStream(), "");
				
				Node translation = doc.getElementsByTagName("Translation").item(0);
				if (translation != null) {
					if (translation.getAttributes().getNamedItem("code").getNodeValue().equals("200")) {
						String lang = translation.getAttributes().getNamedItem("lang").getNodeValue().substring(0, 2);
						String result = "["+lang+"]"+name+": ";
						
						NodeList texts = doc.getElementsByTagName("text");
						for (int i=0; i<texts.getLength(); ++i) {
							Node text = texts.item(i);
							
							if (text != null) {
								String value = text.getTextContent();
								result += (i>0) ? (" "+value) : value;
							}
						}
						
						return result;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "[off]" + msg;
	}
	
	@EventHandler
	public void onChat( PlayerChatEvent event ) {		
		event.setCancelled(true);
		Map<String, String> translated = new HashMap<String, String>();
		
		translated.put("off", 
				event.getPlayer().getDisplayName() + ": " + event.getMessage());
		
		for (Player player: getServer().getPlayers()) {
			if (languages.languages.containsKey(player.getDisplayName())) {
				String lang = languages.languages.get(player.getDisplayName());
				
				if (!translated.containsKey(lang)) {
					translated.put(lang, translate(
							event.getPlayer().getDisplayName(), 
							event.getMessage(), lang));
				}
				
				player.sendMessage(translated.get(lang));
			} else {
				player.sendMessage(translated.get("off"));
			}
		}
	}
	
	@Override
    public void execute( String command, CommandSender sender, String[] args, String label ) {
    	if (args.length == 1) {
    		final String code = args[0];
    		
    		if (supportedLanguages.containsKey(code)) {
    			languages.languages.put(sender.getName(), code);
    			sender.sendMessage("Language set to \"" + supportedLanguages.get(code) + "\"");   		
    		} else if (code.equals("off")) {
    			languages.languages.remove(sender.getName());
    			sender.sendMessage("MBTranslate turned off :)");
    		} else {
    			sender.sendMessage("Language is not supported! Keep this format: English is en, Spanish is es, etc.");
    		}
    	} else {
    		sender.sendMessage( "Invalid amount of arguments!");
    	}
    }
	
}