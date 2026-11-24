package com.wdlpiaoyi.wmisc.compat.irons;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.impl.IronsArcaneAnvilJeiFeature;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class AnvilJeiBudget {
    private static final ResourceLocation SCROLL = new ResourceLocation("irons_spellbooks", "scroll");
    private static final Object IMBUE_KEY = new Object();
    private static int recipesBefore;
    private static int recipesAfter;
    private static long stacksTrimmed;
    private static int tuplesTrimmed;
    private static boolean capped;

    private AnvilJeiBudget() {
    }

    public static List<?> capRecipes(List<?> recipes) {
        if (recipes != null && IronsArcaneAnvilJeiFeature.isActive()) {
            resetCounters();
            int maxImbue = IronsArcaneAnvilJeiFeature.maxImbueRecipes();
            int maxUpgrade = IronsArcaneAnvilJeiFeature.maxUpgradeRecipesPerOrb();
            int total = recipes.size();
            recipesBefore = total;
            recipesAfter = total;
            if (total != 0 && (maxImbue > 0 || maxUpgrade > 0)) {
                Object[] keys = new Object[total];
                Map<Object, Integer> totals = new LinkedHashMap();

                for(int i = 0; i < total; ++i) {
                    keys[i] = bucketKey(recipes.get(i));
                    if (keys[i] != null) {
                        totals.merge(keys[i], 1, Integer::sum);
                    }
                }

                List<Object> kept = new ArrayList(total);
                Map<Object, Integer> seen = new LinkedHashMap();
                Map<Object, Integer> lastBucket = new LinkedHashMap();

                for(int i = 0; i < total; ++i) {
                    Object key = keys[i];
                    if (key == null) {
                        kept.add(recipes.get(i));
                    } else {
                        int bucketTotal = (Integer)totals.getOrDefault(key, 0);
                        int limit = key == IMBUE_KEY ? maxImbue : maxUpgrade;
                        if (limit > 0 && bucketTotal > limit) {
                            int pos = (Integer)seen.merge(key, 1, Integer::sum) - 1;
                            int bucket = (int)((long)pos * (long)limit / (long)bucketTotal);
                            Integer previous = (Integer)lastBucket.put(key, bucket);
                            if (previous == null || previous != bucket) {
                                kept.add(recipes.get(i));
                            }
                        } else {
                            kept.add(recipes.get(i));
                        }
                    }
                }

                recipesAfter = kept.size();
                capped = true;
                return kept;
            } else {
                return recipes;
            }
        } else {
            return recipes;
        }
    }

    public static void trimTuple(AnvilTupleView tuple) {
        if (tuple != null && IronsArcaneAnvilJeiFeature.isActive()) {
            int cap = IronsArcaneAnvilJeiFeature.maxItemsPerSlot();
            if (cap > 0) {
                List<?> left = tuple.wmisc$leftInputs();
                List<?> right = tuple.wmisc$rightInputs();
                List<?> output = tuple.wmisc$outputs();
                long freed = 0L;
                if (right != null && output != null && right.size() == output.size() && right.size() > cap) {
                    freed += keepPaired(right, output, cap);
                } else {
                    freed += keepEven(right, cap);
                    freed += keepEven(output, cap);
                }

                freed += keepEven(left, cap);
                if (freed > 0L) {
                    stacksTrimmed += freed;
                    ++tuplesTrimmed;
                }

            }
        }
    }

    public static void logSummary() {
        if (IronsArcaneAnvilJeiFeature.isActive() && capped) {
            capped = false;
            Wmisc.LOG.info("[w_misc] 奥术铁砧 JEI：配方 {} → {} 条，槽位候选 -{} 个（裁了 {} 条配方的候选，每槽上限 {}）", new Object[]{format((long)recipesBefore), format((long)recipesAfter), format(stacksTrimmed), format((long)tuplesTrimmed), format((long)IronsArcaneAnvilJeiFeature.maxItemsPerSlot())});
        }
    }

    private static Object bucketKey(Object recipe) {
        if (recipe instanceof AnvilRecipeView view) {
            Item left = view.wmisc$leftItem();
            Item right = view.wmisc$rightItem();
            if (left == null) {
                return null;
            } else if (right == null) {
                return IMBUE_KEY;
            } else {
                return SCROLL.equals(ForgeRegistries.ITEMS.getKey(left)) ? null : right;
            }
        } else {
            return null;
        }
    }

    private static long keepEven(List<?> list, int cap) {
        return list != null && list.size() > cap ? retain(list, evenIndices(list.size(), cap)) : 0L;
    }

    private static long keepPaired(List<?> first, List<?> second, int cap) {
        List<Integer> indices = evenIndices(Math.min(first.size(), second.size()), cap);
        return retain(first, indices) + retain(second, indices);
    }

    private static List<Integer> evenIndices(int size, int cap) {
        List<Integer> indices = new ArrayList(cap);

        for(int i = 0; i < cap; ++i) {
            indices.add((int)((long)i * (long)size / (long)cap));
        }

        return indices;
    }

    private static long retain(List<?> list, List<Integer> indices) {
        int freed = list.size() - indices.size();
        if (freed <= 0) {
            return 0L;
        } else {
            List<Object> kept = new ArrayList(indices.size());

            for(int index : indices) {
                kept.add(list.get(index));
            }

            // JEI 放进来的列表未必可变，所以先试着原地改，抛 UnsupportedOperationException 就放弃。
            List mutable = list;

            try {
                mutable.clear();
                mutable.addAll(kept);
            } catch (UnsupportedOperationException var6) {
                return 0L;
            }

            return (long)freed;
        }
    }

    private static void resetCounters() {
        recipesBefore = 0;
        recipesAfter = 0;
        stacksTrimmed = 0L;
        tuplesTrimmed = 0;
        capped = false;
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
