package com.croety.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class ContentCleanupTests {
    // 从真实注册表检查，防止只隐藏创造栏而遗留示例内容。
    @GameTest(template = "empty")
    public static void exampleContentIsNotRegistered(GameTestHelper helper) {
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("croety", "example_item")), "示例物品应取消注册");
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("croety", "example_block")), "示例方块物品应取消注册");
        helper.assertTrue(!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("croety", "example_block")), "示例方块应取消注册");
        helper.assertTrue(!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(new ResourceLocation("croety", "example_tab")), "示例创造标签页应取消注册");
        helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(new ResourceLocation("croety", "demo")), "实际内容标签页必须保留");
        helper.succeed();
    }
}
