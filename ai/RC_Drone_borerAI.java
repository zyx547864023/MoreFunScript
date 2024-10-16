package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_NeedDrawLine;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RC_Drone_borerAI extends RC_BaseShipAI {
    private final static String ID = "RC_RepairCombatLayeredRenderingPlugin";
    private static final float REPAIR_POINT = 10f;
    private static final float ARMOR_REPAIR_POINT = 1f;

    protected IntervalUtil tracker = new IntervalUtil(1f, 1f);
    public RC_Drone_borerAI(ShipAPI ship) {
        super(ship);
    }
    /**
     * 没有目标的时候编队飞行
     */
    //路径列表
    private static List<Vector2f> routeList = new ArrayList<>();
    static {
        routeList.add(new Vector2f(300,0));
        routeList.add(new Vector2f(0,300));
        routeList.add(new Vector2f(-300,0));
        routeList.add(new Vector2f(0,-300));
    }
    //编队形状 一字阵
    private static List<Vector2f> shapeList = new ArrayList<>();
    static {
        shapeList.add(new Vector2f(0,0));
        shapeList.add(new Vector2f(50,0));
        shapeList.add(new Vector2f(100,0));
        shapeList.add(new Vector2f(150,0));
    }
    /**
     * 40CR以下不修
     * 不修的时候返回母船
     * 母船炸了
     * @param amount
     */
    @Override
    public void advance(float amount) {
        try {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            //List<ShipAPI> allyList = AIUtils.getAlliesOnMap(ship);
            Set<ShipAPI> alliesNotFighter = new HashSet<>();
            alliesNotFighter = RC_BaseShipAI.getAlliesOnMapNotFighter(ship,alliesNotFighter);
            int allyCount = alliesNotFighter.size();
            if (allyCount==0) {
                /**
                 * 	void applyDamage(CombatEntityAPI entity, Vector2f point,
                 *                      float damageAmount, DamageType damageType, float empAmount,
                 *                      boolean bypassShields, boolean dealsSoftFlux,
                 *                      Object source, boolean playSound);
                 *
                 * 	void applyDamage(CombatEntityAPI entity, Vector2f point,
                 *                      float damageAmount, DamageType damageType, float empAmount,
                 *                      boolean bypassShields, boolean dealsSoftFlux,
                 *                      Object source);
                 *
                 * 	void applyDamage(Object damageModifierParam, CombatEntityAPI entity, Vector2f point,
                 *                      float damageAmount, DamageType damageType, float empAmount,
                 *                      boolean bypassShields, boolean dealsSoftFlux,
                 *                      Object source, boolean playSound);
                 */
                engine.applyDamage(ship,ship.getLocation(),10000F,DamageType.ENERGY,0,true,true,ship,true);
                ship.splitShip();
                return;
            }
            //获取飞机的半径
            FighterWingAPI wing = ship.getWing();
            if (wing != null) {
                float shipRange = wing.getRange();
                //获取母船的坐标
                ShipAPI motherShip = ship.getWing().getSourceShip();
                //搜索母船在飞机半径范围内血最少的船 如果全体满血就找甲最少的船
                float minHp = 999999;
                float minDistance = 999999;
                float minArmor = 999999;
                ShipAPI hpTarget = null;
                ShipAPI armorTarget = null;
                float needCR = 0;
                //如果母船炸了重新找母船
                if (!motherShip.isAlive()||motherShip.isRetreating())
                {
                    Iterator i$ = alliesNotFighter.iterator();
                    while (i$.hasNext()) {
                        ShipAPI s = (ShipAPI) i$.next();
                        float distance = MathUtils.getDistance(s, ship);
                        if (distance < minDistance && s.getOwner() == ship.getOwner()) {
                            minDistance = distance;
                            motherShip = s;
                        }
                    }
                }
                //护航任务
                CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                CombatTaskManagerAPI task = manager.getTaskManager(false);
                CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(motherShip);
                Vector2f targetLocation = null;
                if (mission!=null) {
                    if ((mission.getType() == CombatAssignmentType.LIGHT_ESCORT
                            || mission.getType() == CombatAssignmentType.MEDIUM_ESCORT
                            || mission.getType() == CombatAssignmentType.HEAVY_ESCORT)) {
                        float distance = MathUtils.getDistance(motherShip, mission.getTarget().getLocation());
                        if (distance < shipRange) {
                            targetLocation = mission.getTarget().getLocation();
                        }
                    }
                }
                alliesNotFighter.remove(RC_BaseShipAI.getAlliesOnMapCR(ship,new HashSet<ShipAPI>()));
                Iterator i$ = alliesNotFighter.iterator();
                while (i$.hasNext()) {
                    ShipAPI s = (ShipAPI) i$.next();
                    if (s.getOwner() != motherShip.getOwner() || s.equals(ship)) {
                        continue;
                    }
                    float distance = MathUtils.getDistance(s, motherShip);
                    float maxArmor = 0;
                    if (distance < shipRange) {
                        //存活、不是飞机、是跟母船同一个阵营
                        if (s.isAlive() && !s.isFighter() && s.getOwner() == motherShip.getOwner() && !s.equals(ship)) {
                            if (s.getCurrentCR() > 0.45) {
                                if(s.getLocation().equals(targetLocation))
                                {
                                    //获取Hp
                                    if (s.getHitpoints() < s.getMaxHitpoints()-1) {
                                        needCR = REPAIR_POINT/s.getMaxHitpoints()/4;
                                    }
                                    float nowArmor = 0;
                                    int count = 0;
                                    float[][] grid = s.getArmorGrid().getGrid();
                                    for (int x = 0; x < grid.length; x++) {
                                        for (int y = 0; y < grid[x].length; y++) {
                                            nowArmor += s.getArmorGrid().getArmorValue(x, y);
                                            count++;
                                        }
                                    }
                                    maxArmor = count * s.getArmorGrid().getArmorRating() / 15f;
                                    //获取装甲最少
                                    if (nowArmor < maxArmor-1) {
                                        needCR = ARMOR_REPAIR_POINT/maxArmor/30;
                                    }
                                    flyToTarget(s, amount, needCR);
                                    return;
                                }
                                //获取Hp
                                if (s.getHitpoints() < minHp && s.getHitpoints() < s.getMaxHitpoints()-1) {
                                    minHp = s.getHitpoints();
                                    hpTarget = s;
                                    needCR = REPAIR_POINT/s.getMaxHitpoints()/4;
                                }
                                float nowArmor = 0;
                                int count = 0;
                                float[][] grid = s.getArmorGrid().getGrid();
                                for (int x = 0; x < grid.length; x++) {
                                    for (int y = 0; y < grid[x].length; y++) {
                                        nowArmor += s.getArmorGrid().getArmorValue(x, y);
                                        count++;
                                    }
                                }
                                maxArmor = count * s.getArmorGrid().getArmorRating() / 15f;
                                //获取装甲最少
                                if (nowArmor < minArmor && nowArmor < maxArmor-1) {
                                    minArmor = nowArmor;
                                    armorTarget = s;
                                    needCR = ARMOR_REPAIR_POINT/maxArmor/30;
                                }
                            }
                        }
                    }
                }
                //飞过去
                if (hpTarget!=null)
                {
                    flyToTarget(hpTarget, amount, needCR);
                    if (ship.getPhaseCloak()!=null) {
                        if (MathUtils.getDistance(ship.getLocation(),hpTarget.getLocation())<=hpTarget.getCollisionRadius()) {
                            if (ship.isPhased()) {
                                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                            }
                        }
                        else {
                            usePhase(amount);
                        }
                    }
                }
                else if(armorTarget!=null)
                {
                    flyToTarget(armorTarget, amount, needCR);
                    if (ship.getPhaseCloak()!=null) {
                        if (MathUtils.getDistance(ship.getLocation(),armorTarget.getLocation())<=armorTarget.getCollisionRadius()) {
                            if (ship.isPhased()) {
                                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                            }
                        }
                        else {
                            usePhase(amount);
                        }
                    }
                }
                else if(motherShip.isAlive()){
                    flyToTarget(motherShip, amount, needCR);
                    if (ship.getPhaseCloak()!=null) {
                        if (MathUtils.getDistance(ship.getLocation(),motherShip.getLocation())<=motherShip.getCollisionRadius()) {
                            if (ship.isPhased()) {
                                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                            }
                        }
                    }
                }
                else {
                    float mindistance = 999999f;
                    for (ShipAPI a:RC_BaseShipAI.getAlliesOnMapNotFighter(ship,new HashSet<ShipAPI>())) {
                        if (MathUtils.getDistance(a,ship)<mindistance&&!a.isFighter()) {
                            motherShip = a;
                            mindistance = MathUtils.getDistance(a,ship);
                        }
                    }
                    flyToTarget(motherShip, amount, needCR);
                    if (ship.getPhaseCloak()!=null) {
                        if (MathUtils.getDistance(ship.getLocation(),motherShip.getLocation())<motherShip.getCollisionRadius()) {
                            if (ship.isPhased()) {
                                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info("");
        }
    }

    /**
     * 飞向目标
     *  百分比扣除CR
     */
    public void flyToTarget(ShipAPI target, float amount ,float needCR) {
        Vector2f targetLocation = target.getLocation();
        boolean isGet = false;
        Object repairTarget = target;
        float mult = 1f;
        java.util.List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = target.getEngineController().getShipEngines();
        for (int e = 0; e < shipEngines.size(); e++) {
            ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
            if (oe.isDisabled()) {
                targetLocation = oe.getLocation();
                mult = 0.5f;
                isGet = true;
                repairTarget = oe;
                needCR = 0;
                break;
            }
        }

        if (!isGet) {
            java.util.List<WeaponAPI> shipWeapons = target.getAllWeapons();
            for (int w = 0; w < shipWeapons.size(); w++) {
                WeaponAPI ow = shipWeapons.get(w);
                if (ow.isDisabled()) {
                    targetLocation = ow.getLocation();
                    mult = 0.5f;
                    isGet = true;
                    repairTarget = ow;
                    needCR = 0;
                    break;
                }
            }
        }

        if(!isGet) {
            float width = target.getSpriteAPI().getWidth();
            float height = target.getSpriteAPI().getHeight();
            float[][] grid = target.getArmorGrid().getGrid();
            float xlength = grid.length;
            float ylength = grid[0].length;

            //获得船最左上的格子坐标
            Vector2f leftTop = MathUtils.getPoint(target.getLocation(),height/2,target.getFacing());
            leftTop = MathUtils.getPoint(leftTop,width/2,target.getFacing()+90);

            for (int x = 0; x < grid.length; x++) {
                if(isGet)
                {
                    break;
                }
                for (int y = 0; y < grid[x].length; y++) {
                    if (target.getArmorGrid().getArmorRating() / 15f > target.getArmorGrid().getArmorValue(x, y)) {
                        targetLocation = target.getLocation();
                        mult = 0.5f;
                        isGet = true;
                        repairTarget = new Vector2f(x,y);
                        break;
                    }
                }
            }
            if (isGet){
                targetLocation = MathUtils.getPoint(leftTop,width/xlength*((Vector2f)repairTarget).x+width/xlength/2,target.getFacing()-90);
                targetLocation = MathUtils.getPoint(targetLocation,height/ylength*(ylength-((Vector2f)repairTarget).y)-height/ylength/2,target.getFacing()-180);
            }
        }

        if(!isGet){
            if (target.getHitpoints() < target.getMaxHitpoints()) {
                isGet = true;
            }
        }
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        float distance = MathUtils.getDistance(targetLocation,ship.getLocation());
        if(distance>ship.getCollisionRadius()*10)
        {
            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
            ship.giveCommand(ShipCommand.ACCELERATE, (Object)null ,0);
        }
        //如果很近那就围绕飞船转圈
        else{
            if (isGet) {
                if (MathUtils.getShortestRotation(shipFacing, toTargetAngle) > 0) {
                    RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
                    ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object)null ,0);
                } else {
                    RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
                    ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object)null ,0);
                }
                if (ship.isPhased()||target.isPhased()) {
                    return;
                }
                Map<ShipAPI,RC_NeedDrawLine> allDrawShip = (Map<ShipAPI,RC_NeedDrawLine>) engine.getCustomData().get(ID);
                if(allDrawShip==null)
                {
                    allDrawShip = new HashMap<>();
                }
                RC_NeedDrawLine thisDrawShip = allDrawShip.get(target);
                if(thisDrawShip==null)
                {
                    thisDrawShip = new RC_NeedDrawLine(target,0,new ArrayList<Vector2f>(),new ArrayList<Vector2f>(), Color.GREEN);
                }
                thisDrawShip.startList.add(ship.getLocation());
                if (ship.isPhased()) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }
                thisDrawShip.endList.add(targetLocation);
                //thisDrawShip.angle = toTargetAngle;
                allDrawShip.put(target,thisDrawShip);
                engine.getCustomData().put(ID,allDrawShip);

                if (repairTarget instanceof ShipAPI) {
                    target.setHitpoints(target.getHitpoints() + amount * REPAIR_POINT);
                } else if (repairTarget instanceof WeaponAPI) {
                    WeaponAPI weapon = ((WeaponAPI)repairTarget);
                    if (weapon.getCurrHealth()+amount * REPAIR_POINT<weapon.getMaxHealth()) {
                        weapon.setCurrHealth(weapon.getCurrHealth() + amount * REPAIR_POINT);
                    }
                } else if (repairTarget instanceof ShipEngineControllerAPI.ShipEngineAPI) {
                    ShipEngineControllerAPI.ShipEngineAPI engine = ((ShipEngineControllerAPI.ShipEngineAPI)repairTarget);
                    if (engine.getHitpoints()+amount * REPAIR_POINT<engine.getMaxHitpoints()) {
                        engine.setHitpoints(engine.getHitpoints() + amount * REPAIR_POINT);
                    }
                } else if (repairTarget instanceof Vector2f) {
                    Vector2f location = ((Vector2f) repairTarget);
                    float nowHp = target.getArmorGrid().getArmorValue((int) location.x, (int) location.y);
                    target.getArmorGrid().setArmorValue((int) location.x, (int) location.y, nowHp + amount );
                }
                target.setCurrentCR(target.getCurrentCR() - amount * needCR);
                target.syncWithArmorGridState();
                target.syncWeaponDecalsWithArmorDamage();
            }
            else {

            }
        }
    }
    @Override
    public void usePhase(float amount){
        mayHitProj.clear();
        mayHitBeam.clear();
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            if(mayHit(damagingProjectile,ship.getCollisionRadius())){
                isProjectileMany = true;
                break;
            }
        }
        if (!isProjectileMany) {
            for (BeamAPI b : engine.getBeams()) {
                if(mayHit(b,ship.getCollisionRadius())){
                    isProjectileMany = true;
                    break;
                }
            }
        }
        if(isProjectileMany) {
            if (!ship.isPhased()) {
                if (ship.getFluxLevel()<0.9f) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }
            }
            else {
                if (ship.getFluxLevel()>0.9f) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null, 0);
                }
            }
        }
        else {
            if (ship.isPhased()&&ship.getFluxLevel()>0.3f) {
                tracker.advance(amount);
                if (tracker.intervalElapsed()) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }
            }
        }
    }
}
