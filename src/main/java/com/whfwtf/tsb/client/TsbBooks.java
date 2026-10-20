package com.whfwtf.tsb.client;

import com.whfwtf.tsb.TSB;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.repository.FileRepository;

import javax.annotation.Nullable;

/**
 * 《刀匠标准手册》的客户端注册。
 *
 * <p>复用 Mantle 的书系统（和匠魂《材料与你》同一套）：书的数据在
 * {@code assets/tinkersslashblade/book/bladesmith/}，由 {@code tools/gen_book.py} 生成。
 *
 * <p>只在客户端加载；服务端不会碰这个类。
 */
@SuppressWarnings("removal")
public final class TsbBooks {

    public static final ResourceLocation BLADESMITH_ID =
            ResourceLocation.tryParse(TSB.MODID + ":bladesmith");

    @Nullable
    private static BookData bladesmith;

    private TsbBooks() {}

    /** 由 {@link TSB} 的构造函数在客户端调用（与 TConstruct 的 TinkerBook 同时机）。 */
    public static void register() {
        if (bladesmith != null) {
            return;
        }
        bladesmith = BookLoader.registerBook(BLADESMITH_ID,
                new FileRepository(ResourceLocation.tryParse(TSB.MODID + ":book/bladesmith")));
    }

    @Nullable
    public static BookData bladesmith() {
        return bladesmith;
    }
}
