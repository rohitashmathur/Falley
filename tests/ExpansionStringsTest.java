package com.perfgames.ropelab;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;
/** Actual expansion catalog formatting plus localized stage and story coverage. */
public final class ExpansionStringsTest {
    private static int checks;
    private static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    private static List<Character> types(String s){List<Character> r=new ArrayList<>();Matcher m=Pattern.compile("%[^a-zA-Z%]*([dsf%])").matcher(s);while(m.find())if(!m.group(1).equals("%"))r.add(m.group(1).charAt(0));return r;}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception {
        Field f=ExpansionStrings.class.getDeclaredField("STRINGS");f.setAccessible(true);
        Map<String,String[]> map=(Map<String,String[]>)f.get(null);check(map.size()>=130,"complete expansion catalog");
        for(Map.Entry<String,String[]> e:map.entrySet()){
            List<Character> spec=types(e.getKey());Object[] values=new Object[spec.size()];
            for(int i=0;i<values.length;i++){char t=spec.get(i);if(t=='d')values[i]=7;else if(t=='f')values[i]=3.5f;else values[i]="DEMO";}
            for(int language=0;language<3;language++){
                String expected=language==0?e.getKey():e.getValue()[language-1];
                check(expected!=null&&!expected.trim().isEmpty(),"nonempty localized text");check(types(expected).equals(spec),"ordered format types preserved");
                check(ExpansionStrings.text(language,e.getKey(),values).equals(String.format(Locale.US,expected,values)),"actual expansion lookup formats correctly");
            }
        }
        for(int language=1;language<3;language++)for(int i=0;i<6;i++){
            check(!ReleaseStrings.text(language,ChapterStories.intro(i)).equals(ChapterStories.intro(i)),"chapter intro localized");
            check(!ReleaseStrings.text(language,ChapterStories.outro(i)).equals(ChapterStories.outro(i)),"chapter ending localized");
        }
        System.out.println("PASS: expansion EN/HI/ID catalog, format integrity and chapter stories ("+checks+" checks)");
    }
}
