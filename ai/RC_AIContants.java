package real_combat.ai;

import java.util.HashSet;
import java.util.Set;

public class RC_AIContants {
    public final static Set<String> driveSystemId = new HashSet<>();
    static {
        //必须转向后使用
        driveSystemId.add("burndrive");//烈焰驱动器
        driveSystemId.add("microburn");//等离子爆裂驱动器
        driveSystemId.add("microburn_omega");//等离子爆裂驱动器 - 欧米伽
        driveSystemId.add("maneuveringjets");//烈焰喷射
        driveSystemId.add("plasmajets");//等离子推进器
    }
    public final static Set<String>  pdSystemId = new HashSet<>();
    static {
        //必须转向后使用
        pdSystemId.add("flarelauncher");//热诱弹发射器
        pdSystemId.add("canister_flak");//罐式高射炮
        pdSystemId.add("flarelauncher_fighter");//诱饵发射器
        pdSystemId.add("flarelauncher_single");//诱饵发射器 (单发)
        pdSystemId.add("flarelauncher_active");//追踪热诱弹发射器

        pdSystemId.add("emp");//
    }
    public final static Set<String>  droneSystemId = new HashSet<>();
    static {
        droneSystemId.add("drone_pd");//点防无人机
        droneSystemId.add("drone_pd_x2");//点防无人机
        droneSystemId.add("drone_borer");//钻探无人机
        droneSystemId.add("drone_sensor");//感应无人机
        droneSystemId.add("drone_terminator");//终结者无人机
    }
    public final static Set<String>  statusSystemId = new HashSet<>();
    static {
        statusSystemId.add("highenergyfocus");//高能聚焦系统
        //必须转向后使用
        statusSystemId.add("ammofeed");//加速填弹器
        statusSystemId.add("forgevats");//导弹自动工厂
        statusSystemId.add("fastmissileracks");//高速导弹挂架
        statusSystemId.add("temporalshell");//时流之壳
        statusSystemId.add("acausaldisruptor");//量子干扰
        statusSystemId.add("entropyamplifier");//熵放大器
    }

    public final static Set<String>  carrierSystemId = new HashSet<>();
    static {
        //必须转向后使用
        carrierSystemId.add("reservewing");//后备部署
        carrierSystemId.add("targetingfeed");//目标馈送系统
        carrierSystemId.add("recalldevice");//召回装置
    }

    public final static Set<String>  specialSystemId = new HashSet<>();
    static {
        specialSystemId.add("mote_control");//光尘吸引场
        specialSystemId.add("drone_strike");//终结指令
        //速度达到后使用 或用于转向
        specialSystemId.add("phaseteleporter");//相位传送
        //直接使用
        specialSystemId.add("displacer");//闪现

        specialSystemId.add("fortressshield");//堡垒护盾
        specialSystemId.add("damper");//阻尼力场
        specialSystemId.add("damper_omega");//熵抑制器

        specialSystemId.add("mine_strike");//空雷突袭
    }
}
