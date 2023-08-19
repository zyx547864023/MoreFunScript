package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.prototype.entities.Ship;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_ModulesFighterAI;
import real_combat.entity.RC_AnchoredEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 分体之后飞机移动过去 ，播放起飞动画
 * 合体之后把飞机存起来，分体的时候再放出去
 *
 */

public class RC_TrinityForceSystem extends BaseShipSystemScript {
    public static final float MIN_DISTANCE = 5f;
    private static String ID = "RC_TrinityForceSystem";
    private String CONVERTED_FIGHTERBAY = "converted_fighterbay";
    private static String COLLISION_RADIUS = "COLLISION_RADIUS";
    private static String MOTHERSHIP = "MOTHERSHIP";
    private static String ORIGINAL = "ORIGINAL";
    private static String ARC = "ARC";
    private static String IS_MERGE = "IS_MERGE";
    private final static String ANCHORED_ENTITY_LIST="ANCHORED_ENTITY_LIST";
    private final static String SPRITE_NAME = "graphics/icons/hullsys/fortress_shield.png";

    private final static float THICKNESS = 10f;
    private final static Color FRINGE = new Color(20, 88, 143, 218);
    private final static Color CORE = new Color(61, 59, 59, 218);
    private boolean init = false;
    private ShipAPI ship;

    List<ShipAPI> newShips = new ArrayList<>();

    private RC_TrinityForceSystemCombatPlugin plugin;
    /***
     * F动态控制slot位置尝试
     * F动态控制武器位置尝试
     * F生成新装配舰船尝试
     * 启动F的时候搜索有护航命令的舰船
     * @param stats
     * @param id
     * @param state
     * @param effectLevel
     */
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            if (ship.getCustomData().get(ID + IS_MERGE)==null) {
                init = false;
                ship.setCustomData(ID + IS_MERGE,true);
                engine.removePlugin(plugin);
            }

            if(!init) {
                init = true;
                plugin = new RC_TrinityForceSystemCombatPlugin(ship);
                engine.addPlugin(plugin);
                if ((boolean)ship.getCustomData().get(ID + IS_MERGE)) {
                    CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                    CombatTaskManagerAPI task = manager.getTaskManager(false);
                    //获取所有子船
                    int alive = 0;
                    for (ShipAPI m:ship.getChildModulesCopy())
                    {
                        if (m.isAlive()) {
                            alive++;
                            ShipAPI newShip = null;
                            if (alive>newShips.size()){
/*
                                FleetMemberAPI newFleetMember = Global.getFactory().createFleetMember
                                        (FleetMemberType.SHIP, m.getVariant());
                                newFleetMember.setOwner(ship.getOwner());
                                newFleetMember.getCrewComposition().addCrew(newFleetMember.getNeededCrew());
                                newFleetMember.getVariant().removePermaMod(CONVERTED_FIGHTERBAY);
                                float height = engine.getMapHeight();
                                Vector2f newLocation = new Vector2f(ship.getCollisionRadius() * 4, ship.getOwner() == 0 ? height / -2f - 20000f - ship.getCollisionRadius() * 4 : height / 2f + 20000f + ship.getCollisionRadius() * 4);
                                newShip = manager.spawnFleetMember(newFleetMember, newLocation, m.getFacing(), 0f);
*/
                                float height = engine.getMapHeight();
                                Vector2f newLocation = new Vector2f(ship.getCollisionRadius() * 4, ship.getOwner() == 0 ? height / -2f - 20000f - ship.getCollisionRadius() * 4 : height / 2f + 20000f + ship.getCollisionRadius() * 4);
                                m.getVariant().removePermaMod(CONVERTED_FIGHTERBAY);
                                newShip = engine.createFXDrone(m.getVariant());
                                newShip.getLocation().set(newLocation);
                                newShip.setFacing(m.getFacing());
                                engine.addEntity(newShip);

                                newShips.add(newShip);

                                m.setCustomData(COLLISION_RADIUS, m.getCollisionClass());
                                //生成完了加回去
                                //newFleetMember.getVariant().addPermaMod(CONVERTED_FIGHTERBAY);
                                m.getVariant().addPermaMod(CONVERTED_FIGHTERBAY);
                                //子船原来的船关联
                                newShip.setCustomData(ORIGINAL, m);
                                newShip.setCustomData(MOTHERSHIP, ship);
                                newShip.setCustomData(COLLISION_RADIUS, newShip.getCollisionClass());
                            }
                            else {
                                for (ShipAPI n:newShips) {
                                    if (n.getCustomData().get(ORIGINAL).equals(m)) {
                                        newShip = n;
                                        newShip.setAlphaMult(1);
                                        newShip.setPhased(false);
                                        newShip.setCollisionClass(m.getCollisionClass());
                                        newShip.setFacing(m.getFacing());
                                        newShip.getLocation().set(m.getLocation());
                                        List<FighterLaunchBayAPI> fighterLaunchBays = newShip.getLaunchBaysCopy();
                                        for (FighterLaunchBayAPI b : fighterLaunchBays) {
                                            if (b.getWing()!=null) {
                                                List<ShipAPI> fighters = b.getWing().getWingMembers();
                                                for (ShipAPI f:fighters) {
                                                    if (f.getWing().getSpec().equals(b.getWing().getSpec())) {
                                                        f.resetDefaultAI();
                                                        f.removeCustomData(ID);
                                                        if (f.isPhased()) {
                                                            f.setPhased(false);
                                                            f.setAnimatedLaunch();
                                                            f.getLocation().set(b.getLandingLocation(f));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            copyShip(ship.getOwner(), newShip, m);
                            engine.applyDamage(newShip, newShip.getLocation(), 1f, DamageType.ENERGY, 0, true, false, newShip, false);
                            newShip.setCollisionClass(CollisionClass.NONE);
                            newShip.getVelocity().set(MathUtils.getPoint(new Vector2f(), m.getMaxSpeed(), newShip.getFacing()));
                            newShip.turnOnTravelDrive();
                            newShip.getEngineController().extendFlame(newShip, 4f, 3f, 2f);

                            //隐藏原船
                            m.setPhased(true);
                            m.setAlphaMult(0);
                            m.setCustomData(ORIGINAL, newShip);
                            m.setCollisionClass(CollisionClass.NONE);
                            //关闭系统
                            m.setShipAI(null);
                            //关闭系统
                            m.setShipSystemDisabled(true);
                            m.setCustomData(ARC, m.getShield().getArc());
                            m.getShield().setArc(0);
                            MutableShipStatsAPI mstats = m.getMutableStats();
                            mstats.getBallisticWeaponRangeBonus().modifyFlat(ID,-100000f);
                            mstats.getEnergyWeaponRangeBonus().modifyFlat(ID,-100000f);
                            mstats.getMissileWeaponRangeBonus().modifyFlat(ID,-100000f);
                        }
                    }
                    if(alive == 0)
                    {
                        return;
                    }
                    //这里代码可能有点问题 缺少合体后newShip的target清理
                    List<ShipAPI> shipList = engine.getShips();
                    for (ShipAPI s:shipList) {
                        if (s.getShipTarget() != null) {
                            for (ShipAPI m:ship.getChildModulesCopy())
                            {
                                if(s.getShipTarget().equals(m))
                                {
                                    s.setShipTarget(null);
                                }
                            }
                        }
                        //命令目标
                        CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
                        if(mission!=null) {
                            for (ShipAPI m:ship.getChildModulesCopy())
                            {
                                if(m.getLocation().equals(mission.getTarget().getLocation())){
                                    task.removeAssignment(mission);
                                }
                            }
                        }
                    }
                }
            }
            else {
                //是否已经合体完
                if(ship.getCustomData().get(ID + IS_MERGE)!=null) {
                    if (!(boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                        //如果不是继续合体
                        ship.setCustomData(ID, true);
                    }
                }
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e.getMessage());
        }
    }

    /***
     * 解除F之后把船还原回来
     * 当船离开主船一定距离以后重置碰撞
     * 敌人数量为零时自动解除
     * @param stats
     * @param id
     */
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {

        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            if (ship!=null) {
                if (ship.getCustomData().get(ID + IS_MERGE) != null) {
                    if ((boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                        return new StatusData("分体！", false);
                    } else {
                        return new StatusData("合体！", false);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) {return null;}
        //如果想要合体然后幅能大于1/2
        if (ship.getCustomData().get(ID + IS_MERGE)!=null) {
            if (ship.getFluxTracker().getCurrFlux() > ship.getMaxFlux() / 2 && !(boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                return "幅能不足";
            }
        }
        int alive = 0;
        for (ShipAPI m:ship.getChildModulesCopy()) {
            if (m.isAlive()) {
                alive++;
            }
        }
        if (alive==0) {
            return "模块全毁";
        }
        if (ship!=null) {
            if (ship.getCustomData().get(ID + IS_MERGE) != null) {
                if ((boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                    return "分体！";
                } else {
                    return "合体！";
                }
            }
            else {
                return "分体！";
            }
        }
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //如果想要合体然后幅能大于1/2
        if (ship.getCustomData().get(ID + IS_MERGE)!=null) {
            if (ship.getFluxTracker().getCurrFlux() > ship.getMaxFlux() / 2 && !(boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                return false;
            }
        }
        int alive = 0;
        for (ShipAPI m:ship.getChildModulesCopy()) {
            if (m.isAlive()) {
                alive++;
            }
        }
        if (alive==0) {
            return false;
        }
        return true;
    }

    private void copyShip(int owner, ShipAPI newShip, ShipAPI s) {
        CombatEngineAPI engine = Global.getCombatEngine();
        newShip.setHitpoints(s.getHitpoints());
        newShip.getFluxTracker().setCurrFlux(s.getCurrFlux());
        newShip.getFluxTracker().setHardFlux(s.getMinFlux());
        newShip.setCRAtDeployment(s.getCRAtDeployment());
        newShip.setCurrentCR(s.getCurrentCR());
        newShip.setOwner(owner);
        newShip.resetDefaultAI();
        if (newShip.getShipAI()!=null) {
            newShip.getShipAI().forceCircumstanceEvaluation();
        }

        if (newShip.getSystem() != null) {
            newShip.getSystem().setCooldownRemaining(s.getSystem().getCooldownRemaining());
        }

        ShieldAPI shield = s.getShield();
        if (shield!=null)
        {
            if(shield.isOn())
            {
                newShip.getShield().toggleOn();
            }
            newShip.getShield().setArc(shield.getArc());
        }
        ShipSystemAPI phaseCloak = s.getPhaseCloak();
        if (phaseCloak!=null)
        {
            newShip.getPhaseCloak().forceState(phaseCloak.getState(),0f);
            newShip.getPhaseCloak().setCooldownRemaining(phaseCloak.getCooldownRemaining());
        }

        newShip.getVelocity().set(s.getVelocity());
        s.getVelocity().set(new Vector2f());
        s.setPhased(true);
        s.setCollisionClass(CollisionClass.NONE);

        List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = s.getEngineController().getShipEngines();
        List<ShipEngineControllerAPI.ShipEngineAPI> newShipEngines = newShip.getEngineController().getShipEngines();
        for (int e = 0; e < shipEngines.size(); e++) {
            ShipEngineControllerAPI.ShipEngineAPI ne = newShipEngines.get(e);
            ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
            ne.setHitpoints(oe.getHitpoints());
            if (oe.isDisabled()) {
                ne.disable();
            }
        }

        List<WeaponAPI> shipWeapons = s.getAllWeapons();
        List<WeaponAPI> newShipWeapons = newShip.getAllWeapons();
        for (int w = 0; w < shipWeapons.size(); w++) {
            WeaponAPI nw = newShipWeapons.get(w);
            WeaponAPI ow = shipWeapons.get(w);
            nw.setCurrHealth(ow.getCurrHealth());
            nw.setAmmo(ow.getAmmo());
            nw.setCurrAngle(ow.getCurrAngle());
            nw.setRemainingCooldownTo(ow.getCooldownRemaining());
            if (ow.isDisabled()) {
                nw.disable();
            }
        }

        float[][] grid = s.getArmorGrid().getGrid();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                newShip.getArmorGrid().setArmorValue(x, y, grid[x][y]);
            }
        }

        //没有效果不知道干啥用
        newShip.syncWithArmorGridState();
        newShip.syncWeaponDecalsWithArmorDamage();

        newShip.getLocation().set(s.getLocation());

        //转移粘贴对象
        if(engine.getCustomData().get(ANCHORED_ENTITY_LIST)!=null) {
            List<RC_AnchoredEntity> myAnchoredEntityList = (List<RC_AnchoredEntity>) engine.getCustomData().get(ANCHORED_ENTITY_LIST);
            for (RC_AnchoredEntity m:myAnchoredEntityList)
            {
                if(m.getAnchor().equals(s))
                {
                    m.reanchor(newShip,m.getLocation());
                }
            }
        }
    }

    private void copyShipStatus( ShipAPI newShip, ShipAPI s) {
        if (ship.getCustomData().get(ID) == null) {
            float height = Global.getCombatEngine().getMapHeight();
            float width = Global.getCombatEngine().getMapWidth();
            Vector2f newLocation = new Vector2f(ship.getOwner() == 0 ? -width * 4 : width * 4, ship.getOwner() == 0 ? -height * 4 : height * 4);
            newShip.getLocation().set(newLocation);
        }
    }

    private class RC_TrinityForceSystemCombatPlugin implements EveryFrameCombatPlugin {
        private final IntervalUtil Interval = new IntervalUtil(0.3f, 0.5f);
        private final ShipAPI ship;
        private final Color JITTER_COLOR = new Color(90,165,255,55);
        private final Color JITTER_UNDER_COLOR = new Color(90,165,255,155);
        private int step = 0;
        public RC_TrinityForceSystemCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        /**
         * 新船炸原船删
         * 原船炸新船删
         * @param amount
         * @param events
         */
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            //保证新船合体的时候在地图外面待着
            List<ShipAPI> removeShip = new ArrayList<>();
            for (ShipAPI s:newShips) {
                if (s.isPhased()) {
                    float height = Global.getCombatEngine().getMapHeight();
                    float width = Global.getCombatEngine().getMapWidth();
                    Vector2f newLocation = new Vector2f(ship.getOwner() == 0 ? -width * 4 : width * 4, ship.getOwner() == 0 ? -height * 4 : height * 4);
                    s.getLocation().set(newLocation);
                    //命令目标 取消箭头
                    CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                    CombatTaskManagerAPI task = manager.getTaskManager(false);
                    CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
                    if(mission!=null) {
                        task.removeAssignment(mission);
                    }
                    if(s.getCustomData().get(ORIGINAL)!=null)
                    {
                        ShipAPI m = (ShipAPI)s.getCustomData().get(ORIGINAL);
                        if (!m.isAlive()){
                            removeShip.add(s);
                        }
                    }
                }
                else if(!s.isAlive()){
                    if(s.getCustomData().get(ORIGINAL)!=null)
                    {
                        ShipAPI m = (ShipAPI)s.getCustomData().get(ORIGINAL);
                        removeShip.add(m);
                    }
                }
            }
            for (ShipAPI r : removeShip)
            {
                //CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                //manager.removeFromReserves(r.getFleetMember());
                engine.removeObject(r);
                newShips.remove(r);
            }
            if (engine.isPaused()) {return;}
            //撤退
            if (ship.isRetreating())
            {
                //已经出地图
                if (ship.getLocation().y<0||ship.getLocation().y>engine.getMapHeight()) {
                    removeShip = new ArrayList<>();
                    for (ShipAPI s : newShips) {
                        if (s.isPhased()) {
                            removeShip.add(s);
                        }
                    }
                    for (ShipAPI r : removeShip)
                    {
                        engine.removeObject(r);
                        newShips.remove(r);
                    }
                }
            }
            if (!ship.isAlive()) {
                return;
            }
            try {
                if (ship.getCustomData().get(ID + IS_MERGE)!=null) {
                    //新船炸 原船炸
                    int outCount = 0;
                    int aliveCount = 0;
                    for (ShipAPI m : ship.getChildModulesCopy()) {
                        if(m.isAlive()) {
                            aliveCount++;
                        }
                        if (m.getCustomData().get(ORIGINAL) != null) {
                            ShipAPI newShip = (ShipAPI) m.getCustomData().get(ORIGINAL);
                            if (!newShip.isAlive()) {
                                m.removeCustomData(ORIGINAL);
                            } else {
                                //分体的情况下 状态全部复制给模块，这样显示正常
                                copyShipStatus(m, newShip);

                                //新船离开母船碰撞半径 恢复碰撞半径
                                float nowDistance = MathUtils.getDistance(newShip, ship);
                                if (nowDistance > 0) {
                                    outCount++;
                                    CollisionClass collisionClass = (CollisionClass) newShip.getCustomData().get(COLLISION_RADIUS);
                                    newShip.setCollisionClass(collisionClass);
                                    newShip.turnOffTravelDrive();
                                }
                            }
                        }
                    }
                    if ( outCount >= aliveCount) {
                        ship.setCustomData(ID + IS_MERGE, false);
                    }
                    //按下合体 开始合体
                    if (ship.getCustomData().get(ID)!=null) {
                        switch (step){
                            case 0:ship.getFluxTracker().setCurrFlux(ship.getMaxFlux()/2+ship.getCurrFlux());
                                    ship.getFluxTracker().setHardFlux(ship.getMaxFlux()/2+ship.getMinFlux());
                                    step=2;
                                    break;
                            //产生大量幅能
                            case 1:step=2;break;
                            //所有船瞬间传送到相应位置 先透明度变成0
                            //大家都抖动
                            case 2:
                                int alive = 0;
                                for (ShipAPI m:ship.getChildModulesCopy())
                                {
                                    if (m.getCustomData().get(ORIGINAL)!=null)
                                    {
                                        ShipAPI newShip = (ShipAPI) m.getCustomData().get(ORIGINAL);
                                        if (newShip.isAlive()) {
                                            alive++;
                                            newShip.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                            newShip.setAlphaMult(newShip.getAlphaMult() - amount);
                                            if (newShip.getAlphaMult() < 0) {
                                                newShip.setAlphaMult(0);
                                                //获取新的
                                                float distance = m.getCollisionRadius() + ship.getCollisionRadius();
                                                if (distance < MathUtils.getDistance(m.getLocation(),ship.getLocation())+MIN_DISTANCE) {
                                                    distance = MathUtils.getDistance(m.getLocation(),ship.getLocation())+MIN_DISTANCE;
                                                }
                                                Vector2f newLocation = MathUtils.getPoint(ship.getLocation(), distance, m.getFacing());
                                                newShip.getLocation().set(newLocation);
                                                step = 3;

                                                //合体开始时转移所有飞机到旧的航母 失败 只能假装降落然后移除
                                                List<FighterWingAPI> wings = newShip.getAllWings();
                                                for (FighterWingAPI w : wings) {
                                                    for (ShipAPI f : w.getWingMembers()) {
                                                        ShipAIPlugin ai = f.getShipAI();
                                                        if (f.getCustomData().get(ID) == null) {
                                                            f.setShipAI(new RC_ModulesFighterAI(f, m, newShip, null, null));
                                                            f.setCustomData(ID, true);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (alive==0)
                                {
                                    step = 5;
                                    ship.setCustomData(ID + IS_MERGE, true);
                                }
                                else {
                                    //禁用姿态
                                    if(ship.equals(engine.getPlayerShip())) {
                                        engine.maintainStatusForPlayerShip(ID, SPRITE_NAME, "团结之力", "禁用姿态控制", true);
                                    }
                                    ship.getVelocity().set((Vector2f) ship.getVelocity().scale(0.99f));
                                    ship.setAngularVelocity(0);
                                    ship.setJitter(this, JITTER_COLOR, 1, 3, 0, 1f);
                                    ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                }
                                break;
                            case 3:
                                alive = 0;
                                for (ShipAPI m:ship.getChildModulesCopy())
                                {
                                    if (m.getCustomData().get(ORIGINAL)!=null)
                                    {
                                        ShipAPI newShip = (ShipAPI) m.getCustomData().get(ORIGINAL);
                                        if (newShip.isAlive()) {
                                            alive++;
                                            newShip.setFacing(m.getFacing());
                                            newShip.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                            newShip.setAlphaMult(newShip.getAlphaMult() + amount);
                                            if (newShip.getAlphaMult() > 1) {
                                                newShip.setAlphaMult(1);
                                                newShip.setCollisionClass(CollisionClass.NONE);
                                                step = 4;
                                            }
                                        }
                                    }
                                }
                                if (alive==0)
                                {
                                    step = 5;
                                    ship.setCustomData(ID + IS_MERGE, true);
                                }
                                else {
                                    //禁用姿态
                                    if(ship.equals(engine.getPlayerShip())) {
                                        engine.maintainStatusForPlayerShip(ID, SPRITE_NAME, "团结之力", "禁用姿态控制", true);
                                    }
                                    ship.getVelocity().set(new Vector2f(0, 0));
                                    ship.setAngularVelocity(0);
                                    ship.setJitter(this, JITTER_COLOR, 1, 3, 0, 1f);
                                    ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                }
                                break;
                            //慢慢移动
                            //跟随转向 通过command控制其移动 主船可移动
                            case 4:
                                int allReady = 0;
                                for (ShipAPI m:ship.getChildModulesCopy())
                                {
                                    if (m.getCustomData().get(ORIGINAL)!=null)
                                    {
                                        ShipAPI newShip = (ShipAPI) m.getCustomData().get(ORIGINAL);
                                        if (newShip.isAlive()) {
                                            newShip.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                            float nowDistance = MathUtils.getDistance(newShip.getLocation(), m.getLocation());
                                            if (nowDistance < MIN_DISTANCE) {
                                                copyShip(ship.getOwner(),m,newShip);
                                                engine.applyDamage(m, m.getLocation(), 1f, DamageType.ENERGY, 0, true, false, m, false);
                                                newShip.setAlphaMult(0);
                                                newShip.setPhased(true);
                                                float height = engine.getMapHeight();
                                                float width = engine.getMapWidth();
                                                Vector2f newLocation = new Vector2f(ship.getOwner() == 0 ? -width*4 : width*4, ship.getOwner() == 0 ? -height*4 : height*4);
                                                newShip.getLocation().set(newLocation);
                                                newShip.setCollisionClass(CollisionClass.NONE);
                                                newShip.setShipAI(null);

                                                //合体后马上清理
                                                CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());

                                                //manager.removeFromReserves(newShip.getFleetMember());

                                                CombatTaskManagerAPI task = manager.getTaskManager(false);
                                                CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(newShip);
                                                if(mission!=null) {
                                                    task.removeAssignment(mission);
                                                }

                                                m.setAlphaMult(1);
                                                m.setPhased(false);
                                                CollisionClass collisionClass = (CollisionClass) m.getCustomData().get(COLLISION_RADIUS);
                                                m.setCollisionClass(collisionClass);
                                                m.removeCustomData(ORIGINAL);
                                                m.resetDefaultAI();

                                                m.setShipSystemDisabled(true);
                                                MutableShipStatsAPI mstats = m.getMutableStats();
                                                mstats.getBallisticWeaponRangeBonus().unmodifyFlat(ID);
                                                mstats.getEnergyWeaponRangeBonus().unmodifyFlat(ID);
                                                mstats.getMissileWeaponRangeBonus().unmodifyFlat(ID);
                                            } else {
                                                float angle = VectorUtils.getAngle(newShip.getLocation(),m.getLocation());
                                                Vector2f newVelocity = MathUtils.getPoint(new Vector2f(0, 0), newShip.getMaxSpeed(), angle);
                                                newShip.getVelocity().set(newVelocity);
                                                newShip.setFacing(m.getFacing());
                                                newShip.setCollisionClass(CollisionClass.NONE);
                                                //给一个电弧
                                                Vector2f newShipRandomLocation = MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()/2);
                                                Vector2f mRandomLocation = MathUtils.getRandomPointInCircle(m.getLocation(),m.getCollisionRadius()/2);
                                                Interval.advance(engine.getElapsedInLastFrame());
                                                if (Interval.intervalElapsed()) {
                                                    engine.spawnEmpArcPierceShields(ship, newShipRandomLocation, new SimpleEntity(m.getLocation()), new SimpleEntity(mRandomLocation), DamageType.ENERGY, 0f, 0f, 1000000f, null, THICKNESS+MathUtils.getRandomNumberInRange(0,1)*THICKNESS, FRINGE, CORE);
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        allReady++;
                                    }
                                }
                                //禁用姿态
                                if(ship.equals(engine.getPlayerShip())) {
                                    engine.maintainStatusForPlayerShip(ID, SPRITE_NAME, "团结之力", "禁用姿态控制", true);
                                }
                                ship.getVelocity().set(new Vector2f(0,0));
                                ship.setAngularVelocity(0);
                                ship.setJitter(this, JITTER_COLOR, 1, 3, 0, 1f);
                                ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1, 25, 0f, 7f);
                                if (allReady == ship.getChildModulesCopy().size())
                                {
                                    for (ShipAPI m:ship.getChildModulesCopy())
                                    {
                                        if (m.isAlive())
                                        {
                                            CollisionClass collisionClass = (CollisionClass) m.getCustomData().get(COLLISION_RADIUS);
                                            m.setCollisionClass(collisionClass);
                                            m.getCollisionClass();
                                        }
                                    }
                                    step = 5;
                                    ship.removeCustomData(ID + IS_MERGE);
                                    ship.removeCustomData(ID);
                                }
                                break;
                        }
                    }
                    else {

                    }
                }
                else {

                }
            } catch (Exception e)
            {
                Global.getLogger(this.getClass()).info(e);
            }
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {

        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {

        }

        @Override
        public void init(CombatEngineAPI engine) {

        }
    }
}