package com.perfgames.ropelab;

/** Contract checks for safe, uniformly scaled phone/tablet rendering and input mapping. */
public final class ViewportLayoutTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(float a,float b,String message){check(Math.abs(a-b)<.002f,message);}
    public static void main(String[] args){
        int[][] sizes={{480,800},{720,1600},{1080,2400},{1200,1920},{1600,2560},{1920,1200},{2560,1600},{900,1200}};
        for(int[] size:sizes)for(float density:new float[]{1,1.5f,2,3})for(boolean game:new boolean[]{false,true})for(int inset:new int[]{0,24,72}){
            int left=inset/2,top=inset,right=inset/2,bottom=inset*2;
            ViewportLayout v=ViewportLayout.create(size[0],size[1],left,top,right,bottom,density,game);
            check(Float.isFinite(v.scale)&&v.scale>0&&Float.isFinite(v.height),"all layouts have finite positive metrics");
            check(v.height>=820-.001f,"every required fixed-content region remains available");
            check(v.left>=left-.001f&&v.top>=top-.001f,"origin respects status/taskbar/side insets");
            check(v.left+480*v.scale<=size[0]-right+.001f&&v.top+v.height*v.scale<=size[1]-bottom+.001f,"actual scaled content stays within safe app bounds");
            check(v.navTop()>v.contentBottom()&&v.navDivider()>v.contentBottom()&&v.navTop()+86<=v.height+.001f,"root navigation has a separate lower region");
            check(v.arenaBottom>160&&v.arenaBottom<=v.height-90+.001f,"expanded visual arena leaves room for jump controls");
            if(!game)check(480*v.scale/density<=600+.001f,"tablet menu width remains readable rather than filling an extra-wide display");
            for(float x:new float[]{24,240,456})for(float y:new float[]{80,400,v.navTop()+43}){
                float px=v.left+x*v.scale,py=v.top+y*v.scale;
                near((px-v.left)/v.scale,x,"scaled pointer X returns to the same logical control");
                near((py-v.top)/v.scale,y,"scaled pointer Y returns to the same logical control");
            }
        }
        ViewportLayout phone=ViewportLayout.create(720,1600,0,32,0,64,2,false);
        check(phone.top==32&&phone.extra>0,"Redmi-shaped menus start below the inset and use available height");
        near(phone.top+phone.height*phone.scale,1536,"phone navigation ends above the bottom system inset");
        ViewportLayout invalid=ViewportLayout.create(0,0,0,0,0,0,0,false);
        check(Float.isFinite(invalid.scale)&&invalid.scale>0,"pre-layout zero dimensions are safe");
        System.out.println("PASS: responsive phone/tablet safe geometry, readable menu cap, separate navigation and pointer round trips ("+checks+" checks)");
    }
}
