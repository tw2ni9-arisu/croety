# Flywheel 1.0.5 与 Ponder 1.0.91 客户端 API 参考

> **本文档里的每一个类名、方法名、参数表和字段都是从本机实际使用的 jar 用 javap 导出的**，
> 没有一条是凭记忆写的。文末「8. 验证记录」列了跑过的每一条命令。
> **本文档里作为「示例」（你可以照抄的那几个）都用 javac 实际编译过**，
> classpath 只放 flywheel-forge-api、不含完整 Flywheel jar，复现方法见 4.7 与 8.5。
> 标注为「实现原文」「源码原文」「接口注释」的片段是**从真实源码摘录的引文**（含节选），
> 目的是让你看到真实行为而不是照抄，它们**不保证能直接编译**（尤其是 2.7 那段 Create 源码，
> import 已省略）。来源是 tools/find-api.ps1 -Source：Ponder / Create 用官方 sources jar，
> Flywheel 的 api/lib 用 ForgeFlower 反编译，见 8.7。
>
> 环境：Minecraft 1.20.1 + Forge 47.4.23 + Parchment 2023.09.03 映射。
> Flywheel：编译期 flywheel-forge-api-1.20.1-1.0.5-264，运行期 flywheel-forge-1.20.1-1.0.5-264。
> Ponder：Ponder-Forge-1.20.1-1.0.91（内含 Catnip，即 net.createmod.catnip）。

## 目录

**A. Flywheel**

- [1. 概念：visual / visualizer / instance / renderer 的分工](#1-概念visual--visualizer--instance--renderer-的分工)
- [2. 注册可视化](#2-注册可视化)
- [3. 顶点 / 实例数据](#3-顶点--实例数据)
- [4. 常见坑](#4-常见坑)

**B. Ponder**

- [5. Ponder 是什么，以及如何注册场景](#5-ponder-是什么以及如何注册场景)
- [6. 编写一个 Ponder 场景](#6-编写一个-ponder-场景)
- [7. 本地化](#7-本地化)
- [8. 验证记录](#8-验证记录)

---

# A. Flywheel

## 1. 概念：visual / visualizer / instance / renderer 的分工

Flywheel 1.0.5 把「渲染一个方块实体」拆成了几个互不相同的角色。先把它们和**真实的类**对上号：

| 角色 | 真实类型 | 职责 |
|---|---|---|
| **Visual**（逻辑） | dev.engine_room.flywheel.api.visual.Visual 及其子接口 | 跟随原版对象存活；每帧/每 tick 写实例数据；delete() 时释放 |
| **Visualizer**（工厂 + 开关） | dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer 与 EntityVisualizer | 把 BlockEntity / Entity 映射成一个 Visual；决定是否跳过原版渲染 |
| **Instance**（数据） | dev.engine_room.flywheel.api.instance.Instance 及其实现 | 一份 GPU 侧结构化数据（变换矩阵 / 颜色 / 光照 / overlay 等） |
| **Instancer**（批次） | dev.engine_room.flywheel.api.instance.Instancer | 同一 (InstanceType, Model, bias) 下所有实例的容器，也是创建实例的唯一入口 |
| **Renderer**（后端） | dev.engine_room.flywheel.backend.engine.* | 把 Instancer 的实例批量提交给 GPU。**只在完整 jar 里，addon 用不到也不能用** |

真正被 addon 摸到的只有前四行。Instancer 只通过 VisualizationContext#instancerProvider() 拿到。

### 1.1 Visual 家族的完整签名

javap 原文（dev.engine_room.flywheel.api.visual 包）：

```java
public interface Visual {
    void update(float);
    void delete();
}
```

其余全都是它的子接口，**按需要实现，不要全都实现**：

| 接口 | 额外要求实现的方法 | 什么时候加 |
|---|---|---|
| BlockEntityVisual<T extends BlockEntity> | void collectCrumblingInstances(java.util.function.Consumer<Instance>) | 方块实体的视觉。**必实现**，见 4.2 |
| EntityVisual<T extends Entity> | （无新增方法） | 实体的视觉 |
| EffectVisual<T extends Effect> | （无新增方法） | 特效的视觉 |
| DynamicVisual | Plan<DynamicVisual$Context> planFrame() | 需要**每帧**更新（旋转、动画） |
| TickableVisual | Plan<TickableVisual$Context> planTick() | 需要**每 tick**更新 |
| LightUpdatedVisual（extends SectionTrackedVisual） | void updateLight(float) | 需要响应光照变化 |
| SectionTrackedVisual | void setSectionCollector(SectionTrackedVisual$SectionCollector) | 需要按区块分组 |
| ShaderLightVisual（extends SectionTrackedVisual） | （无新增方法） | 由 shader 自己算光 |
| DistanceUpdateLimiter | boolean shouldUpdate(double) | 远处限频更新 |

两个 Context 的实际形状（javap）：

```java
// dev.engine_room.flywheel.api.visual.DynamicVisual$Context
public interface Context {                     // 源码里写 DynamicVisual.Context
    net.minecraft.client.Camera camera();
    org.joml.FrustumIntersection frustum();
    float partialTick();
    DistanceUpdateLimiter limiter();
}

// dev.engine_room.flywheel.api.visual.TickableVisual$Context
public interface Context { }                   // 空接口，只作为类型标记
```

Effect 本身（不是 Visual）是一个可以被「可视化」的对象：

```java
public interface Effect {
    net.minecraft.world.level.LevelAccessor level();
    EffectVisual<?> visualize(VisualizationContext, float);
}
```

### 1.2 VisualizationContext：Visual 能拿到的全部东西

```java
public interface VisualizationContext {
    InstancerProvider instancerProvider();
    net.minecraft.core.Vec3i renderOrigin();
    VisualEmbedding createEmbedding(net.minecraft.core.Vec3i);
}

public interface VisualEmbedding extends VisualizationContext {
    void transforms(org.joml.Matrix4fc, org.joml.Matrix3fc);
    void delete();
}
```

renderOrigin() 是**当前渲染批次的原点**。写实例坐标时必须减掉它，这是「模型飘到天上」最常见的原因。
好消息是 AbstractBlockEntityVisual 的子类已经通过父类的 renderOrigin() 处理好了。

### 1.3 VisualizationManager：全局入口（只读）

```java
public interface VisualizationManager {
    static boolean supportsVisualization(net.minecraft.world.level.LevelAccessor);
    static VisualizationManager get(net.minecraft.world.level.LevelAccessor);
    static VisualizationManager getOrThrow(net.minecraft.world.level.LevelAccessor);

    net.minecraft.core.Vec3i renderOrigin();
    VisualManager<net.minecraft.world.level.block.entity.BlockEntity> blockEntities();
    VisualManager<net.minecraft.world.entity.Entity> entities();
    VisualManager<Effect> effects();
    VisualizationManager$RenderDispatcher renderDispatcher();
}

public interface VisualManager<T> {
    int visualCount();
    void queueAdd(T);
    void queueRemove(T);
    void queueUpdate(T);
}
```

**注意：VisualizationManager 没有任何 register 方法。** 注册走的是 2.1 的 VisualizerRegistry。
RenderDispatcher 的三个方法是给 LevelRenderer 调的，addon 不要碰：

```java
// dev.engine_room.flywheel.api.visualization.VisualizationManager$RenderDispatcher
public interface RenderDispatcher {
    void onStartLevelRender(dev.engine_room.flywheel.api.backend.RenderContext);
    void afterEntities(dev.engine_room.flywheel.api.backend.RenderContext);
    void beforeCrumbling(dev.engine_room.flywheel.api.backend.RenderContext,
                         it.unimi.dsi.fastutil.longs.Long2ObjectMap<
                             java.util.SortedSet<net.minecraft.server.level.BlockDestructionProgress>>);
}
```

---

## 2. 注册可视化

### 2.1 两种注册入口

**底层入口** —— dev.engine_room.flywheel.api.visualization.VisualizerRegistry，全部是静态方法：

```java
public final class VisualizerRegistry {
    static <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(BlockEntityType<T>);
    static <T extends Entity>      EntityVisualizer<? super T>      getVisualizer(EntityType<T>);

    static <T extends BlockEntity> void setVisualizer(BlockEntityType<T>, BlockEntityVisualizer<? super T>);
    static <T extends Entity>      void setVisualizer(EntityType<T>, EntityVisualizer<? super T>);
}
```

（为可读性把 net.minecraft.world.level.block.entity.BlockEntity 等写成了短名，实际类型就是原版类。）

两个 Visualizer 接口：

```java
public interface BlockEntityVisualizer<T extends BlockEntity> {
    BlockEntityVisual<? super T> createVisual(VisualizationContext, T, float);
    boolean skipVanillaRender(T);
}

public interface EntityVisualizer<T extends Entity> {
    EntityVisual<? super T> createVisual(VisualizationContext, T, float);
    boolean skipVanillaRender(T);
}
```

**推荐入口**（addon 应该用这个）—— dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer：

```java
public final class SimpleBlockEntityVisualizer<T extends BlockEntity>
        implements BlockEntityVisualizer<T> {
    static <T extends BlockEntity> Builder<T> builder(BlockEntityType<T>);
}

public final class Builder<T extends BlockEntity> {
    Builder<T> factory(Factory<T>);
    Builder<T> skipVanillaRender(java.util.function.Predicate<T>);
    Builder<T> neverSkipVanillaRender();
    SimpleBlockEntityVisualizer<T> apply();      // ← 这一句才真正注册
}

public interface Factory<T extends BlockEntity> {
    BlockEntityVisual<? super T> create(VisualizationContext, T, float);
}
```

**apply() 就是注册动作** —— javap -c 显示它的字节码末尾是：

```text
46: aload_1
47: invokestatic  // Method dev/engine_room/flywheel/lib/visualization/
                  //        VisualizerRegistry.setVisualizer:(Lnet/minecraft/world/level/
                  //        block/entity/BlockEntityType;Ldev/engine_room/flywheel/api/
                  //        visualization/BlockEntityVisualizer;)V
```

也就是说链条**必须以 .apply() 结尾**。只 .factory(...) 不 .apply() 是完全静默的失败。
SimpleEntityVisualizer（对应 EntityVisualizer）有完全对称的一套
builder / factory / skipVanillaRender / neverSkipVanillaRender / apply。

### 2.2 什么时候调

必须在**客户端**、且 **BlockEntityType 已经存在之后**调一次。Create 自己的做法（javap -c 实证）：

```text
com/simibubi/create/AllBlockEntityTypes.class  →  static {} 里的 lambda 调 SimpleBlockEntityVisualizer
com/simibubi/create/AllEntityTypes.class       →  同样
```

放在 FMLClientSetupEvent 的监听里同样是安全的（此时 RegistryObject#get() 已经可用）。
如果想放在静态初始化块里，注意 BlockEntityType 的字段必须已经赋值——用 RegistryObject 的话就放到事件里。

### 2.3 完整最小示例（已编译验证）

这个例子包含三件事：**方块实体拿到 visual**、**visual 创建实例**、**实例写自己的变换**。

```java
package com.croety.client;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Consumer;

public class MyMachineVisual extends AbstractBlockEntityVisual<MyMachineBlockEntity>
        implements SimpleDynamicVisual {

    private final Instancer<TransformedInstance> instancer;
    private final TransformedInstance wheel;

    public MyMachineVisual(VisualizationContext ctx, MyMachineBlockEntity be, float partialTick) {
        super(ctx, be, partialTick);              // 1. 走 AbstractBlockEntityVisual 的构造

        Model model = Models.block(blockState);   // 2. blockState 是父类给的 protected 字段
        this.instancer = instancerProvider()      // 3. instancerProvider() 也是父类给的
                .instancer(InstanceTypes.TRANSFORMED, model);
        this.wheel = instancer.createInstance();  // 4. 实例只能由 Instancer 创建
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        // 5. 实例自己写变换。renderOrigin 已由父类处理，这里用方块局部坐标
        wheel.setIdentityTransform()
                .translate(0.5f, 0.5f, 0.5f)
                .rotateY((float) Math.toRadians(45))
                .translateBack(0.5f, 0.5f, 0.5f);
        wheel.light(computePackedLight());        // 6. 光照同样由父类算好
        wheel.setChanged();                       // 7. 必须显式告诉 Flywheel 数据变了
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(wheel);                   // 8. 挖掘破坏动画要用到，见 4.2
    }

    @Override
    public void updateLight(float partialTick) {
        relight(wheel);                           // 9. 光照变化时重新采样，见 4.2
    }

    @Override
    protected void _delete() {
        wheel.delete();                           // 10. 释放实例，否则泄漏显存
    }

    /** 在客户端初始化时调用一次，例如挂在 FMLClientSetupEvent 上。 */
    public static void register(BlockEntityType<MyMachineBlockEntity> type) {
        SimpleBlockEntityVisualizer.builder(type)
                .factory(MyMachineVisual::new)    // Factory: (ctx, be, partialTick) -> Visual
                .neverSkipVanillaRender()         // 想让原版模型也画必须显式写这行，见 4.3
                .apply();                         // ← 没有这一句就不会注册
    }
}
```

几个关键点，全是 javap 确认过的：

* AbstractBlockEntityVisual 的构造函数是 (VisualizationContext, T, float)，正好和
  SimpleBlockEntityVisualizer$Factory#create(VisualizationContext, T, float) 对上，
  所以 MyMachineVisual::new 能直接当 Factory 用。
* 父类给的现成字段：protected final T blockEntity、protected final BlockPos pos、
  protected final BlockPos visualPos、protected final BlockState blockState、protected boolean deleted。
* 父类给的现成方法：instancerProvider()、renderOrigin()、computePackedLight()、
  六个 relight(...) 重载、getVisualPosition()、isVisible(FrustumIntersection)、
  doDistanceLimitThisFrame(DynamicVisual$Context)、setSectionCollector(...)。

它们的实现（ForgeFlower 反编译 flywheel-forge-api 的原文）：

```java
// AbstractVisual
public void update(float partialTick) { }               // ← 空实现，不写就是不更新
protected abstract void _delete();
protected InstancerProvider instancerProvider() { return visualizationContext.instancerProvider(); }
protected Vec3i renderOrigin() { return visualizationContext.renderOrigin(); }
public final void delete() {
   if (!this.deleted) {                                  // ← 幂等：_delete() 只会跑一次
      this._delete();
      this.deleted = true;
   }
}

// AbstractBlockEntityVisual 的构造函数
public AbstractBlockEntityVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
   super(ctx, blockEntity.getLevel(), partialTick);
   this.blockEntity = blockEntity;
   this.pos = blockEntity.getBlockPos();
   this.blockState = blockEntity.getBlockState();
   this.visualPos = this.pos.subtract(ctx.renderOrigin());   // ← visualPos 已经是相对坐标
}
public BlockPos getVisualPosition() { return this.visualPos; }
protected int computePackedLight() { return LevelRenderer.getLightColor(this.level, this.pos); }
protected void relight(FlatLit... instances) { this.relight(this.pos, instances); }
```

这解释了两件事：为什么 update(float) 可以完全不写（父类是空实现），
以及为什么 getVisualPosition() 返回的是**渲染原点相对坐标**而不是世界坐标 ——
visualPos = pos - ctx.renderOrigin() 在构造时就减掉了。

### 2.4 自己实现 Visualizer（不用 Simple* 时）

```java
public final class MyMachineVisualizer implements BlockEntityVisualizer<MyMachineBlockEntity> {
    @Override
    public BlockEntityVisual<? super MyMachineBlockEntity> createVisual(
            VisualizationContext ctx, MyMachineBlockEntity be, float partialTick) {
        return new MyMachineVisual(ctx, be, partialTick);
    }

    @Override
    public boolean skipVanillaRender(MyMachineBlockEntity be) {
        return true;
    }
}

// 然后：
VisualizerRegistry.setVisualizer(MY_BLOCK_ENTITY_TYPE.get(), new MyMachineVisualizer());
```

### 2.5 实体视觉

AbstractEntityVisual<T extends Entity> 比方块实体版简单，**只要求 _delete()**：

```java
public class MyEntityVisual extends AbstractEntityVisual<MyEntity> implements SimpleTickableVisual {
    private final Instancer<OrientedInstance> instancer;
    private final OrientedInstance gear;

    public MyEntityVisual(VisualizationContext ctx, MyEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
        this.instancer = instancerProvider()
                .instancer(InstanceTypes.ORIENTED, Models.partial(GEAR), 1);   // 三参重载带 bias
        this.gear = instancer.createInstance();
    }

    @Override
    public void tick(dev.engine_room.flywheel.api.visual.TickableVisual.Context context) {
        gear.position(getVisualPosition())            // 父类给的世界坐标
                .rotation(entity.getYRot(), 0.0f, 0.0f, 0.0f)
                .light(computePackedLight(1.0f))
                .setChanged();
    }

    @Override
    protected void _delete() {
        gear.delete();
    }
}

// 注册（EntityType 版本）：
SimpleEntityVisualizer.builder(MY_ENTITY_TYPE.get())
        .factory(MyEntityVisual::new)
        .neverSkipVanillaRender()
        .apply();
```

AbstractEntityVisual 的额外方法：distanceSquared(double, double, double)、getVisualPosition()、
getVisualPosition(float)、isVisible(FrustumIntersection)、computePackedLight(float)、
relight(float, FlatLit...)。它的公开字段是 protected final T entity 和
protected final EntityVisibilityTester visibilityTester。

### 2.6 手动让 Flywheel 重新处理某个对象

dev.engine_room.flywheel.lib.visualization.VisualizationHelper 全是静态方法：

```java
public static void queueAdd(dev.engine_room.flywheel.api.visual.Effect);
public static void queueRemove(dev.engine_room.flywheel.api.visual.Effect);
public static void queueUpdate(net.minecraft.world.level.block.entity.BlockEntity);
public static void queueUpdate(net.minecraft.world.entity.Entity);
public static void queueUpdate(dev.engine_room.flywheel.api.visual.Effect);
public static <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(T);
public static <T extends Entity>      EntityVisualizer<? super T>      getVisualizer(T);
public static <T extends BlockEntity> boolean canVisualize(T);
public static <T extends Entity>      boolean canVisualize(T);
public static <T extends BlockEntity> boolean skipVanillaRender(T);
public static <T extends Entity>      boolean skipVanillaRender(T);
public static <T extends BlockEntity> boolean tryAddBlockEntity(T);
```

Create 自己在这些类里用了它：KineticBlockEntity、StickerBlockEntity、ArmBlockEntity、
FunnelBlockEntity、BeltTunnelBlockEntity、TrackBlockEntity。

### 2.7 一个真实的完整范例：Create 的 SingleAxisRotatingVisual

与其只看本文档的骨架，不如直接读 Create 自己的实现。
com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual
（来自 create-1.20.1-6.0.8-291-sources.jar 的原始源码，下面是**节选**：
import 已省略，tick 里的换色逻辑用省略号代替，不要整段照抄）就是最小完整形态：

```java
public class SingleAxisRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T>
        implements SimpleTickableVisual {

    protected final RotatingInstance rotatingModel;

    public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick,
                                    Direction from, Model model) {
        super(context, blockEntity, partialTick);
        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
            .createInstance()
            .rotateToFace(from, rotationAxis())
            .setup(blockEntity)
            .setPosition(getVisualPosition());

        rotatingModel.setChanged();
    }

    /** 造 Factory 的惯用写法：把构造参数烘进 lambda。 */
    public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> of(PartialModel partial) {
        return (context, blockEntity, partialTick) ->
            new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.partial(partial));
    }

    @Override
    public void update(float pt) {
        rotatingModel.setup(blockEntity).setChanged();
    }

    @Override
    public void tick(Context context) {
        // ... 改颜色 ...
        rotatingModel.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotatingModel);
    }

    @Override
    protected void _delete() {
        rotatingModel.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotatingModel);
    }
}
```

三个可复制的结论：

1. **Factory 用静态方法返回 lambda**（of / ofZ），而不是到处写 MyVisual::new ——
   Create 就是这样把 PartialModel 之类的额外参数烘进去的。
2. Create 自己的视觉**确实同时实现了 updateLight 和 collectCrumblingInstances**
   （见 [4.2](#42-abstractblockentityvisual-仍然要求你实现-collectcrumblinginstances-和-updatelight)），
   这两条不是可选项。
3. 它覆写了 update(float)（AbstractVisual 里是空实现）来做每帧数据刷新，
   同时也实现了 SimpleTickableVisual 的 tick(Context) 做每 tick 逻辑 —— 两者可以并存。

Create 里还有 OrientedRotatingVisual、ContraptionVisual 等实现可以参考，
用 tools/find-api.ps1 -Source <类名> 直接看源码（见 8.7）。

---

## 3. 顶点 / 实例数据

### 3.1 哪些类在 API jar 里 —— lib 包全部都在

这一点决定了你能 import 什么，必须先说清楚：

| 包 | 类数 | 在 flywheel-forge-api（编译期） | 在 flywheel-forge（运行期） |
|---|---|---|---|
| dev.engine_room.flywheel.api.* | 63 | 有 | 有 |
| dev.engine_room.flywheel.lib.* | 120 | **有** | 有 |
| dev.engine_room.flywheel.backend.* | 163 | **没有** | 有 |
| dev.engine_room.flywheel.impl.* | 74 | **没有** | 有 |

* API jar 合计 **183** 个类，完整 jar 合计 **420** 个类。
* 完整 jar 是 API jar 的**严格超集**：差集里没有一个是 api/lib 的类，
  237 条差异全部落在 backend.*（163）与 impl.*（74）。
* 本项目 build.gradle 的声明正是：

```groovy
compileOnly fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${minecraft_version}:${flywheel_version}")
runtimeOnly  fg.deobf("dev.engine-room.flywheel:flywheel-forge-${minecraft_version}:${flywheel_version}")
```

  所以 **api.* 和 lib.* 都可以放心 import**，backend.* / impl.* 一定编译不过。

### 3.2 dev.engine_room.flywheel.api.instance：实例的抽象层

```java
public interface Instance {
    InstanceType<?> type();
    InstanceHandle handle();
    default void setChanged();
    default void delete();
    default void setVisible(boolean);
}

public interface InstanceHandle {
    void setChanged();
    void setDeleted();
    void setVisible(boolean);
    boolean isVisible();
}

public interface InstanceType<I extends Instance> {
    I create(InstanceHandle);
    dev.engine_room.flywheel.api.layout.Layout layout();
    InstanceWriter<I> writer();
    net.minecraft.resources.ResourceLocation vertexShader();
    net.minecraft.resources.ResourceLocation cullShader();
}

public interface InstanceWriter<I extends Instance> {
    void write(long, I);
}

public interface Instancer<I extends Instance> {
    I createInstance();
    default void createInstances(I[]);
    void stealInstance(I);
}

public interface InstancerProvider {
    <I extends Instance> Instancer<I> instancer(InstanceType<I>, Model, int);
    default <I extends Instance> Instancer<I> instancer(InstanceType<I>, Model);
}
```

```java
// dev.engine_room.flywheel.api.model
public interface Model {
    java.util.List<Model$ConfiguredMesh> meshes();
    org.joml.Vector4fc boundingSphere();
}

public final class Model$ConfiguredMesh extends java.lang.Record {   // 是 record
    Material material();
    Mesh mesh();
}

public interface Mesh {
    int vertexCount();
    void write(MutableVertexList);
    IndexSequence indexSequence();
    int indexCount();
    org.joml.Vector4fc boundingSphere();
}

public interface IndexSequence { void fill(long, int); }
```

### 3.3 dev.engine_room.flywheel.api.vertex：顶点视图

**只读视图**和**可写视图**是两个接口，Mesh#write 收到的是可写那个：

```java
public interface VertexList {                       // 只读
    float x(int); float y(int); float z(int);
    float r(int); float g(int); float b(int); float a(int);
    float u(int); float v(int);
    int overlay(int); int light(int);
    float normalX(int); float normalY(int); float normalZ(int);
    default void write(MutableVertexList, int, int);
    default void write(MutableVertexList, int, int, int);
    default void writeAll(MutableVertexList);
    int vertexCount();
    default boolean isEmpty();
}

public interface MutableVertexList extends VertexList {   // 可写
    void x(int, float); void y(int, float); void z(int, float);
    void r(int, float); void g(int, float); void b(int, float); void a(int, float);
    void u(int, float); void v(int, float);
    void overlay(int, int); void light(int, int);
    void normalX(int, float); void normalY(int, float); void normalZ(int, float);
}
```

dev.engine_room.flywheel.lib.vertex 里是它们的实现：
DefaultVertexList（接口，全部 default 方法）、AbstractVertexView、FullVertexView、
NoOverlayVertexView、PosTexNormalVertexView、PosVertexView、VertexView、VertexTransformations。

最常用的是 VertexView（直接指向一块 native 内存）：

```java
public interface VertexView extends MutableVertexList {
    long ptr();
    void ptr(long);
    void vertexCount(int);
    long stride();
    Object nativeMemoryOwner();
    void nativeMemoryOwner(Object);
    default void load(dev.engine_room.flywheel.lib.memory.MemoryBlock);
}
```

### 3.4 dev.engine_room.flywheel.lib.transform：变换助手

这是一套 **CRTP 风格的接口**（Self extends ...<Self>）。TransformedInstance / PosedInstance /
OrientedInstance / PoseTransformStack 都实现了它，所以可以链式写。

```java
public interface Translate<Self extends Translate<Self>> {
    static final float CENTER;
    Self translate(float, float, float);              // 唯一的抽象方法
    default Self translate(double, double, double);
    default Self translate(float);                    // 三轴等量
    default Self translateX(float);
    default Self translateY(float);
    default Self translateZ(float);
    default Self translate(net.minecraft.core.Vec3i);
    default Self translate(org.joml.Vector3ic);
    default Self translate(org.joml.Vector3fc);
    default Self translate(net.minecraft.world.phys.Vec3);
    default Self translateBack(float, float, float);  // 反向平移，绕中心旋转时用
    default Self translateBack(double, double, double);
    default Self translateBack(float);
    default Self translateBack(net.minecraft.core.Vec3i);
    default Self translateBack(org.joml.Vector3ic);
    default Self translateBack(org.joml.Vector3fc);
    default Self translateBack(net.minecraft.world.phys.Vec3);
    default Self center();                            // 等价于 translate(0.5, 0.5, 0.5)
    default Self uncenter();
    default Self nudge(int);
}
```

```java
public interface Rotate<Self extends Rotate<Self>> {
    Self rotate(org.joml.Quaternionfc);               // 唯一的抽象方法
    default Self rotate(org.joml.AxisAngle4f);
    default Self rotate(float, float, float, float);
    default Self rotate(float, com.mojang.math.Axis);
    default Self rotate(float, org.joml.Vector3fc);
    default Self rotate(float, net.minecraft.core.Direction);
    default Self rotate(float, net.minecraft.core.Direction$Axis);
    default Self rotateDegrees(float, float, float, float);
    default Self rotateDegrees(float, com.mojang.math.Axis);
    default Self rotateDegrees(float, org.joml.Vector3fc);
    default Self rotateDegrees(float, net.minecraft.core.Direction);
    default Self rotateDegrees(float, net.minecraft.core.Direction$Axis);
    default Self rotateX(float);      // 弧度
    default Self rotateY(float);
    default Self rotateZ(float);
    default Self rotateXDegrees(float);   // 角度
    default Self rotateYDegrees(float);
    default Self rotateZDegrees(float);
    default Self rotateToFace(net.minecraft.core.Direction);
    default Self rotateTo(float, float, float, float, float, float);
    default Self rotateTo(org.joml.Vector3fc, org.joml.Vector3fc);
    default Self rotateTo(net.minecraft.core.Direction, net.minecraft.core.Direction);
    default Self self();
}
```

```java
public interface Scale<Self extends Scale<Self>> {
    Self scale(float, float, float);                  // 唯一的抽象方法
    default Self scale(float);
    default Self scaleX(float);
    default Self scaleY(float);
    default Self scaleZ(float);
    default Self scale(org.joml.Vector3fc);
}
```

```java
public interface Affine<Self extends Affine<Self>>
        extends Translate<Self>, Rotate<Self>, Scale<Self> {
    default Self rotateAround(org.joml.Quaternionfc, float, float, float);
    default Self rotateAround(org.joml.Quaternionfc, org.joml.Vector3fc);
    default Self rotateCentered(org.joml.Quaternionfc);
    default Self rotateCentered(float, float, float, float);
    default Self rotateCentered(float, com.mojang.math.Axis);
    default Self rotateCentered(float, org.joml.Vector3fc);
    default Self rotateCentered(float, net.minecraft.core.Direction$Axis);
    default Self rotateCentered(float, net.minecraft.core.Direction);
    default Self rotateCenteredDegrees(float, float, float, float);
    default Self rotateCenteredDegrees(float, com.mojang.math.Axis);
    default Self rotateCenteredDegrees(float, org.joml.Vector3fc);
    default Self rotateCenteredDegrees(float, net.minecraft.core.Direction);
    default Self rotateCenteredDegrees(float, net.minecraft.core.Direction$Axis);
    default Self rotateXCentered(float);
    default Self rotateYCentered(float);
    default Self rotateZCentered(float);
    default Self rotateXCenteredDegrees(float);
    default Self rotateYCenteredDegrees(float);
    default Self rotateZCenteredDegrees(float);
}

public interface Transform<Self extends Transform<Self>> extends Affine<Self> {
    Self mulPose(org.joml.Matrix4fc);
    Self mulNormal(org.joml.Matrix3fc);
    default Self transform(org.joml.Matrix4fc, org.joml.Matrix3fc);
    default Self transform(com.mojang.blaze3d.vertex.PoseStack$Pose);
    default Self transform(com.mojang.blaze3d.vertex.PoseStack);
}

public interface TransformStack<Self extends TransformStack<Self>> extends Transform<Self> {
    static PoseTransformStack of(com.mojang.blaze3d.vertex.PoseStack);
    Self pushPose();
    Self popPose();
}
```

PoseTransformStack 是把 PoseStack 包成同一套链式 API 的适配器（PoseTransformStack.of(poseStack)），
另有 public PoseStack unwrap()。

**注意 rotateX/Y/Z 收的是弧度**，rotateXDegrees/YDegrees/ZDegrees 才是角度 ——
这两种在同一个接口里并存，混用是最典型的「转向差 57 倍」错误。

### 3.5 dev.engine_room.flywheel.lib.instance：addon 应该直接用这些

```java
public final class InstanceTypes {
    public static final InstanceType<TransformedInstance> TRANSFORMED;
    public static final InstanceType<PosedInstance>       POSED;
    public static final InstanceType<OrientedInstance>    ORIENTED;
    public static final InstanceType<ShadowInstance>      SHADOW;
    static {};     // 四个常量在静态初始化块里创建
}
```

四个实例的数据形状：

| 类 | 父类 / 接口 | 公开字段 | 适合 |
|---|---|---|---|
| TransformedInstance | ColoredLitOverlayInstance + Affine<TransformedInstance> | public final Matrix4f pose | 任意 4x4 变换，最通用 |
| PosedInstance | ColoredLitOverlayInstance + Transform<PosedInstance> | public final Matrix4f pose；public final Matrix3f normal | 需要法线矩阵（非等比缩放 / 剪切） |
| OrientedInstance | ColoredLitOverlayInstance + Rotate<OrientedInstance> | public float posX/posY/posZ；public float pivotX/pivotY/pivotZ；public final Quaternionf rotation | 位置 + 朝向，数据量最小 |
| ShadowInstance | AbstractInstance | public float x,y,z,entityX,entityZ,sizeX,sizeZ,alpha,radius | 实体影子 |

ColoredLitInstance 提供的公共 API（上面三个 ColoredLit* 子类都能用）：

```java
public byte red, green, blue, alpha;
public int light;

ColoredLitInstance colorArgb(int);
ColoredLitInstance colorRgb(int);
ColoredLitInstance color(int, int, int, int);
ColoredLitInstance color(int, int, int);
ColoredLitInstance color(byte, byte, byte, byte);
ColoredLitInstance color(byte, byte, byte);
ColoredLitInstance color(float, float, float, float);
ColoredLitInstance color(float, float, float);
ColoredLitInstance light(int);
```

ColoredLitOverlayInstance 再追加 public int overlay 和 ColoredLitOverlayInstance overlay(int)。

TransformedInstance 自己的方法（javap 原文，挑常用的）：

```java
TransformedInstance setTransform(org.joml.Matrix4fc);
TransformedInstance setTransform(com.mojang.blaze3d.vertex.PoseStack$Pose);
TransformedInstance setTransform(com.mojang.blaze3d.vertex.PoseStack);
TransformedInstance setIdentityTransform();
TransformedInstance setZeroTransform();
TransformedInstance mul(org.joml.Matrix4fc);
TransformedInstance mul(com.mojang.blaze3d.vertex.PoseStack$Pose);
TransformedInstance mul(com.mojang.blaze3d.vertex.PoseStack);
TransformedInstance rotateAround(org.joml.Quaternionfc, float, float, float);
TransformedInstance rotateCentered(float, float, float, float);
TransformedInstance rotateXCentered(float);
TransformedInstance rotateYCentered(float);
TransformedInstance rotateZCentered(float);
```

OrientedInstance 自己的方法：

```java
OrientedInstance position(float, float, float);
OrientedInstance position(org.joml.Vector3fc);
OrientedInstance position(net.minecraft.core.Vec3i);
OrientedInstance position(net.minecraft.world.phys.Vec3);
OrientedInstance zeroPosition();
OrientedInstance translatePosition(float, float, float);
OrientedInstance pivot(float, float, float);
OrientedInstance pivot(org.joml.Vector3fc);
OrientedInstance pivot(net.minecraft.core.Vec3i);
OrientedInstance pivot(net.minecraft.world.phys.Vec3);
OrientedInstance centerPivot();
OrientedInstance translatePivot(float, float, float);
OrientedInstance rotation(org.joml.Quaternionfc);
OrientedInstance rotation(float, float, float, float);   // 四元数 xyzw
OrientedInstance identityRotation();
```

AbstractInstance（共同父类）提供 public final void setChanged() / delete() / setVisible(boolean)，
以及 type() 与 handle()。反编译后的实现只有三行转发：

```java
public final void setChanged()                { this.handle.setChanged(); }
public final void delete()                    { this.handle.setDeleted(); }
public final void setVisible(boolean visible) { this.handle.setVisible(visible); }
```

也就是说实例本身不持有任何 GPU 状态，真正的连接全在 InstanceHandle 上。
这两件事都**不会**自动发生：改了字段要自己 setChanged()，不用了要自己 delete()。

FlatLit 是「有光照的实例」的标记接口，还带四个静态批量重光照方法：

```java
public interface FlatLit extends Instance {
    FlatLit light(int);
    default FlatLit light(int, int);
    static void relight(int, FlatLit...);
    static void relight(int, java.util.Iterator<FlatLit>);
    static void relight(int, java.lang.Iterable<FlatLit>);
    static void relight(int, java.util.stream.Stream<FlatLit>);
}
```

### 3.6 模型与材质助手

```java
// dev.engine_room.flywheel.lib.model.baked.PartialModel —— 加载一个 .json 模型
public final class PartialModel {
    public static PartialModel of(net.minecraft.resources.ResourceLocation);
    public net.minecraft.client.resources.model.BakedModel get();
    public net.minecraft.resources.ResourceLocation modelLocation();
}

// dev.engine_room.flywheel.lib.model.Models —— 四个工厂
public static Model block(net.minecraft.world.level.block.state.BlockState);
public static Model partial(PartialModel);
public static <T> Model partial(PartialModel, T, java.util.function.BiConsumer<T, PoseStack>);
public static Model partial(PartialModel, net.minecraft.core.Direction);

// dev.engine_room.flywheel.lib.material.Materials —— 现成的 Material 常量
public static final Material SOLID_BLOCK, SOLID_UNSHADED_BLOCK,
        CUTOUT_MIPPED_BLOCK, CUTOUT_MIPPED_UNSHADED_BLOCK,
        CUTOUT_BLOCK, CUTOUT_UNSHADED_BLOCK,
        TRANSLUCENT_BLOCK, TRANSLUCENT_UNSHADED_BLOCK,
        TRIPWIRE_BLOCK, TRIPWIRE_UNSHADED_BLOCK,
        GLINT, GLINT_ENTITY, TRANSLUCENT_ENTITY;
```

需要自己拼模型时用 BakedModelBuilder / BlockModelBuilder / ForgeBlockModelBuilder
（都在 lib.model.baked），它们是 create(...) + 链式 + build() 返回 SimpleModel：

```java
public static BakedModelBuilder create(net.minecraft.client.resources.model.BakedModel);
public BakedModelBuilder level(net.minecraft.world.level.BlockAndTintGetter);
public BakedModelBuilder pos(net.minecraft.core.BlockPos);
public BakedModelBuilder poseStack(com.mojang.blaze3d.vertex.PoseStack);
public BakedModelBuilder materialFunc(
        java.util.function.BiFunction<net.minecraft.client.renderer.RenderType, java.lang.Boolean, Material>);
public abstract SimpleModel build();

public static BlockModelBuilder create(BlockAndTintGetter, java.lang.Iterable<net.minecraft.core.BlockPos>);
public BlockModelBuilder poseStack(PoseStack);
public BlockModelBuilder renderFluids(boolean);
public BlockModelBuilder materialFunc(BiFunction<RenderType, Boolean, Material>);
public abstract SimpleModel build();
```

ModelUtil 里几个有用的静态方法：

```java
public static Material getMaterial(net.minecraft.client.renderer.RenderType, boolean);
public static Material getItemMaterial(net.minecraft.client.renderer.RenderType);
public static int computeTotalVertexCount(java.lang.Iterable<Mesh>);
public static org.joml.Vector4f computeBoundingSphere(java.util.Collection<Model$ConfiguredMesh>);
public static org.joml.Vector4f computeBoundingSphere(java.lang.Iterable<Mesh>);
public static org.joml.Vector4f computeBoundingSphere(VertexList);
```

### 3.7 自定义 InstanceType（进阶，一般不需要）

```java
public final class SimpleInstanceType<I extends Instance> implements InstanceType<I> {
    static <I extends Instance> Builder<I> builder(SimpleInstanceType$Factory<I>);
}

public final class Builder<I extends Instance> {
    Builder<I> layout(dev.engine_room.flywheel.api.layout.Layout);
    Builder<I> writer(InstanceWriter<I>);
    Builder<I> vertexShader(net.minecraft.resources.ResourceLocation);
    Builder<I> cullShader(net.minecraft.resources.ResourceLocation);
    SimpleInstanceType<I> build();
}
```

Layout 用 LayoutBuilder.create() 构造：

```java
public interface LayoutBuilder {
    static LayoutBuilder create();
    LayoutBuilder scalar(java.lang.String, dev.engine_room.flywheel.api.layout.ValueRepr);
    LayoutBuilder vector(java.lang.String, ValueRepr, int);
    LayoutBuilder matrix(java.lang.String, FloatRepr, int, int);
    LayoutBuilder matrix(java.lang.String, FloatRepr, int);
    LayoutBuilder scalarArray(java.lang.String, ValueRepr, int);
    LayoutBuilder vectorArray(java.lang.String, ValueRepr, int, int);
    LayoutBuilder matrixArray(java.lang.String, FloatRepr, int, int, int);
    LayoutBuilder matrixArray(java.lang.String, FloatRepr, int, int);
    Layout build();
}
```

---

## 4. 常见坑

### 4.1 编译期 classpath 上只有 API jar

见 3.1 的表。要点：

* 能 import：dev.engine_room.flywheel.api.*（63 个类）与 dev.engine_room.flywheel.lib.*（120 个类）。
  API jar 不是只有 api 包，lib 包整个都在里面。
* 不能 import：dev.engine_room.flywheel.backend.*（163）与 dev.engine_room.flywheel.impl.*（74）。
  典型会误用的名字：dev.engine_room.flywheel.backend.engine.EngineImpl、
  dev.engine_room.flywheel.backend.compile.Pipelines、dev.engine_room.flywheel.impl.FlwImpl、
  dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl。**都不在 classpath 上。**
* runtimeOnly 意味着完整 jar 在 runClient / 正式运行时是有的，所以 lib 类的运行期实现来自完整 jar；
  不要试图引用 impl 里的类来「补全」功能。

### 4.2 AbstractBlockEntityVisual 仍然要求你实现 collectCrumblingInstances 和 updateLight

这一条是**编译验证时真的报错**才发现的，最容易踩：

```text
error: MyMachineVisual 不是抽象的, 并且未覆盖 BlockEntityVisual 中的抽象方法 collectCrumblingInstances(Consumer<Instance>)
error: MyMachineVisual 不是抽象的, 并且未覆盖 LightUpdatedVisual 中的抽象方法 updateLight(float)
```

原因是 AbstractBlockEntityVisual 的声明是：

```java
public abstract class AbstractBlockEntityVisual<T extends BlockEntity>
        extends AbstractVisual
        implements BlockEntityVisual<T>, LightUpdatedVisual {
```

它**已经把 setSectionCollector 实现了**，但 BlockEntityVisual#collectCrumblingInstances 和
LightUpdatedVisual#updateLight 没实现，于是全部转嫁给子类。

一个 AbstractBlockEntityVisual 子类的**最小完整实现清单**：

1. protected void _delete()（来自 AbstractVisual，必须）
2. public void collectCrumblingInstances(java.util.function.Consumer<Instance>)（必须）
3. public void updateLight(float)（必须）

AbstractEntityVisual 只需要 _delete()。

### 4.3 两个 Builder 静默陷阱：漏掉 .apply()、以及 skipVanillaRender 的默认值

**（1）漏掉 .apply()**：SimpleBlockEntityVisualizer.builder(type).factory(...) 返回的是 Builder，
**注册发生在 apply() 里**（源码见 2.1）。漏掉最后一个 .apply() 编译器不会报错，
游戏里就是「你的 Visual 从来不跑」。TagBuilder 同理：不调 register() 标签不会存在。

**（2）skipVanillaRender 不写就是 true**。这是反直觉的一条，源码原文（ForgeFlower 反编译
flywheel-forge-api 得到的 SimpleBlockEntityVisualizer$Builder.apply）：

```java
public SimpleBlockEntityVisualizer<T> apply() {
   Objects.requireNonNull(this.visualFactory, "Visual factory cannot be null!");
   if (this.skipVanillaRender == null) {
      this.skipVanillaRender = (blockEntity) -> true;      // ← 默认「跳过原版渲染」
   }
   SimpleBlockEntityVisualizer<T> visualizer =
       new SimpleBlockEntityVisualizer(this.visualFactory, this.skipVanillaRender);
   VisualizerRegistry.setVisualizer(this.type, visualizer);
   return visualizer;
}
```

也就是说：

| 你写的 | 结果 |
|---|---|
| 只 .factory(...) 然后 .apply() | **跳过**原版渲染（默认值就是 true） |
| .skipVanillaRender(be -> true) | 跳过原版渲染 |
| .skipVanillaRender(be -> be.某条件) | 按条件决定 |
| .neverSkipVanillaRender() | 原版模型**照常渲染**（内部是 be -> false） |

所以「想让原版模型也画」不能靠「不写 skipVanillaRender」，必须显式写
**.neverSkipVanillaRender()**。另一半也成立：如果你只想让 Flywheel 画、不画原版，
什么都不写即可，不需要多余的 .skipVanillaRender(be -> true)。

### 4.4 实例泄漏

Instancer#createInstance() 出来的实例**不会**被自动回收，Visual#delete() 也不会替你删。
每个实例都必须在 _delete() 里显式 instance.delete()。
动态数量的实例用 dev.engine_room.flywheel.lib.visual.util.InstanceRecycler<I> 或
SmartRecycler<K, I>，两者都有 resetCount() / get() / discardExtra() / delete()。

### 4.5 改了实例数据一定要 setChanged()

TransformedInstance#pose 是 public final Matrix4f，OrientedInstance#rotation 是
public final Quaternionf —— **你可以直接改它们，Flywheel 不知道**。
所有写操作之后必须调 Instance#setChanged()（AbstractInstance 里是 public final void setChanged()），
否则 GPU 侧数据不会同步。

### 4.6 setChanged() 不等于重新提交

* instance.setChanged()：把**这个实例**的数据重传。
* VisualizationHelper.queueUpdate(blockEntity)：让 Flywheel 重新处理**这个方块实体的 visual**
  （例如方块状态变了、模型要换）。Create 在 KineticBlockEntity 等类里用的就是这个。
* VisualizationHelper.queueAdd / queueRemove(Effect)：特效的增删。

### 4.7 自己复现编译验证

本文档所有 Java 片段都用下面这套办法编译验证过（classpath **故意排除**完整 Flywheel jar，
因为项目 compileOnly 的只有 API jar）。完整脚本见 8.5。

```powershell
$jargs = @('-cp', "$V\libs\*", '-d', "$V\out", '--release', '17',
           '-proc:none', '-nowarn', "$V\FlywheelSnippet.java")
& "$env:JAVA_HOME\bin\javac.exe" @jargs
```

---

# B. Ponder

## 5. Ponder 是什么，以及如何注册场景

Ponder 是 Create 的「游戏内教学场景」系统。一个场景 = **一份结构文件（schematic .nbt）+ 一段 Java 脚本 + 一份语言文件**。
玩家按住 Ponder 键时，客户端在虚拟世界里加载结构，按脚本逐条播放指令（文字、镜头旋转、方块动画、输入提示）。

本项目的 Ponder 来自 Ponder-Forge-1.20.1-1.0.91，它是 implementation 依赖，
**同时内含 Catnip**（net.createmod.catnip.*）—— 所以 net.createmod.catnip.math.Pointing
这类类不用额外声明依赖。

### 5.1 先纠正几个不存在的名字

| 你可能想写的 | 实际存在的类 |
|---|---|
| net.createmod.ponder.PonderRegistry | **不存在**。对应物是 net.createmod.ponder.foundation.PonderIndex（静态门面） |
| net.createmod.ponder.foundation.PonderStoryBoardEntry | 接口叫 net.createmod.ponder.api.registration.StoryBoardEntry，实现类才叫 net.createmod.ponder.foundation.PonderStoryBoardEntry |
| 「PonderSceneBuilder 的 DSL」 | PonderSceneBuilder 实现的是接口 net.createmod.ponder.api.scene.SceneBuilder，**写场景代码时按接口用** |
| 「指令枚举 PonderInput / PonderInstruction」 | 没有指令枚举。指令是 net.createmod.ponder.foundation.instruction.PonderInstruction 的**类层次**，场景里通过 ShowInputInstruction / TextInstruction 等间接使用 |

真实的注册表类有三个：PonderSceneRegistry、PonderTagRegistry、PonderChapterRegistry
（在 net.createmod.ponder.foundation.registration 与 net.createmod.ponder.foundation 下）。

### 5.2 注册入口：PonderIndex.addPlugin

```java
public class net.createmod.ponder.foundation.PonderIndex {
    public static void addPlugin(net.createmod.ponder.api.registration.PonderPlugin);
    public static void forEachPlugin(java.util.function.Consumer<PonderPlugin>);
    public static java.util.stream.Stream<PonderPlugin> streamPlugins();
    public static void reload();
    public static void registerAll();
    public static void gatherSharedText();
    public static SceneRegistryAccess getSceneAccess();
    public static TagRegistryAccess   getTagAccess();
    public static LangRegistryAccess  getLangAccess();
    public static boolean editingModeActive();
}
```

插件接口（除 getModId 外全部有 default 实现，**只需要覆写你用得到的**）：

```java
public interface PonderPlugin {
    String getModId();                                                   // 唯一必须实现的
    default void registerScenes(PonderSceneRegistrationHelper<ResourceLocation>);
    default void registerTags(PonderTagRegistrationHelper<ResourceLocation>);
    default void registerSharedText(SharedTextRegistrationHelper);
    default void onPonderLevelRestore(net.createmod.ponder.api.level.PonderLevel);
    default void indexExclusions(IndexExclusionHelper);
}
```

**在哪里调 addPlugin**：Create 自己的做法（javap -c 实证）是在
com.simibubi.create.CreateClient.clientInit(FMLClientSetupEvent) 里调：

```text
com/simibubi/create/CreateClient.class:
  public static void clientInit(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent);
    58: invokestatic  // Method net/createmod/ponder/foundation/PonderIndex
                      //        .addPlugin:(Lnet/createmod/ponder/api/registration/PonderPlugin;)V
```

所以照做即可：**在 mod 的客户端初始化（FMLClientSetupEvent）里
PonderIndex.addPlugin(new MyPonderPlugin())。**

### 5.3 场景注册助手

```java
public interface PonderSceneRegistrationHelper<T> {
    <S> PonderSceneRegistrationHelper<S> withKeyFunction(java.util.function.Function<S, T>);
    StoryBoardEntry addStoryBoard(T, net.minecraft.resources.ResourceLocation, PonderStoryBoard, ResourceLocation...);
    StoryBoardEntry addStoryBoard(T, java.lang.String, PonderStoryBoard, ResourceLocation...);
    MultiSceneBuilder forComponents(T...);
    MultiSceneBuilder forComponents(java.lang.Iterable<? extends T>);
    ResourceLocation asLocation(java.lang.String);
}

public interface MultiSceneBuilder {
    MultiSceneBuilder addStoryBoard(ResourceLocation, PonderStoryBoard);
    MultiSceneBuilder addStoryBoard(ResourceLocation, PonderStoryBoard, ResourceLocation...);
    MultiSceneBuilder addStoryBoard(ResourceLocation, PonderStoryBoard, java.util.function.Consumer<StoryBoardEntry>);
    MultiSceneBuilder addStoryBoard(String, PonderStoryBoard);
    MultiSceneBuilder addStoryBoard(String, PonderStoryBoard, ResourceLocation...);
    MultiSceneBuilder addStoryBoard(String, PonderStoryBoard, java.util.function.Consumer<StoryBoardEntry>);
}
```

```java
public interface StoryBoardEntry {
    PonderStoryBoard getBoard();
    String getNamespace();
    ResourceLocation getSchematicLocation();
    ResourceLocation getComponent();
    java.util.List<ResourceLocation> getTags();
    java.util.List<StoryBoardEntry$SceneOrderingEntry> getOrderingEntries();
    default StoryBoardEntry orderBefore(String);
    StoryBoardEntry orderBefore(String, String);
    default StoryBoardEntry orderAfter(String);
    StoryBoardEntry orderAfter(String, String);
    StoryBoardEntry highlightTag(ResourceLocation);
    StoryBoardEntry highlightTags(ResourceLocation...);
    StoryBoardEntry highlightAllTags();
}

public interface PonderStoryBoard {
    void program(net.createmod.ponder.api.scene.SceneBuilder,
                 net.createmod.ponder.api.scene.SceneBuildingUtil);
}
```

PonderStoryBoard 是**函数式接口**，所以 MyScenes::intro 可以直接当参数传。

**结构文件的位置**（字节码实证：PonderSceneRegistry.loadSchematic 里的拼接常量是 ponder/ + path + .nbt）：

```text
src/main/resources/assets/<你的modid>/ponder/<schematicPath>.nbt
```

addStoryBoard(component, "my_machine/intro", ...) 会去读
assets/<modid>/ponder/my_machine/intro.nbt。文件缺失时 Ponder 只打一条
「Ponder schematic missing: ...」的错误日志然后给一个空结构，**不会崩**，但场景是空的。
Create 的 jar 里有 178 个这样的 .nbt，可以作为格式参考。

### 5.4 标签注册助手

```java
public interface PonderTagRegistrationHelper<T> {
    <S> PonderTagRegistrationHelper<S> withKeyFunction(java.util.function.Function<S, T>);
    TagBuilder registerTag(ResourceLocation);
    TagBuilder registerTag(String);
    void addTagToComponent(T, ResourceLocation);
    MultiTagBuilder$Tag<T> addToTag(ResourceLocation);
    MultiTagBuilder$Tag<T> addToTag(ResourceLocation...);
    MultiTagBuilder$Component addToComponent(T);
    MultiTagBuilder$Component addToComponent(T...);
}

public interface TagBuilder {
    TagBuilder title(String);
    TagBuilder description(String);
    TagBuilder addToIndex();
    TagBuilder icon(ResourceLocation);
    TagBuilder icon(String);
    TagBuilder idAsIcon();
    TagBuilder item(net.minecraft.world.level.ItemLike, boolean, boolean);
    default TagBuilder item(net.minecraft.world.level.ItemLike);
    void register();          // ← 不调 register() 标签不会存在
}

public interface MultiTagBuilder { }                                      // 空标记接口
public interface MultiTagBuilder$Tag<T> { MultiTagBuilder$Tag<T> add(T); }
public interface MultiTagBuilder$Component { MultiTagBuilder$Component add(ResourceLocation); }
```

```java
public interface SharedTextRegistrationHelper {
    void registerSharedText(String, String);
}

public interface IndexExclusionHelper {
    IndexExclusionHelper exclude(net.minecraft.world.level.ItemLike);
    IndexExclusionHelper excludeItemVariants(Class<? extends Item>, Item);
    IndexExclusionHelper excludeBlockVariants(Class<? extends Block>, Block);
    IndexExclusionHelper exclude(java.util.function.Predicate<net.minecraft.world.level.ItemLike>);
}
```

### 5.5 完整的插件骨架（已编译验证）

```java
package com.croety.client.ponder;

import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class CroetyPonderPlugin implements PonderPlugin {

    private static final ResourceLocation MY_MACHINE = new ResourceLocation("croety", "my_machine");
    private static final ResourceLocation TAG_MACHINES = new ResourceLocation("croety", "machines");

    @Override
    public String getModId() {
        return "croety";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        StoryBoardEntry entry = helper.addStoryBoard(
                MY_MACHINE, "my_machine/intro", CroetyPonderScenes::intro, TAG_MACHINES);
        entry.orderBefore("kinetics", "create");
        entry.highlightAllTags();

        // 同一个方块挂多个场景：
        helper.forComponents(MY_MACHINE)
                .addStoryBoard("my_machine/usage", CroetyPonderScenes::usage,
                        e -> e.highlightTag(TAG_MACHINES));
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG_MACHINES)
                .title("Croety Machines")
                .description("Machines added by Croety")
                .item(Items.IRON_INGOT)
                .addToIndex()
                .register();                 // ← 不调 register() 标签就不存在
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("croety.machine.shared", "Shared description");
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        helper.exclude(Items.DIRT);
    }

    @Override
    public void onPonderLevelRestore(PonderLevel level) {
        // 场景世界恢复后的钩子，一般留空
    }

    /** 在 FMLClientSetupEvent 里调用。 */
    public static void register() {
        PonderIndex.addPlugin(new CroetyPonderPlugin());
    }
}
```

### 5.6 Create 侧的注册 API（也已 javap 验证）

```java
// com.simibubi.create.foundation.ponder.CreatePonderPlugin
public class CreatePonderPlugin implements net.createmod.ponder.api.registration.PonderPlugin {
    public String getModId();
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation>);
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation>);
    public void registerSharedText(SharedTextRegistrationHelper);
    public void onPonderLevelRestore(PonderLevel);
    public void indexExclusions(IndexExclusionHelper);
}

// com.simibubi.create.infrastructure.ponder.AllCreatePonderScenes
public static void register(PonderSceneRegistrationHelper<ResourceLocation>);

// com.simibubi.create.foundation.ponder.CreateSceneBuilder
public class CreateSceneBuilder extends net.createmod.ponder.foundation.PonderSceneBuilder {
    public CreateSceneBuilder(net.createmod.ponder.api.scene.SceneBuilder);
    public CreateSceneBuilder$WorldInstructions   world();
    public CreateSceneBuilder$EffectInstructions  effects();
    public CreateSceneBuilder$SpecialInstructions special();
}
```

CreateSceneBuilder 的作用是**在 Ponder 原生 DSL 上多加一层 Create 专属指令**，
构造时把一个已有的 SceneBuilder 包进去（所以在 PonderStoryBoard#program 的第一行 new 出来即可）。
它多出来的方法（javap 原文，节选）：

```java
// CreateSceneBuilder$WorldInstructions
void rotateBearing(BlockPos, float, int);
void movePulley(BlockPos, float, int);
void animateBogey(BlockPos, float, int);
void moveDeployer(BlockPos, float, int);
void createItemOnBeltLike(BlockPos, Direction, ItemStack);
ElementLink<BeltItemElement> createItemOnBelt(BlockPos, Direction, ItemStack);
void removeItemsFromBelt(BlockPos);
void stallBeltItem(ElementLink<BeltItemElement>, boolean);
void changeBeltItemTo(ElementLink<BeltItemElement>, ItemStack);
void setKineticSpeed(Selection, float);
void multiplyKineticSpeed(Selection, float);
void modifyKineticSpeed(Selection, java.util.function.UnaryOperator<Float>);
void propagatePipeChange(BlockPos);
void setFilterData(Selection, Class<? extends BlockEntity>, ItemStack);
void instructArm(BlockPos, ArmBlockEntity$Phase, ItemStack, int);
void flapFunnel(BlockPos, boolean);
void setCraftingResult(BlockPos, ItemStack);
void connectCrafterInvs(BlockPos, BlockPos);
void toggleControls(BlockPos);
void animateTrainStation(BlockPos, boolean);
void conductorBlaze(BlockPos, boolean);
void changeSignalState(BlockPos, SignalBlockEntity$SignalState);
void setDisplayBoardText(BlockPos, int, net.minecraft.network.chat.Component);
void dyeDisplayBoard(BlockPos, int, net.minecraft.world.item.DyeColor);
void flashDisplayLink(BlockPos);

// CreateSceneBuilder$EffectInstructions
void superGlue(BlockPos, Direction, boolean);
void rotationSpeedIndicator(BlockPos);
void rotationDirectionIndicator(BlockPos);

// CreateSceneBuilder$SpecialInstructions
ElementLink<ParrotElement> createBirb(Vec3, java.util.function.Supplier<? extends ParrotPose>);
ElementLink<ParrotElement> birbOnTurntable(BlockPos);
ElementLink<ParrotElement> birbOnSpinnyShaft(BlockPos);
void conductorBirb(ElementLink<ParrotElement>, boolean);
```

同包下还有 com.simibubi.create.foundation.ponder.element.BeltItemElement，
以及 com.simibubi.create.infrastructure.ponder.scenes.*（Create 自己那 50 多个场景类，可作写法参考）。

---

## 6. 编写一个 Ponder 场景

### 6.1 SceneBuilder：场景级控制（含镜头与时间轴）

```java
public interface SceneBuilder {
    OverlayInstructions  overlay();
    WorldInstructions    world();
    DebugInstructions    debug();
    EffectInstructions   effects();
    SpecialInstructions  special();
    net.createmod.ponder.foundation.PonderScene getScene();

    void title(java.lang.String sceneId, java.lang.String title);
    void configureBasePlate(int xOffset, int zOffset, int basePlateSize);
    void showBasePlate();
    void scaleSceneView(float factor);
    void removeShadow();
    void setSceneOffsetY(float yOffset);

    void addInstruction(net.createmod.ponder.foundation.instruction.PonderInstruction);
    void addInstruction(java.util.function.Consumer<PonderScene>);

    void idle(int ticks);
    void idleSeconds(int seconds);
    void markAsFinished();
    void setNextUpEnabled(boolean isEnabled);
    void rotateCameraY(float degrees);
    void addKeyframe();
    void addLazyKeyframe();
}
```

实现类是 net.createmod.ponder.foundation.PonderSceneBuilder
（构造函数 PonderSceneBuilder(PonderScene)），五个 xxx() 分别返回五个内部类：
PonderSceneBuilder$PonderOverlayInstructions、$PonderWorldInstructions、
$PonderDebugInstructions、$PonderEffectInstructions、$PonderSpecialInstructions。
**你几乎永远不该 new 它** —— 它由 Ponder 在编译场景时创建并传进 PonderStoryBoard#program。

几个语义（字节码实证）：

* rotateCameraY(float degrees) 的实现是 addInstruction(new RotateSceneInstruction(0, degrees, true))，
  参数是**角度（度）**。接口注释：「Pans the scene's camera view around the vertical axis by the given amount」。
* idle(int ticks) 的实现是 addInstruction(new DelayInstruction(ticks))；
  idleSeconds(int seconds) 就是 idle(seconds * 20)，按 20 tick/秒换算。
  接口注释特别强调：「Idle does not stall any animations, only schedules a time gap between
  instructions」—— 它**不会**冻结动画，只是排一个时间空档。
* addKeyframe() 追加 KeyframeInstruction.IMMEDIATE，addLazyKeyframe() 追加 KeyframeInstruction.DELAYED。
  接口注释：「Adds a Key Frame at the end of the last delay() instruction for the users to skip to」。
* markAsFinished() 追加 MarkAsFinishedInstruction。接口注释说明它在 storyboard 结束时会自动发生，
  提前调用只是为了跳过多余文字窗口的等待；目前只影响 UI 里 next scene 按钮的闪烁。
* setNextUpEnabled(boolean isEnabled) 的实现是 addInstruction(scene -> scene.setNextUpEnabled(isEnabled))。
* configureBasePlate(xOffset, zOffset, basePlateSize) 只写三个字段；真正把基座画出来的是
  showBasePlate()。它的实现原文是：

```java
public void showBasePlate() {
    world.showSection(scene.getSceneBuildingUtil().select().cuboid(
        new BlockPos(scene.getBasePlateOffsetX(), 0, scene.getBasePlateOffsetZ()),
        new Vec3i(scene.getBasePlateSize() - 1, 0, scene.getBasePlateSize() - 1)), Direction.UP);
}
```

  注意两点：用的是 cuboid 而不是 fromTo；方向是 Direction.UP 而不是 DOWN
  （Direction 参数是淡入方向，见 6.3）。
* title(String sceneId, String title) 的实现原文是：

```java
public void title(String sceneId, String title) {
    scene.sceneId = new ResourceLocation(scene.getNamespace(), sceneId);
    scene.localization.registerSpecific(scene.sceneId, PonderScene.TITLE_KEY, title);
}
```

  接口注释：「Assign a unique translation key, as well as the standard english translation for this
  scene's title using this method, anywhere inside the program function. @param sceneId unique ID for
  this scene, used as a prefix for translation entries」。
  **所以第一个参数是 sceneId（翻译键前缀），不是 schematic 路径**——schematic 路径是在
  addStoryBoard(...) 里给的。两者惯例写成一样，但概念上独立。
  如果整个场景都没调 title()，PonderScene 会把 sceneId 默认成
  new ResourceLocation(namespace, "missing_title")，于是语言键全变成
  <modid>.ponder.missing_title.*。

### 6.2 SceneBuildingUtil：坐标与选区

```java
public interface SceneBuildingUtil {
    SelectionUtil select();
    VectorUtil    vector();
    PositionUtil  grid();
}

public interface SelectionUtil {
    Selection everywhere();
    Selection position(int, int, int);
    Selection position(net.minecraft.core.BlockPos);
    Selection fromTo(int, int, int, int, int, int);
    Selection fromTo(net.minecraft.core.BlockPos, net.minecraft.core.BlockPos);
    Selection column(int, int);
    Selection layer(int);
    Selection layersFrom(int);
    Selection layers(int, int);
    Selection cuboid(net.minecraft.core.BlockPos, net.minecraft.core.Vec3i);
}

public interface VectorUtil {
    net.minecraft.world.phys.Vec3 centerOf(int, int, int);
    net.minecraft.world.phys.Vec3 centerOf(net.minecraft.core.BlockPos);
    net.minecraft.world.phys.Vec3 topOf(int, int, int);
    net.minecraft.world.phys.Vec3 topOf(net.minecraft.core.BlockPos);
    net.minecraft.world.phys.Vec3 blockSurface(net.minecraft.core.BlockPos, net.minecraft.core.Direction);
    net.minecraft.world.phys.Vec3 blockSurface(net.minecraft.core.BlockPos, net.minecraft.core.Direction, float);
    net.minecraft.world.phys.Vec3 of(double, double, double);
}

public interface PositionUtil {
    net.minecraft.core.BlockPos at(int, int, int);
    net.minecraft.core.BlockPos zero();
}
```

```java
public interface Selection extends java.lang.Iterable<net.minecraft.core.BlockPos>,
                                    java.util.function.Predicate<net.minecraft.core.BlockPos> {
    Selection add(Selection);
    Selection substract(Selection);            // 注意是 substract，不是 subtract
    Selection copy();
    net.minecraft.world.phys.Vec3 getCenter();
    net.createmod.catnip.outliner.Outline$OutlineParams makeOutline(
            net.createmod.catnip.outliner.Outliner, java.lang.Object);
    default net.createmod.catnip.outliner.Outline$OutlineParams makeOutline(
            net.createmod.catnip.outliner.Outliner);
}
```

### 6.3 五个指令组

**OverlayInstructions —— 文字、描边、输入提示**（最常用）

```java
public interface OverlayInstructions {
    TextElementBuilder  showText(int duration);
    TextElementBuilder  showOutlineWithText(Selection selection, int duration);
    InputElementBuilder showControls(net.minecraft.world.phys.Vec3 sceneSpace,
                                     net.createmod.catnip.math.Pointing direction, int duration);
    void chaseBoundingBoxOutline(PonderPalette, java.lang.Object, net.minecraft.world.phys.AABB, int);
    void showCenteredScrollInput(net.minecraft.core.BlockPos, net.minecraft.core.Direction, int);
    void showScrollInput(net.minecraft.world.phys.Vec3, net.minecraft.core.Direction, int);
    void showRepeaterScrollInput(net.minecraft.core.BlockPos, int);
    void showFilterSlotInput(net.minecraft.world.phys.Vec3, int);
    void showFilterSlotInput(net.minecraft.world.phys.Vec3, net.minecraft.core.Direction, int);
    void showLine(PonderPalette, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, int);
    void showBigLine(PonderPalette, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, int);
    void showOutline(PonderPalette, java.lang.Object, Selection, int);
}
```

TextElementBuilder 的链式 API：

```java
public interface TextElementBuilder {
    TextElementBuilder colored(PonderPalette color);
    TextElementBuilder pointAt(net.minecraft.world.phys.Vec3 vec);
    TextElementBuilder independent(int y);
    default TextElementBuilder independent();
    TextElementBuilder text(String defaultText);
    TextElementBuilder text(String defaultText, Object... params);
    TextElementBuilder sharedText(net.minecraft.resources.ResourceLocation);
    TextElementBuilder sharedText(ResourceLocation, Object...);
    TextElementBuilder sharedText(String);
    TextElementBuilder sharedText(String, Object...);
    TextElementBuilder placeNearTarget();
    TextElementBuilder attachKeyFrame();
}
```

InputElementBuilder 的链式 API：

```java
public interface InputElementBuilder {
    InputElementBuilder withItem(net.minecraft.world.item.ItemStack);
    InputElementBuilder leftClick();
    InputElementBuilder rightClick();
    InputElementBuilder scroll();
    InputElementBuilder showing(net.createmod.catnip.gui.element.ScreenElement);
    InputElementBuilder whileSneaking();
    InputElementBuilder whileCTRL();
}
```

**WorldInstructions —— 方块、结构、实体的变化**（完整表）

```java
// 结构（Section）。注意 Direction 参数的名字是 fadeInDirection / fadeOutDirection：
// 它表示方块从哪个方向淡入/淡出，不是"区块朝向"。
// showSection(sel, Direction.DOWN) —— 方块从下往上长出来
// showSection(sel, Direction.UP)   —— 方块从上往下落下来（showBasePlate 用的就是这个）
void showSection(Selection selection, Direction fadeInDirection);
void showSectionAndMerge(Selection selection, Direction fadeInDirection, ElementLink<WorldSectionElement> link);
void glueBlockOnto(BlockPos position, Direction fadeInDirection, ElementLink<WorldSectionElement> link);
ElementLink<WorldSectionElement> showIndependentSection(Selection, Direction);
ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection);
void hideSection(Selection selection, Direction fadeOutDirection);
void hideIndependentSection(ElementLink<WorldSectionElement> link, Direction fadeOutDirection);
ElementLink<WorldSectionElement> makeSectionIndependent(Selection);
void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation,
                   double zRotation, int duration);
void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor);
void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor);
void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration);

// 方块
// 最后的 boolean 参数名是 spawnParticles
void setBlock(BlockPos pos, BlockState state, boolean spawnParticles);
void setBlocks(Selection selection, BlockState state, boolean spawnParticles);
void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles);
void modifyBlock(BlockPos pos, java.util.function.UnaryOperator<BlockState> stateFunc, boolean spawnParticles);
void modifyBlocks(Selection selection, java.util.function.UnaryOperator<BlockState> stateFunc, boolean spawnParticles);
void cycleBlockProperty(BlockPos pos, net.minecraft.world.level.block.state.properties.Property<?> property);
void destroyBlock(BlockPos pos);
void restoreBlocks(Selection selection);
void incrementBlockBreakingProgress(BlockPos pos);
void toggleRedstonePower(Selection selection);

// 实体
<T extends net.minecraft.world.entity.Entity> void modifyEntities(Class<T>, java.util.function.Consumer<T>);
<T extends Entity> void modifyEntitiesInside(Class<T>, Selection, java.util.function.Consumer<T>);
void modifyEntity(ElementLink<EntityElement> link, java.util.function.Consumer<Entity> entityCallBack);
ElementLink<EntityElement> createEntity(java.util.function.Function<Level, Entity> factory);
ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack);

// 方块实体
<T extends net.minecraft.world.level.block.entity.BlockEntity>
        void modifyBlockEntity(BlockPos, Class<T>, java.util.function.Consumer<T>);
void modifyBlockEntityNBT(Selection, Class<? extends BlockEntity>,
                          java.util.function.Consumer<net.minecraft.nbt.CompoundTag>);
void modifyBlockEntityNBT(Selection, Class<? extends BlockEntity>,
                          java.util.function.Consumer<CompoundTag>, boolean);
```

**EffectInstructions**

```java
void emitParticles(Vec3 location, ParticleEmitter emitter, float amountPerCycle, int cycles);
<T extends net.minecraft.core.particles.ParticleOptions> ParticleEmitter simpleParticleEmitter(T, Vec3);
<T extends ParticleOptions> ParticleEmitter particleEmitterWithinBlockSpace(T, Vec3);
void indicateRedstone(BlockPos);
void indicateSuccess(BlockPos);
void createRedstoneParticles(BlockPos, int, int);
```

**SpecialInstructions**

```java
ElementLink<ParrotElement> createBirb(Vec3, java.util.function.Supplier<? extends ParrotPose>);
void changeBirbPose(ElementLink<ParrotElement>, java.util.function.Supplier<? extends ParrotPose>);
void movePointOfInterest(Vec3);
void movePointOfInterest(BlockPos);
void rotateParrot(ElementLink<ParrotElement>, double, double, double, int);
void moveParrot(ElementLink<ParrotElement>, Vec3, int);
ElementLink<MinecartElement> createCart(Vec3, float, MinecartElement$MinecartConstructor);
void rotateCart(ElementLink<MinecartElement>, float, int);
void moveCart(ElementLink<MinecartElement>, Vec3, int);
<T extends AnimatedSceneElement> void hideElement(ElementLink<T>, Direction);
```

**DebugInstructions**

```java
void debugSchematic();
void addInstructionInstance(net.createmod.ponder.foundation.instruction.PonderInstruction);
void enqueueCallback(java.util.function.Consumer<PonderScene>);
```

**PonderPalette** 的枚举常量：
WHITE、BLACK、RED、GREEN、BLUE、SLOW、MEDIUM、FAST、INPUT、OUTPUT，
另有 public int getColor() 与 public net.createmod.catnip.theme.Color getColorObject()。

### 6.4 指令的底层类型（一般不用直接碰）

```java
public abstract class PonderInstruction {
    public boolean isBlocking();
    public void reset(PonderScene);
    public abstract boolean isComplete();
    public void onScheduled(PonderScene);
    public abstract void tick(PonderScene);
    public static PonderInstruction simple(java.util.function.Consumer<PonderScene>);
}

public abstract class TickingInstruction extends PonderInstruction {
    protected int totalTicks;
    protected int remainingTicks;
    public TickingInstruction(boolean, int);
}

public abstract class FadeInOutInstruction extends TickingInstruction {
    protected static final int fadeTime;
    public FadeInOutInstruction(int);
}
```

FadeInOutInstruction 的构造字节码是 super(false, duration + 10) ——
**这就是 showText(int) 那个 int 的真实含义：显示时长（tick），额外加 10 tick 淡入淡出**。详见 7.3。

具体指令类（net.createmod.ponder.foundation.instruction）：
DelayInstruction、KeyframeInstruction（常量 IMMEDIATE / DELAYED）、TextInstruction、
ShowInputInstruction、RotateSceneInstruction、MovePoiInstruction、HighlightValueBoxInstruction、
OutlineSelectionInstruction、DisplayWorldSectionInstruction、AnimateWorldSectionInstruction、
AnimateElementInstruction、ReplaceBlocksInstruction、EmitParticlesInstruction、
MarkAsFinishedInstruction、HideAllInstruction、LineInstruction、ChaseAABBInstruction、
BlockEntityDataInstruction、CreateParrotInstruction / AnimateParrotInstruction、
CreateMinecartInstruction / AnimateMinecartInstruction、WorldModifyInstruction、
FadeIntoSceneInstruction / FadeOutOfSceneInstruction。

### 6.5 完整最小场景（已编译验证）

```java
package com.croety.client.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class CroetyPonderScenes {

    private CroetyPonderScenes() {}

    /**
     * 对应 assets/croety/ponder/my_machine/intro.nbt
     * 与语言文件里的 croety.ponder.my_machine.intro.header / .text_1 / .text_2 / .text_3 ...
     */
    public static void intro(SceneBuilder scene, SceneBuildingUtil util) {
        // 1. 标题：第一个参数是 sceneId（语言键前缀），第二个是默认英文标题。
        //    惯例上 sceneId 与 schematic 路径写成一样，但它们是两个独立的东西：
        //    schematic 路径在 addStoryBoard(...) 里给，sceneId 只用来拼翻译键，见 7.1。
        scene.title("my_machine/intro", "My Machine");

        // 2. 基座：5x5，偏移 (0,0)
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // 3. 选区与坐标
        Selection machine = util.select().position(2, 1, 2);
        BlockPos machinePos = util.grid().at(2, 1, 2);
        Vec3 top = util.vector().topOf(machinePos);

        // 4. 让机器从下方升起来（Direction 是淡入方向：DOWN = 从下往上）
        scene.world().showSection(machine, Direction.DOWN);
        scene.idle(20);

        // 5. 第一条文字。60 是显示时长（tick），不是语言键序号！见 7.3
        scene.overlay().showText(60)
                .text("This is my machine")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        // 6. 绿色描边 + 镜头旋转
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), machine, 40);
        scene.rotateCameraY(45);
        scene.idle(20);

        // 7. 输入提示（Pointing 来自 Catnip，Ponder 自带）
        scene.overlay().showControls(top, net.createmod.catnip.math.Pointing.DOWN, 20)
                .rightClick()
                .withItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        scene.idle(30);

        // 8. 特效
        scene.effects().indicateSuccess(machinePos);
        scene.idle(20);

        scene.markAsFinished();
    }

    /** 用 Create 的扩展指令时，把 scene 包一层即可。 */
    public static void kinetic(SceneBuilder builder, SceneBuildingUtil util) {
        com.simibubi.create.foundation.ponder.CreateSceneBuilder scene =
                new com.simibubi.create.foundation.ponder.CreateSceneBuilder(builder);

        scene.title("my_machine/kinetic", "Spinning Up My Machine");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        Selection shaft = util.select().fromTo(0, 1, 2, 4, 1, 2);
        BlockPos cog = util.grid().at(2, 1, 2);

        scene.world().showSection(shaft, Direction.DOWN);
        scene.world().setKineticSpeed(shaft, 32);        // ← Create 专属
        scene.effects().rotationSpeedIndicator(cog);     // ← Create 专属
        scene.idle(20);
        scene.rotateCameraY(45);
        scene.markAsFinished();
    }
}
```

### 6.6 场景运行时的查询 API（PonderScene）

```java
// net.createmod.ponder.foundation.PonderScene —— 只挑 addon 可能用到的
public static final String TITLE_KEY = "header";
public SceneBuilder builder();
public SceneBuildingUtil getSceneBuildingUtil();
public String getTitle();
public String getString(String);
public java.util.function.Supplier<String> registerText(String);
public java.util.function.Supplier<String> registerText(String, Object...);
public PonderLevel getWorld();
public String getNamespace();
public ResourceLocation getLocation();
public ResourceLocation getId();
public int getKeyframeCount();
public int getKeyframeTime(int);
public int getTotalTime();
public int getCurrentTime();
public void setPointOfInterest(net.minecraft.world.phys.Vec3);
public net.minecraft.world.phys.Vec3 getPointOfInterest();
public void seekToTime(int);
public void addToSceneTime(int);
public void markKeyframe(int);
public <E extends PonderElement> E resolve(ElementLink<E>);
public <T extends PonderElement> void forEach(Class<T>, java.util.function.Consumer<T>);
public <T extends net.minecraft.world.entity.Entity> void forEachWorldEntity(Class<T>, java.util.function.Consumer<T>);
public java.util.Set<PonderElement> getElements();
public net.minecraft.world.level.levelgen.structure.BoundingBox getBounds();
```

PonderLevel（net.createmod.ponder.api.level.PonderLevel）继承
net.createmod.catnip.levelWrappers.SchematicLevel，常用方法：
createBackup()、restore()、restoreBlocks(Selection)、setMask(Selection)、clearMask()、
pushFakeLight(int)、popLight()、setBlockBreakingProgress(BlockPos, int)。

---

## 7. 本地化

### 7.1 键的格式（从字节码里的字符串拼接常量提取）

PonderLocalization 的 4 个 langKeyFor* 方法用的是 invokedynamic 字符串拼接，
常量池里的模板是（javap -v 的 BootstrapMethods 段）：

```text
langKeyForShared        :  \u0001.ponder.shared.\u0001
langKeyForTag           :  \u0001.ponder.tag.\u0001
langKeyForTagDescription:  \u0001.ponder.tag.\u0001.description
langKeyForSpecific      :  \u0001.ponder.\u0001.\u0001
provideLang 的键前缀     :  ponder.\u0001
```

（\u0001 是每个参数的位置。）换算成实际的键：

| 用途 | 键 |
|---|---|
| 场景标题（title） | <modid>.ponder.<schematic路径>.header |
| 场景文本（text） | <modid>.ponder.<schematic路径>.text_N |
| 共享文本（sharedText） | <modid>.ponder.shared.<路径> |
| 标签名（TagBuilder#title） | <modid>.ponder.tag.<标签路径> |
| 标签描述（TagBuilder#description） | <modid>.ponder.tag.<标签路径>.description |

两个常量（javap -constants）：

```java
PonderLocalization.LANG_PREFIX = "ponder."
PonderLocalization.UI_PREFIX   = "ui."
PonderScene.TITLE_KEY          = "header"
```

<schematic路径> 就是 addStoryBoard(component, "my_machine/intro", ...) 里的那个字符串，
最后一段不带 .nbt。

真实例子（Create 的 assets/create/lang/en_us.json，共 1053 个 create.ponder.* 键）：

```json
"create.ponder.analog_lever.header": "Controlling signals using the Analog Lever",
"create.ponder.analog_lever.text_1": "Analog Levers make for a compact and precise source of redstone power",
"create.ponder.analog_lever.text_2": "Right-click to increase its analog power output",
"create.ponder.analog_lever.text_3": "Right-click while Sneaking to decrease the power output again"
```

### 7.2 text_N 的 N 从哪来

PonderScene 的原始源码（Ponder sources jar）第 403 行起：

```java
private int textIndex;                       // 字段声明

public Supplier<String> registerText(String defaultText) {
    final String key = "text_" + textIndex;
    localization.registerSpecific(sceneId, key, defaultText);
    Supplier<String> supplier = () -> localization.getSpecific(sceneId, key);
    textIndex++;
    return supplier;
}
```

textIndex 的初值在 PonderScene 的**构造函数**里：textIndex = 1;。
而 TextElementBuilder#text(String defaultText) 的实现只是 scene.registerText(defaultText)
并把结果存进 textGetter。接口里那个参数本来就叫 defaultText —— 它只是**默认/英文文案**，
真正显示的内容永远从语言文件按 text_N 取。

**结论：text_N 的 N 是「本场景内 .text(...) / .sharedText(...) 的调用次序」，从 1 开始。
它和 showText(int) 传的那个 int 没有任何关系。**

用 Create 的 analog_lever 场景做了交叉验证：反编译 RedstoneScenes 后，
三个 .text("...") 的出现次序与语言文件里的 text_1 / text_2 / text_3 完全一致，
第三句正是 "Right-click while Sneaking to decrease the power output again"。

### 7.3 最大的坑：showText(int) 的 int 不是语言键

OverlayInstructions.showText(int) 的参数一路传到：

```text
TextInstruction(TextWindowElement, int)
  → FadeInOutInstruction(int)         // 字节码: super(false, duration + 10)
  → TickingInstruction(boolean, int)  // totalTicks / remainingTicks
```

所以它是**淡入淡出加显示的 tick 数**。Create 的写法都是 showText(60) / showText(80) 这种量级。
写成 showText(1) 的话文字只存在 11 tick；而且**语言键仍然按 .text() 的调用顺序生成**，
不会因为传了 1 就变成 text_1。

### 7.4 Ponder 自带的 UI 文本

Ponder jar 里有 assets/ponder/lang/en_us.json（另有 30 多种语言）。
它包含的全部 UI 键前缀是 ponder.ui.*，例如：

```json
"ponder.ui.hold_to_ponder": "Hold [%1$s] to Ponder",
"ponder.ui.pondering": "Pondering about...",
"ponder.ui.next": "Next Scene",
"ponder.ui.replay": "Replay",
"ponder.ui.exit": "Exit",
"ponder.ui.index_title": "Ponder Index",
"ponder.ponder.shared.sneak_and": "Sneak +",
"ponder.ponder.shared.ctrl_and": "Ctrl +",
"key.ponder.ponder": "Ponder",
"key.categories.ponder": "Ponder"
```

**未验证**：共享文本（sharedText）如果要引用 Ponder 自带的那两条，
按键格式反推 ResourceLocation 应该是 ponder:ponder/shared/sneak_and 之类，
但没有实际跑过 sharedText，**这一条不要照抄**。

### 7.5 文件放哪

```text
src/main/resources/assets/croety/lang/en_us.json     语言文件（手写或 datagen）
src/main/resources/assets/croety/lang/zh_cn.json
src/main/resources/assets/croety/ponder/<路径>.nbt   结构文件
```

PonderIndex.getLangAccess() 返回 LangRegistryAccess，可在运行时取回键值（调试用）：

```java
public interface LangRegistryAccess {
    void provideLang(String, java.util.function.BiConsumer<String, String>);
    String getShared(ResourceLocation);
    String getShared(ResourceLocation, Object...);
    String getTagName(ResourceLocation);
    String getTagDescription(ResourceLocation);
    String getSpecific(ResourceLocation, String);
    String getSpecific(ResourceLocation, String, Object...);
}
```

---

## 8. 验证记录

### 8.1 用到的 jar

```text
FLYWHEEL_FULL = $env:USERPROFILE\.gradle\caches\forge_gradle\deobf_dependencies\dev\engine-room\flywheel\flywheel-forge-1.20.1\1.0.5-264_mapped_parchment_2023.09.03-1.20.1\flywheel-forge-1.20.1-1.0.5-264_mapped_parchment_2023.09.03-1.20.1.jar
FLYWHEEL_API  = $env:USERPROFILE\.gradle\caches\forge_gradle\deobf_dependencies\dev\engine-room\flywheel\flywheel-forge-api-1.20.1\1.0.5-264_mapped_parchment_2023.09.03-1.20.1\flywheel-forge-api-1.20.1-1.0.5-264_mapped_parchment_2023.09.03-1.20.1.jar
PONDER        = $env:USERPROFILE\.gradle\caches\forge_gradle\deobf_dependencies\net\createmod\ponder\Ponder-Forge-1.20.1\1.0.91_mapped_parchment_2023.09.03-1.20.1\Ponder-Forge-1.20.1-1.0.91_mapped_parchment_2023.09.03-1.20.1.jar
CREATE        = $env:USERPROFILE\.gradle\caches\forge_gradle\deobf_dependencies\com\simibubi\create\create-1.20.1\6.0.8-291_mapped_parchment_2023.09.03-1.20.1\create-1.20.1-6.0.8-291_mapped_parchment_2023.09.03-1.20.1-slim.jar
JAVAP         = $env:JAVA_HOME\bin\javap.exe
JAVAC         = $env:JAVA_HOME\bin\javac.exe
```

### 8.2 javap 命令

下面是实际跑过的命令（$JAVAP / $FW_API / $PONDER / $CREATE 为上面的路径）。
**PowerShell 里带 $ 的内部类名必须用单引号**，例如 'Foo$Bar' ——
写成双引号 "Foo$Bar" 的话 $Bar 会被当变量展开成空串。

```powershell
# --- Flywheel: visual 家族 ---
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.Visual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.BlockEntityVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.EntityVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.DynamicVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.TickableVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.EffectVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.Effect
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.LightUpdatedVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.SectionTrackedVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.ShaderLightVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visual.DistanceUpdateLimiter
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.api.visual.DynamicVisual$Context'
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.api.visual.TickableVisual$Context'
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.api.visual.SectionTrackedVisual$SectionCollector'

# --- Flywheel: 可视化注册 ---
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualizationManager
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.api.visualization.VisualizationManager$RenderDispatcher'
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualizationContext
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualizerRegistry
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.EntityVisualizer
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualManager
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualEmbedding
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.visualization.VisualizationLevel

# --- Flywheel: lib 可视化助手（都在 API jar 里） ---
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer$Builder'
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer$Factory'
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer$Builder'
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visualization.VisualizationHelper
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.AbstractVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.AbstractEntityVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.SimpleTickableVisual
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.util.InstanceRecycler
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.visual.util.SmartRecycler

# --- Flywheel: instance / model / vertex / transform / material ---
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.Instance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.InstanceHandle
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.InstanceType
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.InstanceWriter
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.Instancer
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.instance.InstancerProvider
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.model.Model
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.api.model.Model$ConfiguredMesh'
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.model.Mesh
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.model.IndexSequence
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.vertex.VertexList
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.vertex.MutableVertexList
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.vertex.DefaultVertexList
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.vertex.VertexView
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.InstanceTypes
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.AbstractInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.TransformedInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.PosedInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.OrientedInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.ShadowInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.ColoredLitInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.FlatLit
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.instance.SimpleInstanceType
& $JAVAP -cp $FW_API 'dev.engine_room.flywheel.lib.instance.SimpleInstanceType$Builder'
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.Translate
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.Rotate
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.Scale
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.Affine
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.Transform
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.TransformStack
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.transform.PoseTransformStack
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.baked.PartialModel
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.baked.ForgeBlockModelBuilder
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.Models
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.model.ModelUtil
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.material.Materials
& $JAVAP -cp $FW_API dev.engine_room.flywheel.lib.material.SimpleMaterial
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.material.Material
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.layout.Layout
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.layout.LayoutBuilder
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.Flywheel
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.backend.RenderContext
& $JAVAP -cp $FW_API dev.engine_room.flywheel.api.backend.BackendImplemented

# --- Flywheel: 注册动作的字节码证据 ---
& $JAVAP -p -c -cp $FW_API 'dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer$Builder'
#   → apply() 末尾 invokestatic VisualizerRegistry.setVisualizer(BlockEntityType, BlockEntityVisualizer)

# --- Flywheel: API jar vs 完整 jar 的类清单差集 ---
$api  = Get-Content "$env:TEMP\croety-api-recon\flywheelapi-classes.txt"   # 183 行
$full = Get-Content "$env:TEMP\croety-api-recon\flywheel-classes.txt"      # 420 行
Compare-Object $api $full
#   → '=>' 侧 237 条，全部落在 dev.engine_room.flywheel.backend.* (163) 与 ...impl.* (74)
#   → '<=' 侧 0 条（API jar 完全被完整 jar 包含）

# --- Ponder: 注册 ---
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.PonderPlugin
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.PonderTagRegistrationHelper
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.StoryBoardEntry
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.MultiSceneBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.TagBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.SharedTextRegistrationHelper
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.IndexExclusionHelper
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.SceneRegistryAccess
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.TagRegistryAccess
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.LangRegistryAccess
& $JAVAP -cp $PONDER net.createmod.ponder.api.registration.MultiTagBuilder
& $JAVAP -cp $PONDER 'net.createmod.ponder.api.registration.MultiTagBuilder$Tag'
& $JAVAP -cp $PONDER 'net.createmod.ponder.api.registration.MultiTagBuilder$Component'
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.PonderIndex
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.registration.PonderSceneRegistry
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.registration.PonderLocalization
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.PonderScene
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.PonderSceneBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.PonderSceneBuildingUtil
& $JAVAP -cp $PONDER net.createmod.ponder.api.level.PonderLevel

# --- Ponder: 场景 DSL ---
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.SceneBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.SceneBuildingUtil
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.SelectionUtil
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.VectorUtil
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.PositionUtil
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.Selection
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.WorldInstructions
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.OverlayInstructions
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.EffectInstructions
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.SpecialInstructions
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.DebugInstructions
& $JAVAP -cp $PONDER net.createmod.ponder.api.scene.PonderStoryBoard
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.TextElementBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.InputElementBuilder
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.ElementLink
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.PonderElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.WorldSectionElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.ParrotElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.MinecartElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.EntityElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.element.AnimatedSceneElement
& $JAVAP -cp $PONDER net.createmod.ponder.api.PonderPalette
& $JAVAP -cp $PONDER net.createmod.ponder.api.ParticleEmitter

# --- Ponder: 指令类层次 ---
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.PonderInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.TickingInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.FadeInOutInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.TextInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.ShowInputInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.KeyframeInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.DelayInstruction
& $JAVAP -cp $PONDER net.createmod.ponder.foundation.instruction.HighlightValueBoxInstruction

# --- Ponder: 常量值与语言键模板 ---
& $JAVAP -constants -cp $PONDER net.createmod.ponder.foundation.registration.PonderLocalization
& $JAVAP -constants -cp $PONDER net.createmod.ponder.foundation.PonderScene
& $JAVAP -v -p -cp $PONDER net.createmod.ponder.foundation.registration.PonderLocalization
#   BootstrapMethods 段 → \u0001.ponder.shared.\u0001 / \u0001.ponder.tag.\u0001 /
#                          \u0001.ponder.tag.\u0001.description / \u0001.ponder.\u0001
& $JAVAP -v -p -cp $PONDER net.createmod.ponder.foundation.PonderScene
#   BootstrapMethods #13 → text_\u0001
& $JAVAP -v -p -cp $PONDER net.createmod.ponder.foundation.registration.PonderSceneRegistry
#   BootstrapMethods #0  → ponder/\u0001.nbt      ← 结构文件路径
& $JAVAP -p -c -cp $PONDER 'net.createmod.ponder.foundation.element.TextWindowElement$Builder'
#   text(String) → PonderScene.registerText(String)
& $JAVAP -p -c -cp $PONDER net.createmod.ponder.foundation.instruction.FadeInOutInstruction
#   FadeInOutInstruction(int) → TickingInstruction(false, int + 10)
& $JAVAP -p -c -cp $PONDER net.createmod.ponder.foundation.PonderSceneBuilder
#   title / configureBasePlate / showBasePlate / idle / idleSeconds / rotateCameraY /
#   addKeyframe / markAsFinished / scaleSceneView / setSceneOffsetY / removeShadow 的实现

# --- Create 侧 ---
& $JAVAP -cp "$CREATE;$PONDER" com.simibubi.create.foundation.ponder.CreatePonderPlugin
& $JAVAP -cp "$CREATE;$PONDER" com.simibubi.create.foundation.ponder.CreateSceneBuilder
& $JAVAP -cp "$CREATE;$PONDER" 'com.simibubi.create.foundation.ponder.CreateSceneBuilder$WorldInstructions'
& $JAVAP -cp "$CREATE;$PONDER" 'com.simibubi.create.foundation.ponder.CreateSceneBuilder$EffectInstructions'
& $JAVAP -cp "$CREATE;$PONDER" 'com.simibubi.create.foundation.ponder.CreateSceneBuilder$SpecialInstructions'
& $JAVAP -cp "$CREATE;$PONDER" com.simibubi.create.infrastructure.ponder.AllCreatePonderScenes
& $JAVAP -cp "$CREATE;$PONDER" com.simibubi.create.infrastructure.ponder.scenes.TemplateScenes
& $JAVAP -p -c -cp "$CREATE;$PONDER" com.simibubi.create.CreateClient
#   clientInit(FMLClientSetupEvent) 里 invokestatic PonderIndex.addPlugin(PonderPlugin)
& $JAVAP -p -c -cp "$CREATE;$PONDER" com.simibubi.create.infrastructure.ponder.scenes.RedstoneScenes
#   analog_lever 场景的 .text(...) 调用次序 == en_us.json 的 text_1/2/3
```

### 8.3 类清单差集（API jar vs 完整 jar）

```powershell
$api  = Get-Content "$env:TEMP\croety-api-recon\flywheelapi-classes.txt"
$full = Get-Content "$env:TEMP\croety-api-recon\flywheel-classes.txt"
"API: $($api.Count)  FULL: $($full.Count)"        # → API: 183  FULL: 420
Compare-Object $api $full | Where-Object { $_.SideIndicator -eq '=>' } |
    ForEach-Object { ($_.InputObject -split '\.')[0..3] -join '.' } |
    Group-Object | Sort-Object Count -Descending
# → 163 dev.engine_room.flywheel.backend
# →  74 dev.engine_room.flywheel.impl
Compare-Object $api $full | Where-Object { $_.SideIndicator -eq '<=' }   # → 空
```

### 8.4 jar 内资源提取

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($PONDER)
$zip.Entries | Where-Object { $_.FullName -like 'assets/*' } | Select-Object -ExpandProperty FullName
# → assets/ponder/lang/{en_us,zh_cn,...}.json、assets/ponder/ponder/debug/scene_*.nbt、
#   assets/ponder/textures/{gui,special}/*
$zip.Dispose()

# Ponder 自带的 UI 语言键：读 assets/ponder/lang/en_us.json
#   → 键前缀 ponder.ui.* / key.ponder.* / catnip.*
# Create 的场景语言键：读 create-...-slim.jar!/assets/create/lang/en_us.json
#   → 1053 个 create.ponder.* 键，形如 create.ponder.<schematic>.<header|text_N>
# Create 的结构文件：assets/create/ponder/**.nbt，共 178 个
```

### 8.5 示例代码的编译验证

本文档里的 Java 片段被拼成 5 个独立源文件，用 JDK 17 的 javac 编译通过
（EXITCODE=0，产出 12 个 class）：

* FlywheelSnippet.java —— AbstractBlockEntityVisual + SimpleDynamicVisual +
  SimpleBlockEntityVisualizer.builder(...).apply() + 手工 VisualizerRegistry.setVisualizer
* Snippet2.java —— AbstractEntityVisual + SimpleTickableVisual + OrientedInstance +
  VisualizationHelper + Affine 变换链
* PonderSnippet.java —— PonderPlugin + addStoryBoard + forComponents + 一个完整场景
* Snippet3.java —— 标签、共享文本、索引排除、StoryBoardEntry 排序、PonderIndex.get*Access()
* CreatePonderSnippet.java —— CreateSceneBuilder + world().setKineticSpeed +
  effects().rotationSpeedIndicator

关键点：**classpath 里刻意没有放 flywheel-forge-1.20.1-*.jar**，只放
flywheel-forge-api-1.20.1-*.jar，与项目 compileOnly 的声明一致。
编译通过即证明文中用到的 Flywheel 类全部来自 API jar。

第一次编译**失败**了两次，正是 4.2 那两条错误的来源（collectCrumblingInstances / updateLight）。
之后又用 tools/find-api.ps1 -Source 拿到真实源码，据此修正了 showBasePlate 的方向、
title 的第一个参数含义、以及 skipVanillaRender 的默认值（4.3），
并把 2.3 的注册写法改成 .neverSkipVanillaRender() 后**重新编译通过**（EXITCODE=0）。

复现步骤：

```powershell
# 1. 建一个临时目录（不要建在 D:\Develop\croety 里）
$V = "$env:TEMP\croety-fw-verify"
New-Item -ItemType Directory -Force "$V\libs" | Out-Null

# 2. 收集 classpath：
#    - <forge_gradle>\minecraft_user_repo\**\*_mapped_*.jar
#    - <forge_gradle>\deobf_dependencies\**\*_mapped_*.jar   （排除 flywheel-forge-1.20.1-*.jar！）
#    - modules-2 里 MC 签名需要的那几个库：
#      joml-1.10.5、fastutil-8.5.9、guava-31.1-jre、datafixerupper、brigadier、
#      authlib、logging、gson、jsr305、commons-lang3、commons-io
$fg = Join-Path $env:USERPROFILE '.gradle\caches\forge_gradle'
Get-ChildItem (Join-Path $fg 'minecraft_user_repo') -Recurse -Filter '*_mapped_*.jar' |
    ForEach-Object { Copy-Item $_.FullName "$V\libs\" -Force }
Get-ChildItem (Join-Path $fg 'deobf_dependencies')  -Recurse -Filter '*_mapped_*.jar' |
    Where-Object { $_.Name -notlike 'flywheel-forge-1.20.1-*' } |
    ForEach-Object { Copy-Item $_.FullName "$V\libs\" -Force }

# 3. 编译（用通配符 classpath）
$jargs = @('-cp', "$V\libs\*", '-d', "$V\out", '--release', '17',
           '-encoding', 'UTF-8', '-proc:none', '-nowarn', "$V\FlywheelSnippet.java")
& "$env:JAVA_HOME\bin\javac.exe" @jargs
```

> 踩坑记录（都是实际撞到的）：
> 1. 一开始用 javac @argfile 传 440 个 jar 的 classpath，javac 直接失败；
>    改成「复制到单个目录 + -cp dir/* 通配符」才成功。
> 2. PowerShell 5.1 里 Join-Path $root 'a\*\*.jar' 不会展开通配符，
>    要用 Get-ChildItem -Recurse -Filter 再筛一遍。
> 3. **中文注释必须加 -encoding UTF-8**，否则本机 javac 默认按 GBK 读源码，会报
>    「编码 GBK 的不可映射字符」。本项目源码是 UTF-8。

### 8.6 未验证的部分

* **Ponder 播放时的运行时行为**（场景实际怎么渲染、重播时语言键会不会重算）
  只读了源码，没有真的启动游戏观察。不过 textIndex 的初值已经查清，见下。
  （反编译/源码显示 textIndex = 1 是在 PonderScene 的**构造函数**里赋值的，
  每个场景各自一份，因此同一个场景里第一个 .text() 一定是 text_1。）
* sharedText 里引用 Ponder 自带共享文本时 ResourceLocation 的拼法（见 7.4 末尾）是**反推**的，未验证。
* SimpleInstanceType.builder(...) 全链路（自定义 InstanceType）只验证了签名，没有编译示例。
* Ponder 的章节系统（PonderChapter / PonderChapterRegistry）没有展开。
* 所有示例都只做了**编译期**验证，没有在 runClient 里实际跑过，运行时行为需自行确认。

### 8.7 继续深挖：直接读源码

javap 只能给你签名。要看**方法体、注释、以及 mod 自己怎么用**，用这个：

```powershell
cd D:\Develop\croety

# 带注释的原始源码（Create / Ponder 的 sources jar 在 libs/sources 里）
.\tools\find-api.ps1 -Source net.createmod.ponder.api.scene.SceneBuilder
.\tools\find-api.ps1 -Source com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual

# Flywheel 的 sources jar 只含 impl 包，api/lib 会自动用 ForgeFlower 反编译（无注释但代码是真的）
.\tools\find-api.ps1 -Source dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual
.\tools\find-api.ps1 -Source 'dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer$Builder'

# 输出太长时截断
.\tools\find-api.ps1 -Source <类名> | Select-Object -First 80
```

本文档里带引号的「实现原文」「接口注释」全部来自这个模式：
SceneBuilder / OverlayInstructions / WorldInstructions / TextElementBuilder /
PonderSceneRegistrationHelper / PonderScene / PonderSceneBuilder 来自 Ponder 的 sources jar；
AbstractVisual / AbstractBlockEntityVisual / AbstractInstance / VisualizationHelper /
SimpleBlockEntityVisualizer$Builder 来自 flywheel-forge-api 的 ForgeFlower 反编译结果；
SingleAxisRotatingVisual 来自 Create 的 sources jar。
**libs/sources 里的 Flywheel sources jar 只有 dev.engine_room.flywheel.impl 包的内容**，
所以 api/lib 的注释是拿不到的，只有类文件可反编译。
