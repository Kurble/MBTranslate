package me.brammie.mbtranslate;

import java.util.HashMap;
import java.util.Map;

public class LanguageStore {
	
	public String apiKey;
	public Map<String, String> languages;
	
	public LanguageStore() {
		apiKey = "put your key here";
		languages = new HashMap<String, String>();
	}
	
}
