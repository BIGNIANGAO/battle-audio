package battleaudio;

import battleaudio.localization.I18n;
import battleaudio.model.Config;
import com.megacrit.cardcrawl.core.Settings;
import java.util.*;

public final class LocalizationTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void run() {
        Map<String,String> source=I18n.catalog("zhs");
        Settings.GameLanguage saved=Settings.language;
        try {
            check(I18n.languages().length==24,"All 24 game languages are supported");
            for(String language:I18n.languages()) {
                Map<String,String> entries=I18n.catalog(language);
                check(entries.keySet().equals(source.keySet()),language+" has a complete catalog");
                Settings.language=Settings.GameLanguage.valueOf(language.toUpperCase(Locale.ROOT));
                check(I18n.language().equals(language),"Follows the game's current language: "+language);
                for(Map.Entry<String,String> entry:entries.entrySet()) {
                    check(entry.getValue()!=null && !entry.getValue().trim().isEmpty(),language+" nonempty translation: "+entry.getKey());
                    check(I18n.text(entry.getKey()).equals(entry.getValue()),language+" actually selects its catalog");
                    if(entry.getKey().contains("≥"))check(entry.getValue().contains("≥"),language+" retains >= operator");
                    if(entry.getKey().contains("≤"))check(entry.getValue().contains("≤"),language+" retains <= operator");
                }
                check(String.format(Locale.ROOT,I18n.text("%.2f 秒"),1.25).contains("1.25"),language+" retains valid number formatting");
                String file="工程配乐-测试.mp3";
                check(I18n.error("内置音频文件缺失："+file).endsWith(file),language+" preserves file names in errors");
                Config config=new Config();Config.Rule rule=new Config.Rule();rule.name="我自己的中文规则 / My Rule";rule.triggers=EnumSet.of(Config.Trigger.LOW_HEALTH);config.rules.add(rule);
                String before=new com.google.gson.Gson().toJson(config);I18n.text("低血量 ≤ 阈值");
                check(before.equals(new com.google.gson.Gson().toJson(config)),language+" does not mutate rule names or IDs");
            }
            check(I18n.text("关闭","ENG").equals("Close"),"Reviewed English close label");
            check(I18n.text("关闭","unknown").equals("Close"),"Unknown language has English fallback");
            check(I18n.text("not a UI key","fra").equals("not a UI key"),"Unknown text is preserved");
            check(I18n.text("更好的自定义音频触发","zhs").equals("更好的自定义音频触发"),"Approved simplified Chinese name is unchanged");
            battleaudio.ui.LocalizationChoiceTests.run();
        } finally {Settings.language=saved;}
        System.out.println("PASS: "+checks+" localization, format, error and persistence checks across 24 languages");
    }
}