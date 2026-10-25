package com.perfgames.ropelab;

/** Uniform world/text scale with safe, height-aware menus. No physics coordinates change. */
public final class ViewportLayout {
    public static final float WIDTH=480, MIN_HEIGHT=820, MENU_MAX_DP=600;
    public final float scale,left,top,height,extra,arenaBottom;
    private ViewportLayout(float scale,float left,float top,float height){
        this.scale=scale;this.left=left;this.top=top;this.height=Math.max(MIN_HEIGHT,height);
        extra=Math.max(0,this.height-MIN_HEIGHT);arenaBottom=Math.min(950,this.height-90);
    }
    public static ViewportLayout create(int width,int height,int insetLeft,int insetTop,int insetRight,int insetBottom,float density,boolean gameplay){
        float w=Math.max(1,width-Math.max(0,insetLeft)-Math.max(0,insetRight));
        float h=Math.max(1,height-Math.max(0,insetTop)-Math.max(0,insetBottom));
        float cap=gameplay?w:Math.min(w,MENU_MAX_DP*Math.max(.5f,density));
        float scale=Math.max(.01f,Math.min(cap/WIDTH,h/MIN_HEIGHT));
        return new ViewportLayout(scale,insetLeft+(w-WIDTH*scale)/2,insetTop,h/scale);
    }
    public float navTop(){return height-86;}
    public float navDivider(){return height-96;}
    public float contentBottom(){return height-120;}
    public float footer(float y){return y+extra;}
    private ViewportLayout(){this(1,0,0,820);}
}