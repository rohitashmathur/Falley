package com.perfgames.ropelab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Current opt-in disclosures, independent controls, local-data limits and complete translations. */
public final class PrivacyPolicyTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        String text=PrivacyPolicy.fullText();
        check(PrivacyPolicy.UPDATED_DATE.equals("Updated: 29 October 2026"),"4.4 policy date");
        check(text.contains("Firebase Crashlytics")&&text.contains("Google Analytics for Firebase"),"both providers disclosed");
        check(text.contains("off by default")&&text.contains("enable it separately"),"independent opt-in defaults");
        check(text.contains("both options off when the app starts")&&text.contains("does not initialize Firebase"),"cold-start SDK boundary disclosed");
        check(text.contains("next app launch")&&text.contains("stored locally while Firebase was active earlier"),"crash next-launch/cached-report semantics");
        check(text.contains("queued or in transit")&&text.contains("does not delete reports already received"),"in-flight and server-deletion limits");
        check(text.contains("reset local Analytics data")&&text.contains("Analytics changes apply immediately"),"analytics opt-out reset semantics");
        check(text.contains("approximate geography")&&text.contains("IP address"),"network-derived Analytics geography disclosed");
        check(text.contains("not included in Falley reporting events"),"country hint distinguished from SDK geography");
        check(text.contains("advertising-ID collection and advertising personalization"),"no ad identity/personalization");
        check(text.contains("raw sensor readings")&&text.contains("touch coordinates"),"raw inputs excluded");
        check(text.contains("installation identifiers")&&text.contains("app-instance identifier"),"pseudonymous SDK identifiers disclosed");
        check(text.contains("network-state")&&text.contains("background wake")&&text.contains("Play install-referrer"),"normal SDK permission purposes disclosed");
        check(text.contains("app-install/source attribution"),"automatic installation attribution disclosed without promising a personal-data-free referrer");
        check(text.contains("90 days")&&text.contains("property settings"),"retention describes providers without inventing console settings");
        check(text.contains("Share Result")&&text.contains("No result image is shared automatically"),"explicit sharing remains separate");
        check(text.contains(PrivacyPolicy.CONTACT_EMAIL)&&text.contains(PrivacyPolicy.PUBLIC_URL),"verified contact and URL retained");
        check(!text.contains("has no Internet permission")&&!text.contains("no advertising, analytics or tracking SDKs"),"obsolete offline-only guarantees removed");
        Map<String,String[]> catalog=new HashMap<>();catalog.put("Country and language",new String[]{"stale","stale"});
        PrivacyPolicy.addTranslations(catalog);Set<String> headings=new HashSet<>();
        for(String metadata:new String[]{PrivacyPolicy.TITLE,PrivacyPolicy.PUBLISHER,PrivacyPolicy.EFFECTIVE_DATE,PrivacyPolicy.UPDATED_DATE})translated(catalog,metadata);
        for(String[] section:PrivacyPolicy.SECTIONS){
            check(section.length==2,"heading/body structure");check(headings.add(section[0]),"unique policy heading");
            translated(catalog,section[0]);translated(catalog,section[1]);
            check(text.contains(section[0]+"\n"+section[1]),"full text contains exact current section");
        }
        check(PrivacyPolicy.SECTIONS.length==14,"all current disclosure sections retained");
        check(!catalog.get("Country and language")[0].equals("stale"),"current policy overrides older catalog copy");
        System.out.println("PASS: 4.4 optional Firebase disclosures, consent timing/cache/retention limits, local sensor/country boundaries and full EN/HI/ID policy ("+checks+" checks)");
    }
    private static void translated(Map<String,String[]> catalog,String english){
        String[] row=catalog.get(english);check(row!=null&&row.length==2,"Hindi/Indonesian row exists");
        for(String value:row){check(value!=null&&!value.trim().isEmpty()&&!value.equals(english),"real translation for current text");check(value.indexOf('\ufffd')<0&&value.indexOf('\0')<0,"no text corruption");}
    }
}
