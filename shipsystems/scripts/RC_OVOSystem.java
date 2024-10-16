package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局时流放慢
 * 玩家和目标船时流加速
 * 其他船减速
 * 其他所有物体透明度--
 * 其他所有物体碰撞改变
 */
public class RC_OVOSystem extends BaseShipSystemScript {
    public static final float MAX_TIME_MULT = 99f;
    private static String ID = "RC_OVOSystem";
    private static String IS_ON = "IS_ON";
    private boolean init = false;
    private ShipAPI ship;
    private ShipAPI target;
    private Map<CombatEntityAPI,CollisionClass> collisionClassMap = new HashMap<>();
    private Map<CombatEntityAPI,Vector2f> locationMap = new HashMap<>();
    private Map<CombatEntityAPI,Float> alphaMap = new HashMap<>();
    private Map<CombatEntityAPI,Boolean> phasedMap = new HashMap<>();
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        /*
        if (ship.getShipTarget()==null) {
            return;
        }
        else if (target==null){
            target = ship.getShipTarget();
        }
        */
        try {
            addBuff(ship, id, effectLevel, engine);
            //addBuff(target, id, effectLevel, engine);
            if (!init&&effectLevel>=1) {
                for (ShipAPI s : engine.getShips()) {
                    if (s!=ship&&s!=target) {
                        phasedMap.put(s, s.isPhased());
                        s.setPhased(true);
                        alphaMap.put(s, s.getAlphaMult());
                        s.setAlphaMult(0.5f);
                        //collisionClassMap.put(s, s.getCollisionClass());
                        //s.setCollisionClass(CollisionClass.NONE);
                    }
                }
                for (DamagingProjectileAPI p : engine.getProjectiles()) {
                    collisionClassMap.put(p,p.getCollisionClass());
                    p.setCollisionClass(CollisionClass.NONE);
                }
                for (CombatEntityAPI a:engine.getAsteroids()){
                    collisionClassMap.put(a,a.getCollisionClass());
                    a.setCollisionClass(CollisionClass.NONE);
                }
                init = true;
            }
            float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public void addBuff(ShipAPI ship, String id, float effectLevel,CombatEngineAPI engine) {
        MutableShipStatsAPI stats = ship.getMutableStats();

        float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, shipTimeMult);
    }

    public void removeBuff(ShipAPI ship,String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getTimeMult().unmodify(id);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            removeBuff(ship,id);
            /*
            if (target!=null) {
                removeBuff(target, id);
            }
             */
            for (CombatEntityAPI c:alphaMap.keySet()) {
                if (c instanceof ShipAPI) {
                    ((ShipAPI)c).setAlphaMult(alphaMap.get(c));
                    ((ShipAPI)c).setPhased(phasedMap.get(c));
                }
            }
            for (CombatEntityAPI c:collisionClassMap.keySet()) {
                c.setCollisionClass(collisionClassMap.get(c));
            }
            init = false;
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("超维空间", false);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (ship.getShipTarget()==null) {
            return "未选定目标";
        }
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship.getShipTarget()==null) {
            return false;
        }
        return true;
    }
}