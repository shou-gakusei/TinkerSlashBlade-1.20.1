package com.whfwtf.tsb.compat.createmeow;

import com.whfwtf.tsb.TsbBladeItem;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;

/**
 * Create-Meowchanics（「机械动力：老吴学」）「猫猫玩具兵」饰品武器槽兼容桥 —— TSB 侧。
 *
 * <p>Create-Meowchanics 会在猫的武器槽中读取武器面板攻击力作为加成；对 TSB 匠魂拔刀剑，
 * 按设计规则取「SSS 下评分」的 50%：即拔刀剑基础攻击修正值
 * （{@link ISlashBladeState#getBaseAttackModifier()}）乘以 {@link #SSS_BELOW_FACTOR}。</p>
 *
 * <p>本类不依赖 Create-Meowchanics，可独立编译与运行。实际调用由 CM 侧反射桥
 * （{@code cn.laowu.mod.compat.tinkerslashblade.TsbWeaponPanel}）在运行时发起，
 * 通过 {@link #toySoldierWeaponBonus(ItemStack)} 取加成值。</p>
 */
public final class CreateMeowCompat {
    /** 「SSS 下评分」折减系数：取半折。 */
    public static final float SSS_BELOW_FACTOR = 0.5F;

    private CreateMeowCompat() {
    }

    /**
     * 猫猫玩具兵武器槽加成：若 stack 是 TSB 匠魂拔刀剑则返回「SSS 下评分 × 50%」，否则 0。
     *
     * @param stack 猫武器槽中的物品
     * @return 加成攻击力（已含 0.5 折减）；非拔刀剑或无可读状态时返回 0
     */
    public static float toySoldierWeaponBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0F;
        if (!(stack.getItem() instanceof ItemSlashBlade)) return 0F; // 仅处理拔刀剑系（TSB 的 TsbBladeItem 也继承它）
        ISlashBladeState state = stack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null) return 0F;
        float baseAttack = state.getBaseAttackModifier(); // 拔刀剑基础攻击，即「SSS 下评分」载体
        if (baseAttack <= 0F) return 0F;
        return baseAttack * SSS_BELOW_FACTOR;
    }
}
