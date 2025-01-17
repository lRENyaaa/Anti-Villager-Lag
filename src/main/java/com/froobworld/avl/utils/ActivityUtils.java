package com.froobworld.avl.utils;

import com.froobworld.avl.Avl;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ActivityUtils {
    private static final String NAME = Bukkit.getServer().getClass().getPackage().getName();
    private static final String VERSION = NAME.substring(NAME.lastIndexOf('.') + 1);
    private static final Set<String> IGNORE_JOB_SITE_VERSIONS = Sets.newHashSet("v1_16_R1", "v1_16_R2", "v1_16_R3");

    private static Method VILLAGER_GET_HANDLE_METHOD;
    private static Method VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD;
    private static Method BEHAVIOUR_CONTROLLER_GET_SCHEDULE_METHOD;
    private static Method CURRENT_ACTIVITY_METHOD;
    private static Method SET_SCHEDULE_METHOD;

    private static Field ACTIVITIES_FIELD;

    private static Object ACTIVITY_CORE;
    private static Object ACTIVITY_IDLE;
    private static Object ACTIVITY_WORK;
    private static Object ACTIVITY_MEET;
    private static Object ACTIVITY_REST;
    private static Object SCHEDULE_EMPTY;
    private static Object SCHEDULE_SIMPLE;
    private static Object SCHEDULE_VILLAGER_DEFAULT;
    private static Object SCHEDULE_VILLAGER_BABY;
    static {
        boolean newApi = Integer.parseInt(VERSION.split("_")[1]) >= 17;
        boolean Api_1_18 = Integer.parseInt(VERSION.split("_")[1]) >= 18;
        boolean leastApi = Integer.parseInt(VERSION.split("_")[1]) >= 19;

        try {
            String behaviorControllerClass = newApi ? "net.minecraft.world.entity.ai.BehaviorController" : "net.minecraft.server." + VERSION + ".BehaviorController";
            String activityClass = newApi ? "net.minecraft.world.entity.schedule.Activity" : "net.minecraft.server." + VERSION + ".Activity";
            String scheduleClass = newApi ? "net.minecraft.world.entity.schedule.Schedule" : "net.minecraft.server." + VERSION + ".Schedule";
            String entityLivingClass = newApi ? "net.minecraft.world.entity.EntityLiving" : "net.minecraft.server." + VERSION + ".EntityLiving";

            VILLAGER_GET_HANDLE_METHOD = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftVillager").getMethod("getHandle");
            VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD = Class.forName(entityLivingClass).getMethod(Api_1_18 ? (leastApi ? "dz" : "du") : "getBehaviorController");

            BEHAVIOUR_CONTROLLER_GET_SCHEDULE_METHOD = Class.forName(behaviorControllerClass).getMethod(Api_1_18 ? "b" : "getSchedule");
            CURRENT_ACTIVITY_METHOD = Class.forName(scheduleClass).getMethod("a", int.class);
            SET_SCHEDULE_METHOD = Class.forName(behaviorControllerClass).getMethod(Api_1_18 ? "a" : "setSchedule", Class.forName(scheduleClass));

            Map<String, String> activitiesFieldNameMap = new HashMap<>();
            activitiesFieldNameMap.put("v1_14_R1", "g");
            activitiesFieldNameMap.put("v1_15_R1", "g");
            activitiesFieldNameMap.put("v1_16_R1", "j");
            activitiesFieldNameMap.put("v1_16_R2", "j");
            activitiesFieldNameMap.put("v1_16_R3", "j");
            activitiesFieldNameMap.put("v1_17_R1", "k");
            activitiesFieldNameMap.put("v1_17_R2", "k");
            activitiesFieldNameMap.put("v1_17_R3", "k");
            activitiesFieldNameMap.put("v1_18_R1", "k");
            activitiesFieldNameMap.put("v1_18_R2", "k");
            activitiesFieldNameMap.put("v1_19_R1", "k");

            ACTIVITIES_FIELD = Class.forName(behaviorControllerClass).getDeclaredField(activitiesFieldNameMap.get(VERSION));
            ACTIVITIES_FIELD.setAccessible(true);

            ACTIVITY_CORE = Class.forName(activityClass).getField(newApi ? "a" : "CORE").get(null);
            ACTIVITY_IDLE = Class.forName(activityClass).getField(newApi ? "b" : "IDLE").get(null);
            ACTIVITY_WORK = Class.forName(activityClass).getField(newApi ? "c" : "WORK").get(null);
            ACTIVITY_MEET = Class.forName(activityClass).getField(newApi ? "f" : "MEET").get(null);
            ACTIVITY_REST = Class.forName(activityClass).getField(newApi ? "e" : "REST").get(null);
            SCHEDULE_EMPTY = Class.forName(scheduleClass).getField(newApi ? "c" : "EMPTY").get(null);
            SCHEDULE_SIMPLE = Class.forName(scheduleClass).getField(newApi ? "d" : "SIMPLE").get(null);
            SCHEDULE_VILLAGER_DEFAULT = Class.forName(scheduleClass).getField(newApi ? "f" : "VILLAGER_DEFAULT").get(null);
            SCHEDULE_VILLAGER_BABY = Class.forName(scheduleClass).getField(newApi ? "e" : "VILLAGER_BABY").get(null);

        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setActivitiesNormal(Villager villager) {
        try {
            ((Set) ACTIVITIES_FIELD.get(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)))).clear();
            ((Set) ACTIVITIES_FIELD.get(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)))).add(ACTIVITY_CORE);
            Object currentSchedule = BEHAVIOUR_CONTROLLER_GET_SCHEDULE_METHOD.invoke(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)));
            Object currentActivity;
            if (currentSchedule == null) {
                currentActivity = ACTIVITY_IDLE;
            } else {
                currentActivity = CURRENT_ACTIVITY_METHOD.invoke(currentSchedule, (int) villager.getWorld().getTime());
            }
            ((Set) ACTIVITIES_FIELD.get(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)))).add(currentActivity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setActivitiesEmpty(Villager villager) {
        try {
            ImmutableCollection origin = (ImmutableCollection) ACTIVITIES_FIELD.get(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)));
            Set<Object> mutable = (Set) origin.stream().collect(Collectors.toSet());
            mutable.clear();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setScheduleNormal(Villager villager) {
        try {
            SET_SCHEDULE_METHOD.invoke(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)), villager.isAdult() ? (villager.getProfession() == Villager.Profession.NITWIT ? SCHEDULE_SIMPLE : SCHEDULE_VILLAGER_DEFAULT ) : SCHEDULE_VILLAGER_BABY);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setScheduleEmpty(Villager villager) {
        try {
            SET_SCHEDULE_METHOD.invoke(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)), SCHEDULE_EMPTY);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static boolean badCurrentActivity(Villager villager) {
        try {
            Object currentSchedule = BEHAVIOUR_CONTROLLER_GET_SCHEDULE_METHOD.invoke(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager)));
            if(currentSchedule == null) {
                return false;
            }
            Object currentActivity = CURRENT_ACTIVITY_METHOD.invoke(currentSchedule, (int) villager.getWorld().getTime());
            return badActivity(currentActivity, villager);
        } catch (IllegalAccessException | InvocationTargetException ignored) {}

        return false;
    }
    public static boolean wouldBeBadActivity(Villager villager) {
        Object wouldBeSchedule = villager.isAdult() ? (villager.getProfession() == Villager.Profession.NITWIT ? SCHEDULE_VILLAGER_DEFAULT : SCHEDULE_SIMPLE) : SCHEDULE_VILLAGER_BABY;
        try {
            Object currentActivity = CURRENT_ACTIVITY_METHOD.invoke(wouldBeSchedule, (int) villager.getWorld().getTime());
            return badActivity(currentActivity, villager);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }

        return false;
    }

    private static boolean badActivity(Object activity, Villager villager) {
        if(activity == ACTIVITY_REST) {
            return villager.getMemory(MemoryKey.HOME) == null || isPlaceholderMemory(villager, MemoryKey.HOME);
        }
        if(activity == ACTIVITY_WORK) {
            return !IGNORE_JOB_SITE_VERSIONS.contains(VERSION) && (villager.getMemory(MemoryKey.JOB_SITE) == null || isPlaceholderMemory(villager, MemoryKey.JOB_SITE));
        }
        if(activity == ACTIVITY_MEET) {
            return villager.getMemory(MemoryKey.MEETING_POINT) == null || isPlaceholderMemory(villager, MemoryKey.MEETING_POINT);
        }

        return false;
    }

    public static void replaceBadMemories(Villager villager) {
        if(villager.getMemory(MemoryKey.HOME) == null) {
            villager.setMemory(MemoryKey.HOME, new Location(villager.getWorld(), villager.getLocation().getBlockX(), -10000, villager.getLocation().getBlockZ()));
        }
        if(villager.getMemory(MemoryKey.JOB_SITE) == null && !IGNORE_JOB_SITE_VERSIONS.contains(VERSION)) {
            villager.setMemory(MemoryKey.JOB_SITE, new Location(villager.getWorld(), villager.getLocation().getBlockX(), -10000, villager.getLocation().getBlockZ()));
        }
        if(villager.getMemory(MemoryKey.MEETING_POINT) == null) {
            villager.setMemory(MemoryKey.MEETING_POINT, new Location(villager.getWorld(), villager.getLocation().getBlockX(), -10000, villager.getLocation().getBlockZ()));
        }
    }

    public static boolean isPlaceholderMemory(Villager villager, MemoryKey<Location> memoryKey) {
        Location memoryLocation = villager.getMemory(memoryKey);
        return memoryLocation != null && memoryLocation.getY() < 0;
    }

    public static void clearPlaceholderMemories(Villager villager) {

        if(villager.getMemory(MemoryKey.HOME) != null && isPlaceholderMemory(villager, MemoryKey.HOME)) {
            villager.setMemory(MemoryKey.HOME, null);
        }
        if(villager.getMemory(MemoryKey.JOB_SITE) != null && isPlaceholderMemory(villager, MemoryKey.JOB_SITE)) {
            villager.setMemory(MemoryKey.JOB_SITE, null);
        }
        if(villager.getMemory(MemoryKey.MEETING_POINT) != null && isPlaceholderMemory(villager, MemoryKey.MEETING_POINT)) {
            villager.setMemory(MemoryKey.MEETING_POINT, null);
        }
    }

    public static boolean isScheduleNormal(Villager villager) {
        try {
            return BEHAVIOUR_CONTROLLER_GET_SCHEDULE_METHOD.invoke(VILLAGER_GET_BEHAVIOUR_CONTROLLER_METHOD.invoke(VILLAGER_GET_HANDLE_METHOD.invoke(villager))) == (villager.isAdult() ? (villager.getProfession() == Villager.Profession.NITWIT ? SCHEDULE_SIMPLE : SCHEDULE_VILLAGER_DEFAULT ) : SCHEDULE_VILLAGER_BABY );
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

}
