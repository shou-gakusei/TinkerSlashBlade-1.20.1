package com.whfwtf.tsb;

import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

/** 三种刀部件。 */
public enum TsbPartType {

    /** 刀身：决定耐久、攻击与刀性。 */
    BLADE("tsb_part_blade"),
    /** 刀柄：决定耐久加成。 */
    HANDLE("tsb_part_handle"),
    /** 刀鞘：决定攻击修正。 */
    SAYA("tsb_part_saya");

    private final String id;

    TsbPartType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "item.tinkersslashblade." + id;
    }

    /** 该部件装配时需要几个材料锭。 */
    public int ingots() {
        return this == BLADE ? 2 : 1;
    }

    public static TsbPartType byId(String id) {
        for (TsbPartType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return BLADE;
    }

    /**
     * 该部件吃哪种材料属性。
     *
     * <p>匠魂靠这个字段判断「哪些材料能做成这个部件」：浇筑/部件制造台的展示配方会按材料是否拥有
     * 对应属性展开，所以部件必须绑定 {@link MaterialStatsId}（参考匠魂西洋剑的 ToolPartItem 写法）。
     */
    public MaterialStatsId statsId() {
        return this == HANDLE ? HandleMaterialStats.ID : HeadMaterialStats.ID;
    }

    public Item item() {
        return TsbParts.item(this);
    }
}
