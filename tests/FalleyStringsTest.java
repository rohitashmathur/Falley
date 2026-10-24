package com.perfgames.ropelab;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Portable language selection, formatting, fallback and campaign-copy integrity. */
public final class FalleyStringsTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static final Pattern FORMAT=Pattern.compile("%[^a-zA-Z%]*([dsf%])");
    private static List<Character> formats(String value) {
        List<Character> result=new ArrayList<>();Matcher m=FORMAT.matcher(value);
        while(m.find())if(m.group(1).charAt(0)!='%')result.add(m.group(1).charAt(0));return result;
    }
    private static Object[] arguments(List<Character> formats) {
        Object[] args=new Object[formats.size()];
        for(int i=0;i<args.length;i++){char f=formats.get(i);if(f=='d')args[i]=7;else if(f=='f')args[i]=3.5f;else args[i]="TEST";}return args;
    }
    @SuppressWarnings("unchecked")
    private static void catalogFormats() throws ReflectiveOperationException {
        Field field=FalleyStrings.class.getDeclaredField("TRANSLATIONS");field.setAccessible(true);
        Map<String,String[]> entries=(Map<String,String[]>)field.get(null);
        check(entries.size()>=183,"the shipped localized catalog is present rather than a small placeholder list");
        for(Map.Entry<String,String[]> entry:entries.entrySet()) {
            String english=entry.getKey();String[] values=entry.getValue();
            check(values!=null && values.length==2,"every registered key has Hindi and Indonesian slots");
            List<Character> specs=formats(english);Object[] args=arguments(specs);
            for(int language=0;language<=2;language++) {
                String value=language==0?english:values[language-1];
                check(value!=null && !value.trim().isEmpty(),"localized catalog entries are nonempty");
                check(formats(value).equals(specs),"translations preserve the ordered numeric/string arguments");
                String expected=String.format(Locale.US,value,args);
                check(FalleyStrings.text(language,english,args).equals(expected),"actual lookup formats every registered key without exception or fallback");
            }
        }
    }
    private static void behavior() {
        check(FalleyStrings.sanitize(-1)==FalleyStrings.ENGLISH && FalleyStrings.sanitize(99)==FalleyStrings.ENGLISH,"invalid language selection restores English");
        check(FalleyStrings.languageName(0).equals("English") && FalleyStrings.languageName(1).equals("हिन्दी") && FalleyStrings.languageName(2).equals("Bahasa Indonesia"),"the language picker identifies all three languages in their own names");
        check(FalleyStrings.text(1,null).isEmpty(),"null source copy is safe");
        check(FalleyStrings.text(1,"UNREGISTERED %d",7).equals("UNREGISTERED 7"),"unknown copy falls back to formatted English");
        check(FalleyStrings.text(99,"JUMPS %d/%d",1,2).equals("JUMPS 1/2"),"invalid language uses English without losing values");
        check(FalleyStrings.text(1,"JUMPS %d/%d","bad",2).equals("JUMPS %d/%d"),"bad formatting arguments cannot crash gameplay navigation");
        check(FalleyStrings.text(1,"JUMPS %d/%d",1,2).contains("1/2") && FalleyStrings.text(2,"WAVE %d",3).contains("3"),"dynamic jump and wave counters retain their data in localized UI copy");
        for(int language=1;language<=2;language++) {
            for(int i=0;i<StageCatalog.COUNT;i++) {
                StageCatalog.Stage s=StageCatalog.get(i);
                check(!ExpansionStrings.text(language,s.title).equals(s.title),"every playable stage title has a translation through the current UI catalog");
                check(!ExpansionStrings.text(language,s.chapterTitle).equals(s.chapterTitle),"every campaign chapter title has a translation");
            }
            for(String key:new String[]{"SETTINGS","LANGUAGE","CEILING","DOUBLE JUMP","PRIVACY POLICY"})
                check(!FalleyStrings.text(language,key).equals(key),"core settings/gameplay/privacy labels are localized");
        }
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        behavior();catalogFormats();
        System.out.println("PASS: EN/HI/ID selection/fallback, safe formatting, whole-catalog argument integrity and all "+StageCatalog.COUNT+" stage names ("+checks+" checks)");
    }
}
