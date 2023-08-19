package real_combat.shipsystems.scripts.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class RC_TrinityForceSystemBack extends BaseShipSystemScript {
    public static final float MIN_DISTANCE = 1f;

    public static final Color JITTER_COLOR = new Color(90,165,255,55);

    private static String ID = "RC_TrinityForceSystem";
    private static String INTEGRATED = "INTEGRATED";
    private static String MOTHERSHIP = "MOTHERSHIP";
    private static String ORIGINAL = "ORIGINAL";
    private boolean init = false;
    private ShipAPI ship;
    /***
     * F动态调整slot位置尝试
     * F动态调整武器位置尝试
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
            if(!init) {
                init = true;
                ship.setCustomData(ID+"isOn",true);
                engine.addPlugin(new RC_TrinityForceSystemCombatPlugin(ship));
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
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
            init = false;
            ship.removeCustomData(ID+"isOn");
            //engine.removePlugin();
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("合体！", false);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return true;
    }

    public static class RC_TrinityForceSystemCombatPlugin implements EveryFrameCombatPlugin {
        private final ShipAPI ship;
        private float timer = 0f;
        private float transAmAlphaMult = 1f;
        private boolean init = false;
        public RC_TrinityForceSystemCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        private void copyShip(int owner, ShipAPI newShip, ShipAPI s) {
            CombatEngineAPI engine = Global.getCombatEngine();
            newShip.setHitpoints(s.getHitpoints());
            newShip.getFluxTracker().setCurrFlux(s.getCurrFlux());
            newShip.getFluxTracker().setHardFlux(s.getMinFlux());
            newShip.setCRAtDeployment(s.getCRAtDeployment());
            newShip.setCurrentCR(s.getCurrentCR());
            newShip.setOwner(owner);
            if (newShip.getShipAI()!=null) {
                newShip.getShipAI().forceCircumstanceEvaluation();
            }
            if (newShip.getSystem() != null) {
                newShip.getSystem().setCooldownRemaining(s.getSystem().getCooldownRemaining());
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
            for (int x = 0; x < grid.length; x++)
                for (int y = 0; y < grid[x].length; y++) {
                    newShip.getArmorGrid().setArmorValue(x, y, grid[x][y]);
                }

            //没有效果不知道干啥用
            newShip.syncWithArmorGridState();
            newShip.syncWeaponDecalsWithArmorDamage();

            newShip.getLocation().set(s.getLocation());

            //转移粘贴对象
            if (engine.getCustomData().get("speedMapList") != null) {
                List<Map<CombatEntityAPI, CombatEntityAPI>> speedMapList = (List<Map<CombatEntityAPI, CombatEntityAPI>>) engine.getCustomData().get("speedMapList");
                for (Map<CombatEntityAPI, CombatEntityAPI> speedMap : speedMapList) {
                    for (CombatEntityAPI landing : speedMap.keySet()) {
                        if (speedMap.get(landing) != null) {
                            CombatEntityAPI target = speedMap.get(landing);
                            if (target == s) {
                                speedMap.put(landing, newShip);
                                List<Map<CombatEntityAPI, Float>> targetFacingMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("targetFacingMapList");
                                for (Map<CombatEntityAPI, Float> targetFacingMap : targetFacingMapList) {
                                    targetFacingMap.put(newShip, newShip.getFacing());
                                }
                                List<Map<CombatEntityAPI, Float>> landingDistanceMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("landingDistanceMapList");
                                for (Map<CombatEntityAPI, Float> landingDistanceMap : landingDistanceMapList) {
                                    landingDistanceMap.put(landing, MathUtils.getDistance(newShip.getLocation(), landing.getLocation()));
                                }
                                List<Map<CombatEntityAPI, Float>> angleMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("angleMapList");
                                for (Map<CombatEntityAPI, Float> angleMap : angleMapList) {
                                    angleMap.put(landing, VectorUtils.getAngle(newShip.getLocation(), landing.getLocation()));
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            if (ship.getCustomData().get(ID+"isOn")==null) {return;}
            if (!init) {
                init = true;
                try {
                    ShipVariantAPI mainVariant = ship.getVariant();
                    ShipVariantAPI newVariant = null;
                    //新船和旧船如何对应
                    for (String slotId : ship.getVariant().getModuleSlots()) {
                        for (ShipAPI m : ship.getChildModulesCopy())
                        {
                            if (slotId.equals(m.getStationSlot().getId())) {

                            }
                        }
                    }

                    FleetMemberAPI newFleetMember = Global.getFactory().createFleetMember
                            (FleetMemberType.SHIP, newVariant);
                    newFleetMember.setOwner(ship.getOwner());
                    newFleetMember.getCrewComposition().addCrew(newFleetMember.getNeededCrew());
                    float height = engine.getMapHeight();
                    Vector2f newLocation = new Vector2f(ship.getCollisionRadius() * 4, ship.getOwner() == 0 ? height / -2f - 20000f - ship.getCollisionRadius() * 4 : height / 2f + 20000f + ship.getCollisionRadius() * 4);
                    ShipAPI newShip = engine.getFleetManager(ship.getOwner()).spawnFleetMember(newFleetMember, newLocation, ship.getFacing(), 0f);
                    newShip.setCurrentCR(ship.getCurrentCR());
                    if (newShip.getShipAI()!=null) {
                        newShip.getShipAI().forceCircumstanceEvaluation();
                    }
                    //newShip.setCollisionRadius(0);
                    //newShip.setOwner(ship.getOwner());
                    //copyShip(ship.getOwner(),newShip,ship);
                    newShip.getLocation().set(ship.getLocation().x + 1000f, ship.getLocation().y + 1000f);
                } catch (Exception e)
                {
                    Global.getLogger(this.getClass()).info(e);
                }
            }

            /*
            //获取当前已合体船
            List<PartStatus> integrated = new ArrayList<>();
            if (ship.getCustomData().get(INTEGRATED)!=null)
            {
                integrated = (List<PartStatus>)ship.getCustomData().get(INTEGRATED);
            }
            //获取未合体船
            List<PartStatus> waitingIntegrated = new ArrayList<>();
            List<PartStatus> alreadyIntegrated = new ArrayList<>();
            for (PartStatus p : integrated)
            {
                if(p.isArrive)
                {
                    alreadyIntegrated.add(p);
                }
                else {
                    waitingIntegrated.add(p);
                }
            }


            CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
            CombatTaskManagerAPI task = manager.getTaskManager(false);
            float mindistance = 99999f;
            Vector2f shipLocation = ship.getLocation();
            ShipAPI add = null;

            for (ShipAPI s: engine.getShips())
            {
                if(s.getOwner() == ship.getOwner())
                {
                    CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
                    if (mission!=null)
                    {

                        if ((mission.getType() == CombatAssignmentType.LIGHT_ESCORT
                                ||mission.getType() == CombatAssignmentType.MEDIUM_ESCORT
                                ||mission.getType() == CombatAssignmentType.HEAVY_ESCORT)&&ship.getLocation().equals(mission.getTarget().getLocation()))
                        {
                            //坐标
                            Vector2f sLocation = s.getLocation();
                            float distance = MathUtils.getDistance(shipLocation,sLocation);
                            if (distance < mindistance)
                            {
                                mindistance = distance;
                                add = s;
                            }
                        }
                    }
                }
            }

            boolean isCatch = false;
            if(add!=null) {
                if (add.getCustomData().get(MOTHERSHIP) != null) {
                    isCatch = true;
                }
            }
            List<String> slotIds = ship.getVariant().getModuleSlots();
            if (add!=null && slotIds.size()>integrated.size() && !isCatch)
            {
                String slotId = slotIds.get(integrated.size());
                PartStatus partStatus = new PartStatus(false,slotId,add);
                integrated.add(partStatus);
                waitingIntegrated.add(partStatus);
                add.setCustomData(MOTHERSHIP,ship);
            }

            //不需要到齐才能合体但是合体后要把相关数据存入新船里
            //float amount = engine.getElapsedInLastFrame();
            for (PartStatus p : waitingIntegrated)
            {
                //碰撞半径直接干掉

                p.ship.setCollisionRadius(0);
                //获取slot
                WeaponSlotAPI slot = ship.getVariant().getSlot(p.slotId);
                Vector2f slotLocation = slot.getLocation();
                float slotAngle = slot.getAngle();
                move(p.ship,slotLocation,slotAngle,0);

                float nowDistance = MathUtils.getDistance(p.ship.getLocation(),slotLocation);

                if (nowDistance<MIN_DISTANCE)
                {

                    ShipVariantAPI mainVariant = ship.getVariant();
                    mainVariant.setModuleVariant(p.slotId,p.ship.getVariant());
                    FleetMemberAPI newFleetMember = Global.getFactory().createFleetMember
                            (FleetMemberType.SHIP, mainVariant);

                    float height = engine.getMapHeight();
                    Vector2f newLocation = new Vector2f(ship.getCollisionRadius() * 4 , ship.getOwner() == 0 ? height / -2f - 20000f - ship.getCollisionRadius() * 4 : height / 2f + 20000f + ship.getCollisionRadius() * 4);
                    ShipAPI newShip = manager.spawnFleetMember(newFleetMember, newLocation, ship.getFacing(), 0f);
                    newShip.setCustomData(INTEGRATED,integrated);
                    copyShip(ship.getOwner(),newShip,ship);
                    //名单存进去了
                    newShip.setCustomData(INTEGRATED,integrated);
                    //最初的主船
                    if (ship.getCustomData().get(ORIGINAL)!=null)
                    {
                        newShip.setCustomData(ORIGINAL,ship.getCustomData().get(ORIGINAL));
                    }
                    else
                    {
                        newShip.setCustomData(ORIGINAL,ship);
                    }
                    //拷贝模块船状态
                    List<ShipAPI> newShips = newShip.getChildModulesCopy();
                    List<ShipAPI> newShipsCopy = newShip.getChildModulesCopy();
                    List<ShipAPI> oldShips = ship.getChildModulesCopy();
                    List<ShipAPI> removeShip = new ArrayList<>();
                    for (ShipAPI ns:newShips)
                    {
                        WeaponSlotAPI newSlot = ns.getStationSlot();
                        for (ShipAPI os:oldShips)
                        {
                            WeaponSlotAPI oldSlot = ns.getStationSlot();
                            if (newSlot.getId().equals(oldSlot.getId())){
                                if (os.isAlive()) {
                                    copyShip(ship.getOwner(), ns, os);
                                }
                                else
                                {
                                    removeShip.add(ns);
                                }
                                //将他移出去最后一个就是最新的船
                                newShipsCopy.remove(ns);
                            }
                        }
                    }
                    //剩下的newShipsCopy应该只有一个
                    if (newShipsCopy.size()!=0) {
                        copyShip(ship.getOwner(), newShipsCopy.get(0), p.ship);
                    }
                    //清理掉非法目标避免AI混乱
                    List<ShipAPI> shipList = engine.getShips();
                    for (ShipAPI s:shipList)
                    {
                        if(s.getShipTarget()!=null)
                        {
                            if (removeShip.indexOf(s.getShipTarget())!=-1||s.getShipTarget().equals(ship)||s.getShipTarget().equals(p.ship))
                            {
                                s.setShipTarget(null);
                            }
                        }
                        //命令目标
                        CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
                        if(mission!=null) {
                            if(ship.getLocation().equals(mission.getTarget().getLocation())||p.ship.getLocation().equals(mission.getTarget().getLocation())){
                                //task.removeAssignment(mission);
                            }
                        }
                    }
                    for(ShipAPI r:removeShip)
                    {
                        Global.getCombatEngine().removeObject(r);
                    }

                    //隐藏原来的船
                    //p.ship.setPhased(true);
                    //p.ship.setOwner(100);
                    p.isArrive = true;
                    //alreadyIntegrated.add(p);
                    //隐藏主船
                    //ship.setPhased(true);
                    //ship.setOwner(100);

                }
            }
            //ship.splitShip();
            //engine.applyDamage(CombatEntityAPI entity, Vector2f point,
            //float damageAmount, DamageType damageType, float empAmount,
            //boolean bypassShields, boolean dealsSoftFlux,
            //Object source);
            ship.setCustomData(INTEGRATED,integrated);
            */
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

        /**
         * 存放某艘船的目标槽位
         * 是否已经到达
         */
        public class PartStatus{
            boolean isArrive;
            String slotId;
            ShipAPI ship;
            public PartStatus(boolean isArrive,String slotId,ShipAPI ship)
            {
                this.isArrive = isArrive;
                this.slotId = slotId;
                this.ship = ship;
            }
        }

        /**
         * 转向
         * 利用
         * @param ship
         * @param targetLocation
         * @param targetAngle
         * @param amount
         */
        private void move(ShipAPI ship, Vector2f targetLocation, float targetAngle, float amount){
            ship.getLocation().set(targetLocation);
            ship.setFacing(targetAngle);
        }
    }
}