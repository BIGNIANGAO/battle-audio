package battleaudio.ui;

import battleaudio.localization.I18n;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Drives the real Config panel with unchanged input and checks the reported symptoms. */
public final class SettingsPerformanceSmoke {
    private static Object field(Object target,String name)throws Exception{java.lang.reflect.Field value=target.getClass().getDeclaredField(name);value.setAccessible(true);return value.get(target);}
    public static void run(SettingsScreen screen,double openMilliseconds)throws Exception {
        Stage stage=(Stage)field(screen,"stage");Table root=(Table)field(screen,"root");
        TextButton add=stage.getRoot().findActor(I18n.text("添加候选音频"));
        if(add==null)throw new AssertionError("Missing real add-audio control");
        Path settingsFile=Paths.get(System.getProperty("battleaudio.home"),"settings.json");
        long lastSaveBefore=Files.getLastModifiedTime(settingsFile).toMillis();
        java.util.List<Map<String,Object>> samples=new ArrayList<>();
        for(int frame=0;frame<36;frame++) {
            screen.update();stage.act(1f/60);
            screen.render((com.badlogic.gdx.graphics.g2d.SpriteBatch)stage.getBatch());
            Vector2 position=add.localToStageCoordinates(new Vector2());
            Map<String,Object> sample=new LinkedHashMap<>();sample.put("frame",frame);sample.put("x",position.x);sample.put("y",position.y);
            sample.put("width",add.getWidth());sample.put("height",add.getHeight());sample.put("fontScale",add.getLabel().getFontScaleX());sample.put("preferredWidth",add.getPrefWidth());sample.put("rootNeedsLayout",root.needsLayout());samples.add(sample);
        }
        Map<String,Object> report=new LinkedHashMap<>();report.put("configOpenMs",openMilliseconds);report.put("samples",samples);
        double widthMinimum=Double.POSITIVE_INFINITY,widthMaximum=0,scaleMinimum=Double.POSITIVE_INFINITY,scaleMaximum=0;int pending=0;
        for(int index=5;index<samples.size();index++){Map<String,Object> sample=samples.get(index);double width=((Number)sample.get("width")).doubleValue(),scale=((Number)sample.get("fontScale")).doubleValue();widthMinimum=Math.min(widthMinimum,width);widthMaximum=Math.max(widthMaximum,width);scaleMinimum=Math.min(scaleMinimum,scale);scaleMaximum=Math.max(scaleMaximum,scale);if((Boolean)sample.get("rootNeedsLayout"))pending++;}
        report.put("idleConfigWrites",Files.getLastModifiedTime(settingsFile).toMillis()!=lastSaveBefore);
        report.put("addButtonWidthRange",widthMaximum-widthMinimum);report.put("addButtonFontScaleRange",scaleMaximum-scaleMinimum);report.put("pendingLayoutFrames",pending);
        Files.write(Paths.get("settings-performance.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        System.out.println("MEASURE: Config open="+Math.round(openMilliseconds)+" ms; add-button width range="+(widthMaximum-widthMinimum)+"; font-scale range="+(scaleMaximum-scaleMinimum)+"; unsettled layout frames="+pending);
        Skin skin=(Skin)field(screen,"skin");
        com.badlogic.gdx.graphics.g2d.BitmapFont font=skin.getFont("settings-font");
        String newUserText="龘曌鬱갂힣あวะ";
        for(char character:newUserText.toCharArray())if(!font.getData().hasGlyph(character))throw new AssertionError("Lazy font cannot add new user character U+"+Integer.toHexString(character));
        com.badlogic.gdx.scenes.scene2d.ui.Label newName=new com.badlogic.gdx.scenes.scene2d.ui.Label(newUserText,skin);
        newName.setPosition(20,80);stage.addActor(newName);screen.render((com.badlogic.gdx.graphics.g2d.SpriteBatch)stage.getBatch());newName.remove();
        java.util.List<String> failures=new ArrayList<>();
        if(openMilliseconds>650)failures.add("Config initialization blocks for "+Math.round(openMilliseconds)+" ms (limit 650 ms)");
        if(widthMaximum-widthMinimum>.5 || scaleMaximum-scaleMinimum>.005)failures.add("Add-audio button oscillates with unchanged input");
        if(pending>1)failures.add("Config layout does not settle across idle frames");
        if(!failures.isEmpty())throw new AssertionError(String.join("; ",failures));
        System.out.println("PASS: Config opens promptly and add-audio geometry/text stays stable across idle frames");
    }
}