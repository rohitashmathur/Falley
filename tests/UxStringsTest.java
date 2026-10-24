package com.perfgames.ropelab;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;
/** Every new screen label, formatting signature and contextual instruction is localized. */
public final class UxStringsTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static List<Character> types(String text){List<Character> result=new ArrayList<>();Matcher m=Pattern.compile("%[^a-zA-Z%]*([dsf%])").matcher(text);while(m.find())if(!m.group(1).equals("%"))result.add(m.group(1).charAt(0));return result;}
    @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
        Field field=UxStrings.class.getDeclaredField("STRINGS");field.setAccessible(true);Map<String,String[]> map=(Map<String,String[]>)field.get(null);
        check(map.size()>=100,"all spotlight interface copy present");
        for(Map.Entry<String,String[]> e:map.entrySet()){
            List<Character> signature=types(e.getKey());Object[] values=new Object[signature.size()];for(int i=0;i<values.length;i++)values[i]=signature.get(i)=='d'?Integer.valueOf(7):signature.get(i)=='f'?Float.valueOf(3.5f):"DEMO";
            for(int language=0;language<3;language++){
                String expected=language==0?e.getKey():e.getValue()[language-1];
                check(expected!=null&&!expected.trim().isEmpty(),"nonempty translated label");check(types(expected).equals(signature),"ordered format signature preserved");
                check(UxStrings.text(language,e.getKey(),values).equals(String.format(Locale.US,expected,values)),"actual interface lookup formats expected label");
            }
        }
        for(int language=0;language<3;language++){
            check(UxStrings.text(language,"CHAPTER %d",3).equals(ExpansionStrings.text(language,"CHAPTER %d",3)),"existing expansion copy delegates");
            check(UxStrings.text(language,"BACK").equals(ExpansionStrings.text(language,"BACK")),"legacy copy delegates");
            for(String reason:new String[]{null,"CEILING","HARD LANDING","FALLING FIRE","LIGHTNING","BUZZSAW","TOP LASER","SIDE LASER","SPIKES","LASER","FIRE"}){
                String tip=UxRules.deathTip(reason);check(language==0||!UxStrings.text(language,tip).equals(tip),"every contextual loss tip localized");
            }
            for(boolean motion:new boolean[]{false,true})check(language==0||!UxStrings.text(language,UxRules.controlCue(motion)).equals(UxRules.controlCue(motion)),"both control mode cues localized");
        }
        String recovery="Jump when ready. Fall to refill.";
        check(UxStrings.text(FalleyStrings.HINDI,recovery).contains("नीचे गिरने"),"Hindi recovery cue describes descent rather than requiring rope contact");
        check(UxStrings.text(FalleyStrings.INDONESIAN,recovery).contains("Turun"),"Indonesian recovery cue describes descent rather than requiring rope contact");
        for(int language=0;language<3;language++)for(String key:new String[]{recovery,"Two jumps per burst. Descend to refill.","One jump per burst. Descend to refill."})check(!UxStrings.text(language,key).contains("rope")&&!UxStrings.text(language,key).contains("रस्सी")&&!UxStrings.text(language,key).contains("tali"),"burst recovery cue never requires a rope");
        check(UxStrings.text(0,null).equals(""),"null text remains empty");check(UxStrings.text(999,"Play").equals("Play"),"invalid language safely uses English");
        System.out.println("PASS: spotlight EN/HI/ID copy, formats and contextual tips ("+checks+" checks)");
    }
}