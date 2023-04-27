package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

public class RC_Drone_borerAI extends RC_BaseShipAI {
    private final static String ID = "RC_RepairCombatLayeredRenderingPlugin";

    private static final float REPAIR_POINT = 10f;
    private static final float ARMOR_REPAIR_POINT = 1f;
    private CombatEngineAPI engine = Global.getCombatEngine();
    private ShipwideAIFlags AIFlags = new ShipwideAIFlags();
    public RC_Drone_borerAI(ShipAPI drone) {
        super(drone);
    }

    /**
     * 40CR以下不修
     * 不修的时候返回母船
     * 母船炸了
     * @param amount
     */
    public void advance(float amount) {
        try {
            super.advance(amount);
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
                    Iterator i$ = Global.getCombatEngine().getShips().iterator();
                    while (i$.hasNext()) {
                        ShipAPI s = (ShipAPI) i$.next();
                        float distance = MathUtils.getDistance(s, ship);
                        if (distance < minDistance && s.getOwner() == ship.getOwner()) {
                            minDistance = distance;
                            motherShip = s;
                            /*
                            List<FighterLaunchBayAPI> fighterLaunchBays = motherShip.getLaunchBaysCopy();
                            for (FighterLaunchBayAPI f:fighterLaunchBays)
                            {
                                FighterWingAPI.ReturningFighter returningFighter = wing.getReturnData(ship);
                                returningFighter = new FighterWingAPI.ReturningFighter(ship,f);
                                f.land(ship);
                                break;
                            }
                             */
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
                //if (motherShip.getShipTarget()==null) {
                    Iterator i$ = Global.getCombatEngine().getShips().iterator();
                    while (i$.hasNext()) {
                        ShipAPI s = (ShipAPI) i$.next();
                        float distance = MathUtils.getDistance(s, motherShip);
                        float maxArmor = 0;
                        if (distance < shipRange) {
                            //存活、不是飞机、是跟母船同一个阵营
                            if (s.isAlive() && !s.isFighter() && s.getOwner() == motherShip.getOwner() && !s.equals(ship)) {
                                if (s.getCurrentCR() > 0.4) {
                                    if(s.getLocation().equals(targetLocation))
                                    {
                                        //获取Hp
                                        if (s.getHitpoints() < s.getMaxHitpoints()-1) {
                                            needCR = REPAIR_POINT/s.getMaxHitpoints()/2;
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
                                            needCR = ARMOR_REPAIR_POINT/maxArmor/15;
                                        }
                                        flyToTarget(s, amount, needCR);
                                        return;
                                    }
                                    //获取Hp
                                    if (s.getHitpoints() < minHp && s.getHitpoints() < s.getMaxHitpoints()-1) {
                                        minHp = s.getHitpoints();
                                        hpTarget = s;
                                        needCR = REPAIR_POINT/s.getMaxHitpoints()/2;
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
                                        needCR = ARMOR_REPAIR_POINT/maxArmor/15;
                                    }
                                }
                            }
                        }
                    }
                //}
                /*
                else {
                    if(motherShip.getShipTarget()!=null) {
                        if (motherShip.getShipTarget().getOwner() == ship.getOwner()) {
                            hpTarget = motherShip.getShipTarget();
                        }
                    }
                }
                 */
                //飞过去
                if (hpTarget!=null)
                {
                    flyToTarget(hpTarget, amount, needCR);
                }
                else if(armorTarget!=null)
                {
                    flyToTarget(armorTarget, amount, needCR);
                }
                else {
                    flyToTarget(motherShip, amount, needCR);
                }
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info("");
        }
    }

    public ShipwideAIFlags getAIFlags() {
        return this.AIFlags;
    }

    public void cancelCurrentManeuver() {

    }

    public ShipAIConfig getConfig() {
        return null;
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
                        //这里的装甲位置有偏移
                        //targetLocation = target.getArmorGrid().getLocation(x,y);
                        //获取装甲和中心之间的距离
                        //float armorDistance = MathUtils.getDistance(targetLocation,ship.getLocation()));
                        //float shipCenterToArmor = VectorUtils.getAngle(ship.getLocation(),targetLocation);
                        //targetLocation = MathUtils.getPoint(ship.getLocation(),armorDistance/2,shipCenterToArmor);
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
        float distance = MathUtils.getDistance(target,ship);
        if(distance>0)
        {
            turn(needTurnAngle, toTargetAngle, amount);
            ship.giveCommand(ShipCommand.ACCELERATE, (Object)null ,0);
        }
        //如果很近那就围绕飞船转圈
        else{
            if (isGet) {
                if (MathUtils.getShortestRotation(shipFacing, toTargetAngle) > 0) {
                    turn(needTurnAngle, toTargetAngle, amount);
                    ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object)null ,0);
                } else {
                    turn(needTurnAngle, toTargetAngle, amount);
                    ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object)null ,0);
                }

                Map<ShipAPI,NeedDrawLine> allDrawShip = (Map<ShipAPI,NeedDrawLine>) engine.getCustomData().get(ID);
                if(allDrawShip==null)
                {
                    allDrawShip = new HashMap<>();
                }
                NeedDrawLine thisDrawShip = allDrawShip.get(target);
                if(thisDrawShip==null)
                {
                    thisDrawShip = new NeedDrawLine(target,0,new ArrayList<Vector2f>(),new ArrayList<Vector2f>(),toTargetAngle);
                }
                thisDrawShip.startList.add(ship.getLocation());
                thisDrawShip.endList.add(targetLocation);
                thisDrawShip.angle = toTargetAngle;
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
                //如果周围有导弹
                MissileAPI missile = AIUtils.getNearestMissile(ship);
                float range = ship.getAllWeapons().get(0).getRange();
                if(missile!=null){
                    distance = MathUtils.getDistance(missile,ship);
                    //毕竟武器只有一个
                    if(distance<range)
                    {
                        toTargetAngle = VectorUtils.getAngle(ship.getLocation(),missile.getLocation());
                        needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                        turn(needTurnAngle, toTargetAngle, amount);
                        ship.giveCommand(ShipCommand.HOLD_FIRE, (Object)null ,0);
                        return;
                    }
                }
                //如果周围有敌人
                ShipAPI enemy = AIUtils.getNearestEnemy(ship);
                if(enemy!=null){
                    distance = MathUtils.getDistance(enemy,ship);
                    if(distance<range)
                    {
                        toTargetAngle = VectorUtils.getAngle(ship.getLocation(),enemy.getLocation());
                        needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                        turn(needTurnAngle, toTargetAngle, amount);
                        ship.giveCommand(ShipCommand.HOLD_FIRE, (Object)null ,0);
                    }
                }
            }
        }
    }

    private void turn(float needTurnAngle, float toTargetAngle, float amount){
        if( needTurnAngle - Math.abs(ship.getAngularVelocity() * amount) > ship.getMaxSpeed() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), toTargetAngle) > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object)null ,0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object)null ,0);
            }
        }
        else {
            if (ship.getAngularVelocity() > 1) {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object) null, 0);
            } else if ((ship.getAngularVelocity() < -1)) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object) null, 0);
            }
        }
    }

    public class NeedDrawLine{
        ShipAPI ship;
        float angle;
        float timer;
        List<Vector2f> startList = new ArrayList<>();
        List<Vector2f> endList = new ArrayList<>();
        public NeedDrawLine(ShipAPI ship,float timer,List<Vector2f> startList,List<Vector2f> endList,float angle){
            this.ship = ship;
            this.timer = timer;
            this.startList = startList;
            this.endList = endList;
        }
        public List<Vector2f> getStartList()
        {
            return startList;
        }
        public List<Vector2f> getEndList()
        {
            return endList;
        }

        public float getAngle() {
            return angle;
        }

        public float getTimer() {
            return timer;
        }

        public void setTimer(float timer) {
            this.timer = timer;
        }
    }
}
