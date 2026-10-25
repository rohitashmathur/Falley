package com.perfgames.ropelab;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Actual 4.2 lookup, formatting, current rope/policy sources and legacy fallback coverage. */
public final class ReleaseStringsTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static List<Character> signature(String value){
        List<Character> result=new ArrayList<>();
        Matcher matcher=Pattern.compile("%(?:[0-9]+\\$)?[-#+ 0,(<]*[0-9]*(?:\\.[0-9]+)?([a-zA-Z%])").matcher(value);
        while(matcher.find())if(!matcher.group(1).equals("%"))result.add(matcher.group(1).charAt(0));
        return result;
    }
    private static void unicode(String value){
        check(value.indexOf('\0')<0&&value.indexOf('\ufffd')<0,"no null or replacement-character corruption");
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(Character.isHighSurrogate(c)){check(i+1<value.length()&&Character.isLowSurrogate(value.charAt(i+1)),"valid Unicode surrogate pair");i++;}
            else check(!Character.isLowSurrogate(c),"no unpaired Unicode surrogate");
        }
    }
    private static final String[] LABELS={
        "Build your survival run.","Longer warnings","A steady challenge","Faster attacks","CHOOSE HAZARDS","ROPE MIX","Special ropes from Wave 2","Tap cards to add or remove hazards.","Mint chevrons: Spring rebound 1.5×","Mint chevrons: Spring / Violet bolt: Static","Every course","Campaign Stage 1 onward / Arcade Wave 2","Campaign Stage 3 onward / Arcade Wave 2","Ropes change within each course. Read their colour and symbol. Daily uses Classic.","BREEZE","New avatar unlocked","A new way to fall.","Current avatar: %s","Same physics. A different jumping style.","Use %s","Keep %s","Could not save. Please try again.","MEET YOUR AVATAR","Complete chapters for avatars. Campaign and Arcade mix rope types; Daily uses Classic.",
        "Before the drop","Three star goals","Par ≤ %d","Finish to progress. Other stars are optional.",
        "Practice options","Hide practice options","Avatars and ropes. Make the fall yours.","PREVIEW JUMP",
        "Classic jump","Wings spread on takeoff","Graceful wing flare","Tucked knees","Floating jump pose",
        "Mechanical spring pose","Shadow stretch","Jump style only. Same physics and hitbox.",
        "Campaign abilities. Daily and Arcade use Classic.","Retro music","Choose your language",
        "English is selected by default.","You can change this anytime in Settings.","Region hint: %s",
        "Available for your region","Your choice stays on this device.","1 NEW STAR","%d NEW STARS","Try this",
        "%d%% OF THE DROP","Static charged: next jump 0.8×","Static current","Spring rebound 1.5×",
        "Classic rope","Jump recovery","Classic rope in Daily and Arcade.",
        "Daily Drop refreshes at 00:00 UTC. Complete chapters for avatars; collect stars for rope abilities."
    };
    @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
        Field field=ReleaseStrings.class.getDeclaredField("STRINGS");field.setAccessible(true);
        Map<String,String[]> catalog=(Map<String,String[]>)field.get(null);
        check(catalog.size()>=45,"complete 4.2 catalog");
        for(String label:LABELS)check(catalog.containsKey(label),"new screen label is directly covered: "+label);
        for(Map.Entry<String,String[]> row:catalog.entrySet()){
            String english=row.getKey();unicode(english);check(row.getValue().length==2,"both translated languages present");
            List<Character> spec=signature(english);Object[] values=new Object[spec.size()];
            for(int i=0;i<values.length;i++)values[i]=spec.get(i)=='d'?Integer.valueOf(7):spec.get(i)=='f'?Float.valueOf(3.5f):"भारत / Indonesia";
            for(int language=0;language<3;language++){
                String expected=language==0?english:row.getValue()[language-1];
                check(expected!=null&&!expected.trim().isEmpty(),"nonempty catalog entry");unicode(expected);
                check(language==0||!expected.equals(english),"new copy is genuinely translated, not an English fallback");
                check(signature(expected).equals(spec),"ordered argument types preserved");
                check(ReleaseStrings.text(language,english,values).equals(String.format(Locale.US,expected,values)),"actual release lookup formats expected language");
            }
        }
        for(int rope=0;rope<RopeCatalog.COUNT;rope++){
            check(catalog.containsKey(RopeCatalog.name(rope)),"current rope name is covered");
            check(catalog.containsKey(RopeCatalog.description(rope)),"current mechanical description is covered exactly");
            for(int language=1;language<3;language++){
                check(!ReleaseStrings.text(language,RopeCatalog.name(rope)).equals(RopeCatalog.name(rope)),"all rope names translated");
                check(!ReleaseStrings.text(language,RopeCatalog.description(rope)).equals(RopeCatalog.description(rope)),"all rope descriptions translated");
            }
        }
        check(catalog.containsKey(PrivacyPolicy.UPDATED_DATE),"current policy update date translated");
        for(String[] section:PrivacyPolicy.SECTIONS)
            if(section[0].equals("Country and language")||section[0].equals("Data on your device")||section[0].equals("Your choices")){
                check(catalog.containsKey(section[1]),"changed full policy paragraph translated exactly");
                for(int language=1;language<3;language++){
                    check(!ReleaseStrings.text(language,section[0]).equals(section[0]),"policy heading translated through catalog chain");
                    check(!ReleaseStrings.text(language,section[1]).equals(section[1]),"full policy paragraph translated");
                }
            }
        for(int language=0;language<3;language++){
            for(String old:new String[]{"Play","CHAPTER %d","MOTION","Privacy contact","A line of light goes dark. A new way down opens."})
                check(ReleaseStrings.text(language,old,3).equals(UxStrings.text(language,old,3)),"existing catalog delegation remains unchanged");
            check(ReleaseStrings.text(language,"%d%% OF THE DROP",30).contains("30%"),"literal percent renders once");
            check(ReleaseStrings.text(language,"Par ≤ %d",3).contains("≤ 3"),"less-than-or-equal sign survives formatting");
            check(ReleaseStrings.text(language,RopeCatalog.description(RopeCatalog.SPRING)).contains("1.5×"),"spring ratio survives translation");
            check(ReleaseStrings.text(language,RopeCatalog.description(RopeCatalog.STATIC)).contains("0.8×"),"static ratio survives translation");
            check(ReleaseStrings.text(language,"Region hint: %s","भारत").contains("भारत"),"region argument is shown without corruption");
            check(ReleaseStrings.text(language,"unregistered key").equals("unregistered key"),"unknown text safely falls back");
            check(!ReleaseStrings.text(language,"Par ≤ %d","bad").isEmpty(),"invalid format argument never crashes navigation");
            check(ReleaseStrings.text(language,null).equals(""),"null key safely returns empty text");
        }
        check(ReleaseStrings.text(0,"1 NEW STAR").equals("1 NEW STAR"),"one new star uses a singular English label");
        check(ReleaseStrings.text(1,"1 NEW STAR").equals("1 नया सितारा"),"one new star uses singular Hindi noun and adjective");
        check(ReleaseStrings.text(2,"1 NEW STAR").equals("1 BINTANG BARU"),"one new star has a translated Indonesian label");
        for(int language=0;language<3;language++){
            check(!ReleaseStrings.text(language,"1 NEW STAR").equals(ReleaseStrings.text(language,"%d NEW STARS",2)),"one and multiple new-star counts remain distinct");
            check(ReleaseStrings.text(language,"%d NEW STARS",2).contains("2"),"plural path retains the actual new-star count");
        }
        check(ReleaseStrings.text(999,"Choose your language").equals("Choose your language"),"invalid language uses English");
        check(ReleaseStrings.text(0,"English is selected by default.").equals("English is selected by default."),"default copy never claims automatic country selection");
        check(ReleaseStrings.text(1,"English is selected by default.").contains("अंग्रेज़ी"),"Hindi default copy explicitly names English");
        check(ReleaseStrings.text(2,"English is selected by default.").contains("Inggris"),"Indonesian default copy explicitly names English");
        System.out.println("PASS: release EN/HI/ID labels, full privacy copy, rope descriptions, Unicode and formatting ("+checks+" checks)");
    }
}
