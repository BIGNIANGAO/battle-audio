package battleaudio.localization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.core.Settings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Translates UI text only. Stored IDs, rule names and imported file names remain untouched. */
public final class I18n {
    private static final String[] LANGUAGES = {"eng","dut","epo","ptb","zhs","zht","fin","fra","deu","gre","ind","ita","jpn","kor","nor","pol","rus","spa","srp","srb","tha","tur","ukr","vie"};
    private static final Map<String,Map<String,String>> CATALOGS = new HashMap<>();
    private I18n() { }
    public static String[] languages() { return LANGUAGES.clone(); }
    public static String language() { return code(Settings.language == null ? "eng" : Settings.language.name()); }
    public static String code(String language) {
        String value = language == null ? "eng" : language.toLowerCase(Locale.ROOT);
        for (String supported : LANGUAGES) if (supported.equals(value)) return value;
        return "eng";
    }
    public static String text(String source) { return text(source, language()); }
    public static String text(String source, String language) {
        if (source == null) return "";
        String value = catalog(language).get(source);
        if (value == null) value = catalog("eng").get(source);
        return value == null ? source : value;
    }
    public static String[] text(String[] sources) {
        String[] values = new String[sources.length];
        for (int index=0; index<sources.length; index++) values[index]=text(sources[index]);
        return values;
    }
    public static String error(String source) {
        if (source == null) return "";
        if (catalog("zhs").containsKey(source)) return text(source);
        String best=null;
        for (String key : catalog("zhs").keySet()) {
            // Translate known error prefixes, without translating names or paths in their suffixes.
            if (key.length()>=4 && (key.endsWith("：") || key.endsWith("；")) && source.startsWith(key)
                    && (best==null || key.length()>best.length())) best=key;
        }
        return best==null ? source : text(best)+error(source.substring(best.length()));
    }
    public static synchronized Map<String,String> catalog(String language) {
        String code=code(language);
        if (!CATALOGS.containsKey(code)) {
            String path="/battleaudio/localization/"+code+".json";
            try (InputStream input=I18n.class.getResourceAsStream(path)) {
                if (input==null) throw new IllegalStateException("Missing audio localization: "+code);
                Map<String,String> entries=new Gson().fromJson(new InputStreamReader(input,StandardCharsets.UTF_8),new TypeToken<LinkedHashMap<String,String>>(){}.getType());
                if (entries==null || entries.isEmpty()) throw new IllegalStateException("Empty audio localization: "+code);
                CATALOGS.put(code,Collections.unmodifiableMap(entries));
            } catch (IOException ex) { throw new IllegalStateException("Cannot read audio localization: "+code,ex); }
        }
        return CATALOGS.get(code);
    }
    public static Locale locale() {
        String[] tags={"en","nl","eo","pt-BR","zh-CN","zh-TW","fi","fr","de","el","id","it","ja","ko","nb","pl","ru","es","sr-Latn","sr-Cyrl","th","tr","uk","vi"};
        String current=language();
        for(int index=0;index<LANGUAGES.length;index++)if(LANGUAGES[index].equals(current))return Locale.forLanguageTag(tags[index]);
        return Locale.ENGLISH;
    }
}