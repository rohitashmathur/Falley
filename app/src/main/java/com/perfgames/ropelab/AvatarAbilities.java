package com.perfgames.ropelab;

/** Original avatar traits. Heights are relative to Human at the same score/gravity.
 * Immunity applies only to active projectiles; every arena boundary remains lethal. */
public final class AvatarAbilities {
    public static int normalize(int avatar){return avatar>=0&&avatar<AvatarCatalog.COUNT?avatar:AvatarCatalog.HUMAN;}
    public static float jumpHeight(int avatar){switch(normalize(avatar)){case AvatarCatalog.DEMON:case AvatarCatalog.ANGEL:case AvatarCatalog.SHADOW:return 1.2f;case AvatarCatalog.MILITARY:return .85f;case AvatarCatalog.ASTRONAUT:return 1.35f;default:return 1;}}
    public static float massScale(int avatar){switch(normalize(avatar)){case AvatarCatalog.MILITARY:return 1.25f;case AvatarCatalog.ASTRONAUT:return .75f;case AvatarCatalog.ROBOT:return 1.15f;case AvatarCatalog.SHADOW:return .85f;default:return 1;}}
    public static boolean fireballImmune(int avatar){return normalize(avatar)==AvatarCatalog.ROBOT;}
    public static boolean beamImmune(int avatar,int kind){return normalize(avatar)==AvatarCatalog.SHADOW&&(kind==ArcadeRules.TOP_LASERS||kind==ArcadeRules.SIDE_LASERS||kind==ArcadeRules.LIGHTNING);}
    private AvatarAbilities(){}
}
