package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.hullmods.RC_SpiderCore;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * 每一个陨石有三个状态
 *
 * 脚抖动 获取最近的陨石移动到 船和模块夹角的位置前方 然后发射 移动速度和chargeup相关 发射速度1000 抖动结束
 *
 * 时间不冲突
 */

public class RC_AcceleratingField extends BaseShipSystemScript {
    private final static String ID = "RC_AcceleratingField";
    private final static String IS_ON = "IS_ON";
    private final static String WHO_CATCH = "WHO_CATCH";
    private final static String WHO_SHOOT = "WHO_SHOOT";
    private static float maxSpeed = 1000f;
    private ShipAPI ship;
    private ShipAPI motherShip;
    CombatEngineAPI engine = Global.getCombatEngine();
    private int count = 0;
    private boolean init = false;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (ship.getCustomData().get(RC_SpiderCore.ID)==null) {
            return;
        }
        else if(motherShip==null){
            motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
            //motherShip = ship.getParentStation();
        }
        try {
            if (!init) {
                init = true;
                count++;
                Global.getCombatEngine().addLayeredRenderingPlugin(new RC_AcceleratingFieldCombatPlugin(ship));
                Global.getLogger(this.getClass()).info("addLayeredRenderingPlugincount" + count);
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (engine == null) return;
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            init = false;
            ship.setCustomData(ID+IS_ON,false);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //在范围内有陨石
        for (CombatEntityAPI a : engine.getAsteroids()) {
            float distance = MathUtils.getDistance(ship, a);
            Map<String, Object> customData = a.getCustomData();
            if (ship.getCustomData().get(RC_SpiderCore.ID)==null) {
                return false;
            }
            else if(motherShip==null){
                motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
                //motherShip = ship.getParentStation();
            }
            if (customData != null&&motherShip!=null) {
               if (customData.get(WHO_CATCH) != null&&customData.get(WHO_SHOOT) == null&&distance < ship.getCollisionRadius() * 1.5f) {
                   ShipAPI target = motherShip.getShipTarget();
                   if (target==null) {
                       target = ship.getShipTarget();
                       /*
                       float minDistance = motherShip.getCollisionRadius()*10f;
                       List<ShipAPI> enemyList = AIUtils.getNearbyEnemies(motherShip,minDistance);
                       for (ShipAPI e :enemyList) {
                           float newdistance = MathUtils.getDistance(motherShip,e);
                           if (distance<minDistance&&!e.isFighter()){
                               minDistance = newdistance;
                               target = e;
                           }
                       }
                        */
                       if (target==null) {
                           return false;
                       }
                   }
                   if (!target.isAlive()||target.isFighter()||target.getOwner()==ship.getOwner()) {
                       return false;
                   }
                   return true;
               }
            }
        }
        return false;
    }

    public class RC_AcceleratingFieldCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        CombatEntityAPI proj = null;
        private boolean isShoot = false;
        private final ShipAPI ship;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public RC_AcceleratingFieldCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}

            if (!isShoot&&proj==null) {
                //获取最近的陨石
                float minDistance = ship.getCollisionRadius()*1.5f;
                for(CombatEntityAPI a:engine.getAsteroids())
                {
                    Map<String, Object> customData = a.getCustomData();
                    if (customData!=null) {
                        ShipAPI whoCatch = (ShipAPI) customData.get(WHO_CATCH);
                        ShipAPI whoShoot = (ShipAPI) customData.get(WHO_SHOOT);
                        if (customData.get("RC_AsteroidArmSTATUS")==null) {
                            continue;
                        }
                        String status =  customData.get("RC_AsteroidArmSTATUS").toString();
                        if (whoCatch!=null&&whoShoot==null&&!"SHOOT".equals(status)) {
                            float distance = MathUtils.getDistance(ship,a);
                            if (distance < minDistance && whoCatch.equals(ship.getCustomData().get(RC_SpiderCore.ID))) {
                                minDistance = distance;
                                proj = a;
                                if (minDistance==0) {
                                    proj.setCustomData(WHO_CATCH,ship);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (proj!=null&&!isShoot) {
                proj.setCollisionClass(CollisionClass.NONE);
                float distance = MathUtils.getDistance(ship.getLocation(),proj.getLocation());
                if (distance>proj.getCollisionRadius()) {
                    proj.setCustomData("RC_AsteroidArmSTATUS","SET");
                    proj.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),distance, VectorUtils.getAngle(proj.getLocation(),ship.getLocation())));
                }
                else {
                    Vector2f hulkLocation = proj.getLocation();
                    ShipAPI target = null;//motherShip.getShipTarget();
                    /*
                    if (target!=null) {
                        if (target.getFluxTracker().getFluxLevel()>=0.5||target.getHitpoints()/target.getMaxHitpoints()<=0.5f||target.getFluxTracker().isOverloaded()||target.getCurrentCR()<=0.4f||target.getFluxTracker().isVenting()) {
                            target = null;
                        }
                    }
                     */
                    if (target==null) {
                        float maxDistance = 0;
                        float range = motherShip.getCollisionRadius()*10f;
                        List<ShipAPI> enemyList = AIUtils.getNearbyEnemies(motherShip,range);
                        for (ShipAPI e :enemyList) {
                            if (e.getFluxTracker().getFluxLevel()>=0.7||e.getHitpoints()/e.getMaxHitpoints()<=0.3f||e.getFluxTracker().isOverloaded()||e.getCurrentCR()<=0.4f||e.getFluxTracker().isVenting()||e.isRetreating()) {
                                continue;
                            }
                            if (e.isAlive()&&!e.isFighter()) {
                                if (ship.getFluxLevel()>0.8f||motherShip.getFluxLevel()>0.9f) {
                                    target = e;
                                    break;
                                }
                                else if(!e.equals(motherShip.getShipTarget())) {
                                    target = e;
                                    break;
                                }
                            }
                            /*
                            float newdistance = MathUtils.getDistance(motherShip,e);
                            if (newdistance>maxDistance&&!e.isFighter()&&!e.equals(motherShip.getShipTarget())){
                                maxDistance = newdistance;
                                target = e;
                            }
                             */
                        }
                        if (target==null) {
                            return;
                        }
                    }
                    Vector2f targetLocation = target.getLocation();
                    float shipToHulkAngle = VectorUtils.getAngle(hulkLocation, targetLocation);
                    //Global.getLogger(this.getClass()).info(ID+"||"+proj);

                    String status =  proj.getCustomData().get("RC_AsteroidArmSTATUS").toString();
                    if (proj.getCustomData().get("RC_AsteroidArmSTATUS")==null) {
                        return;
                    }
                    if (!"SHOOT".equals(status)) {
                        proj.setMass(proj.getMass()*3);
                        proj.getVelocity().set(MathUtils.getPoint(new Vector2f(0, 0), maxSpeed, shipToHulkAngle));
                        //proj.getCustomData().remove(WHO_CATCH);
                        proj.setCustomData(WHO_CATCH, ship);
                        proj.setCustomData(WHO_SHOOT, ship);
                        proj.setCustomData("RC_AsteroidArmSTATUS","SHOOT");
                        float maxRangeBonus = 25f;
                        ship.setJitterUnder(this, new Color(255, 165, 90, 55), 1, 11, 0f, 3f + maxRangeBonus);
                        ship.setJitter(this, new Color(255, 165, 90, 55), 1, 4, 0f, 0 + maxRangeBonus);
                        isShoot = true;
                    }
                }
            }
        }

        public void getOut(CombatEntityAPI s){

        }

        public void init(CombatEngineAPI engine) {

        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

        @Override
        public boolean isExpired() {
            if (isShoot) {
                //发射之后删除
                if (proj != null && motherShip != null) {
                    if (MathUtils.getDistance(motherShip, proj) > motherShip.getCollisionRadius()) {
                        proj.setCollisionClass(CollisionClass.ASTEROID);
                        proj.removeCustomData("RC_AsteroidArmSTATUS");
                        count--;
                        Global.getLogger(this.getClass()).info("isExpiredcount" + count);
                        return isShoot;
                    }
                }
            }
            return false;
        }
    }
}