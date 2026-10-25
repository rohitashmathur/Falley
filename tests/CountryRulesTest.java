package com.perfgames.ropelab;
import java.util.Locale;
public final class CountryRulesTest {
 private static int checks;private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[]args){
  CountryRules.Hint h=CountryRules.choose("us","in","ID");check(h.country.equals("US")&&h.source.equals("network"),"available serving network beats home SIM/device region");
  h=CountryRules.choose("","in","ID");check(h.country.equals("IN")&&h.source.equals("sim"),"SIM fallback on unavailable network");
  h=CountryRules.choose(null,"unknown"," id ");check(h.country.equals("ID")&&h.source.equals("device"),"device region used only as fallback");
  h=CountryRules.choose(null,null,null);check(h.country.isEmpty()&&h.source.equals("none"),"no-region tablet safely has no hint");
  for(String invalid:new String[]{"", "ZZ","UK","123","USA","../../IN","i n","İD","IN\nUS"})check(CountryRules.normalize(invalid).isEmpty(),"invalid country is rejected");
  for(String code:Locale.getISOCountries()){check(CountryRules.normalize(code.toLowerCase(Locale.ROOT)).equals(code),"all ISO countries normalize independent of case");check(CountryRules.choose(code,"IN","ID").country.equals(code),"all network countries win precedence");}
  FalleyProfile p=new FalleyProfile();for(String country:new String[]{"IN","ID","US",""}){CountryRules.choose(country,"","").suggestedLanguage();check(p.language==FalleyStrings.ENGLISH&&!p.languageConfirmed,"country suggestions never mutate the explicit English default or confirmation");}
  check(CountryRules.choose("IN","","").suggestedLanguage()==FalleyStrings.HINDI&&CountryRules.choose("ID","","").suggestedLanguage()==FalleyStrings.INDONESIAN,"regional hints do not restrict the three available choices");
  System.out.println("PASS: local country priority, ISO validation, missing-region fallback and unchanged English choice ("+checks+" checks)");
 }
}
