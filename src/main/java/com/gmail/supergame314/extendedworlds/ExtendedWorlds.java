package com.gmail.supergame314.extendedworlds;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  ExtendedWorlds for Paper 1.1.8
 *
 *  @author Super__Game
 */

public final class ExtendedWorlds extends JavaPlugin {

    static String prefix = "§f[§c§lEx§7§lWorld§f]";
    static File folder = null;
    static DataFileUse dfu = null;
    static FileConfiguration config;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==0){
            showHelp(sender);
            return true;
        }

        switch (args[0]) {
            case "c":
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(prefix + " §2§l/exw create <ワールド名> <タイプ> [シード値] [環境]");
                    sender.sendMessage(prefix + "   §7シード値は省略またはrndでランダムにできます");
                    return true;
                }
                for (World w : getServer().getWorlds()) {
                    if (w.getName().equals(args[1])) {
                        sender.sendMessage(prefix + " §e§l" + args[1] + "§c§lは既に存在しています！");
                        return true;
                    }
                }
                WorldCreator wc = new WorldCreator(args[1]);
                for (WorldType wt : WorldType.values()) {
                    if (args[2].startsWith(wt.name())) {
                        wc.type(wt);
                    }
                }
                if (args.length >= 4 && !args[3].equalsIgnoreCase("rnd")) {
                    try {
                        wc.seed(Long.parseLong(args[3]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(prefix + "§c§lシード値が不正です！");
                        return true;
                    }
                }
                for (World.Environment e : World.Environment.values()) {
                    if (args.length >= 5 && args[4].startsWith(e.name())) {
                        wc.environment(e);
                    }
                }
                sender.sendMessage(prefix + " §a§lCreating WORLD....");
                sender.sendMessage(prefix + "  §e§lNAME:§e§l" + wc.name());
                sender.sendMessage(prefix + "  §e§lTYPE:§e§l" + wc.type().getName());
                sender.sendMessage(prefix + "  §e§lENVIRONMENT:§e§l" + wc.environment().name());
                sender.sendMessage(prefix + "  §e§lSEED:§e§l" + wc.seed());
                sender.sendMessage(prefix + "  §7requested by :§0" + sender.getName());
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        World w = Bukkit.getServer().createWorld(wc);
                        if (w != null) {
                            dfu.addData("addedWorlds", w.getName());
                            sender.sendMessage(prefix + " §a§lDone!!");
                        } else {
                            sender.sendMessage(prefix + " §c§l正常に読み込めなかったようです");
                        }
                    }
                }.runTask(this);
                break;
            case "rm":
            case "remove":
            case "del":
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(prefix+" §2§l/exw delete <ワールド名>");
                    sender.sendMessage(prefix+"   §7対象のワールドを削除します");
                    sender.sendMessage(prefix+"   §7元に戻せるわけがありません");
                    return true;
                }
                World w = Bukkit.getWorld(args[1]);
                if (w == null) {
                    sender.sendMessage(prefix + "§c§lワールド§e§l" + args[1] + "§c§lが見つかりませんでした");
                    return true;
                }
                if(sender instanceof Player && ((Player) sender).getLocation().getWorld()==w){
                    sender.sendMessage(prefix+" §c§lあなたはそのワールドにいます！他のワールドに移動してください！");
                    return true;
                }
                for (Player p : w.getPlayers()) {
                    p.kickPlayer("[ExW] The world will be deleted. Please rejoin the server.");
                }
                String s = w.getName();
                getServer().unloadWorld(w, false);
                File[] f = Bukkit.getWorldContainer().listFiles();
                boolean b = false;
                if (f != null)
                    for (File file : f) {
                        if (file.getName().equals(s)) {
                            sender.sendMessage(prefix + " §7"+file.getName()+" was found");
                            sender.sendMessage(prefix + " §a§lファイルを削除します...");
                            b = deleteFolder(file);
                            break;
                        }
                    }
                if(b) {
                    dfu.removeData("addedWorlds",s);
                    dfu.removeData("reloadWorlds",s);
                    sender.sendMessage(prefix + " §a§lワールド§e§l"+args[1]+"§a§lが正常に削除されました");
                }else {
                    sender.sendMessage(prefix + " §c§lファイルが見つからなかったか、削除できませんでした");
                    sender.sendMessage(prefix + " §c§l削除が正常に終了できませんでした");
                }
                break;
            case "reload":
                if (args.length < 2) {
                    sender.sendMessage(prefix+" §2§l/exw reload <ワールド名>");
                    sender.sendMessage(prefix+"   §7§lワールドをプラグインフォルダから読み込みなおします");
                    return true;
                }
                w = Bukkit.getWorld(args[1]);
                if (w == null) {
                    sender.sendMessage(prefix + "§c§lワールド§e§l" + args[1] + "§c§lが見つかりませんでした");
                    return true;
                }
                if(!isExistIn(getDataFolder(),args[1])){
                    sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                    sender.sendMessage(prefix+" §c§lファイルをプラグインフォルダにいれてから行ってください");
                    return true;
                }
                if(sender instanceof Player && ((Player) sender).getLocation().getWorld()==w){
                    sender.sendMessage(prefix+" §c§lあなたはそのワールドにいます！他のワールドに移動してください！");
                    return true;
                }
                for (Player p : w.getPlayers()) {
                    p.kickPlayer("[ExW] The world will be reloaded. Please rejoin the server.");
                }
                s = w.getName();
                getServer().unloadWorld(w, false);
                f = Bukkit.getWorldContainer().listFiles();
                b = false;
                if (f != null)
                    for (File file : f) {
                        if (file.getName().equals(s)) {
                            sender.sendMessage(prefix + " §7"+file.getName()+" was found");
                            sender.sendMessage(prefix + " §7ファイルを削除します...");
                            b = deleteFolder(file);
                            break;
                        }
                    }
                if(b) {
                    sender.sendMessage(prefix + " §7ワールド"+args[1]+"が正常に削除されました");
                }else {
                    sender.sendMessage(prefix + " §c§lファイルが見つからなかったか、削除できませんでした");
                    sender.sendMessage(prefix + " §c§l削除が正常に終了できませんでした");
                    return true;
                }
                if(importWorld(sender, s, true)){
                    sender.sendMessage(prefix + " §a§lリロードが完了しました");
                }
                break;
            case "w":
            case "warp":
            case "goto":
                if(sender instanceof Player){
                    if(args.length<2){
                        sender.sendMessage(prefix+" §2§l/exw warp <ワールド名>");
                        sender.sendMessage(prefix+"   §7対象のワールドにワープします");
                        return true;
                    }
                    w = Bukkit.getWorld(args[1]);
                    if(w == null){
                        sender.sendMessage(prefix + "§c§lワールド§e§l"+args[1]+"§c§lが見つかりませんでした");
                        return true;
                    }
                    sender.sendMessage(prefix + " §7teleporting...");
                    int x=0;
                    if(args.length>=3)
                        x=Integer.parseInt(args[2]);
                    int z=0;
                    if(args.length>=4)
                        z=Integer.parseInt(args[3]);
                    ((Player) sender).teleport(new Location(w,x,w.getHighestBlockYAt(x,z),z));
                    w.loadChunk(0,0);
                }
                break;
            case "reloadset":
                if(args.length<3){
                    sender.sendMessage(prefix+" §2§l/exw reloadset <ワールド名> <true/false>");
                    sender.sendMessage(prefix+"   §7鯖起動時に毎回プラグインフォルダから読み込みなおすか設定します");
                    sender.sendMessage(prefix+"   §7falseにすると鯖起動時に毎回プラグインフォルダから読み込みなおします");
                    return true;
                }
                w = Bukkit.getWorld(args[1]);
                if(!isExistIn(getDataFolder(),args[1])){
                    sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                    sender.sendMessage(prefix+" §c§lファイルをプラグインフォルダにいれてから行ってください");
                    return true;
                }
                if(w == null){
                    sender.sendMessage(prefix + "§c§lワールド§e§l"+args[1]+"§c§lが見つかりませんでした");
                    return true;
                }
                if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
                    if(args[2].equalsIgnoreCase("true")){
                        dfu.removeData("addedWorlds",args[1]);
                        dfu.addData("reloadWorlds",args[1]);
                        sender.sendMessage(prefix + "§a§lワールド§e§l"+args[1]+"§a§lの鯖起動時再読み込みを§e§ltrue§a§lにしました");
                    }
                    if(args[2].equalsIgnoreCase("false")){
                        if(!Arrays.asList("world","world_nether","world_the_end").contains(args[2]))dfu.addData("addedWorlds",args[1]);
                        dfu.removeData("reloadWorlds",args[1]);
                        sender.sendMessage(prefix + "§a§lワールド§e§l"+args[1]+"§a§lの鯖起動時再読み込みを§e§lfalse§a§lにしました");
                    }
                }else {
                    sender.sendMessage(prefix + "§c§l§e§ltrue§c§lか§e§lfalse§c§lで設定してください");
                    return true;
                }
                break;
            case "import":
            case "load":
                if(args.length<2){
                    sender.sendMessage(prefix+" §2§l/exw import <ファイル名>");
                    sender.sendMessage(prefix+"   §7プラグインフォルダからワールドをコピーします");
                    sender.sendMessage(prefix+"   §7ファイル名がそのままワールド名になります");
                    return true;
                }
                if(!isExistIn(getDataFolder(),args[1])){
                    sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                    sender.sendMessage(prefix+" §c§lファイルをプラグインフォルダにいれてから行ってください");
                    return true;
                }
                if(importWorld(sender,args[1],true)) {
                    dfu.addData("addedWorlds", args[1]);
                }
                break;
            case "list":
            case "show":
                for(World world:getServer().getWorlds()) {
                    sender.sendMessage(prefix+" §e§l"+world.getName());
                    sender.sendMessage(prefix+" §7 鯖起動時読み込み:"+Arrays.asList(dfu.getData("reloadWorlds")).contains(world.getName()));
                }
                break;
        }
        return true;
    }

    @Override
    public void onEnable() {
        System.out.println("§2==================================================");
        System.out.println("§c┌ーー       §f             §6Enabling           ");
        System.out.println("§c｜ー　xtended §f｜｜｜orld §6 ExtendedWorlds     ");
        System.out.println("§c└ーー  　　   §f└―-┘      §6      Version 1.0   ");
        System.out.println("§2==================================================");
        saveDefaultConfig();
        config = getConfig();

        folder = getDataFolder();
        dfu = new DataFileUse(new File(folder+"\\data.datafile"),this);
        dfu.saveDefaultData("addedWorlds","reloadWorlds");
        String[] worlds=dfu.getData("reloadWorlds");
        if(worlds!=null && worlds.length!=0 &&  Bukkit.getWorld(worlds[0])==null) {
            getLogger().info(config.getBoolean("dont_reload")?"\"Dont_reload\" in the config is true! The server will start without reloading worlds":"[reloading] Starting creating a world");
            for (String world : worlds) {
                if(config.getBoolean("dont_reload")) {
                    if(Bukkit.getWorld(world)!=null){
                        continue;
                    }
                    getLogger().info("[addedWorldCreating] Preparing World \""+world+"\"");
                    Bukkit.createWorld(new WorldCreator(world));
                }else{
                    getLogger().info("[reloading] Preparing World \"" + world + "\"");
                    File[] f = Bukkit.getWorldContainer().listFiles();
                    boolean b = false;
                    if (f != null)
                        for (File file : f) {
                            if (file.getName().equals(world)) {
                                b = deleteFolder(file);
                                break;
                            }
                        }
                    if (b) {
                        getLogger().info("[reloading] §a§lワールド§e§l" + world + "§a§lが正常に削除されました");
                    } else {
                        getLogger().info(prefix + "[reloading] §c§lファイルが見つからなかったか、削除できませんでした");
                        getLogger().warning(prefix + "[reloading] §c§l削除が正常に終了できませんでした");
                    }
                    importWorld(getServer().getConsoleSender(), world, false);
                }
            }
        }
        worlds=dfu.getData("addedWorlds");
        if(worlds!=null) {
            for (String world : worlds) {
                if(Bukkit.getWorld(world)!=null){
                    continue;
                }
                getLogger().info("[addedWorldCreating] Preparing World \""+world+"\"");
                Bukkit.createWorld(new WorldCreator(world));
            }
        }


        // Plugin startup logic
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length==1) {
            return strings(args[0],Arrays.asList("create","delete","warp","import","reload","reloadset","list"));
        }
        if(args.length==2){
            if(args[0].equalsIgnoreCase("warp") || args[0].equalsIgnoreCase("reloadset") || args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("delete")) {
                List<String> list = new ArrayList<>();
                for(World w:Bukkit.getWorlds())
                    list.add(w.getName());
                return strings(args[1], list);
            }
        }
        if(args.length==3){
            if(args[0].equalsIgnoreCase("create")) {
                List<String> list = new ArrayList<>();
                for(WorldType w:WorldType.values())
                    list.add(w.getName());
                return strings(args[2], list);
            }
            if(args[0].equalsIgnoreCase("reloadset")) {
                return strings(args[2],"true","false");
            }
        }
        if(args.length==4){
            if(args[0].equalsIgnoreCase("create")) {
                return strings(args[3], "rnd");
            }
        }
        if(args.length==5){
            if(args[0].equalsIgnoreCase("create")) {
                List<String> list = new ArrayList<>();
                for(World.Environment w: World.Environment.values())
                    list.add(w.name());
                return strings(args[4], list);
            }
        }
        return null;
    }

    private void showHelp(CommandSender sender){
        sender.sendMessage(prefix+" §2§l/exw create <ワールド名> <タイプ> [シード値] [環境]");
        sender.sendMessage(prefix+"   §7新たなワールドを作成します");
        sender.sendMessage(prefix+"   §7シード値は省略またはrndでランダムにできます");
        sender.sendMessage(prefix+" §2§l/exw delete <ワールド名>");
        sender.sendMessage(prefix+"   §7対象のワールドを削除します");
        sender.sendMessage(prefix+"   §7元に戻せるわけがありません");
        sender.sendMessage(prefix+" §2§l/exw warp <ワールド名> [x] [z]");
        sender.sendMessage(prefix+"   §7対象のワールドにワープします");
        sender.sendMessage(prefix+"   §7座標省略すると0,0に飛ばされます....");
        sender.sendMessage(prefix+" §2§l/exw reload <ワールド名>");
        sender.sendMessage(prefix+"   §7ワールドをプラグインフォルダから読み込みなおします");
        sender.sendMessage(prefix+" §2§l/exw reloadset <ワールド名> <true/false>");
        sender.sendMessage(prefix+"   §7鯖起動時に毎回プラグインフォルダから読み込みなおすか");
        sender.sendMessage(prefix+"   §7設定します");
        sender.sendMessage(prefix+"   §7falseにすると鯖起動時に毎回プラグインフォルダから");
        sender.sendMessage(prefix+"   §7読み込みなおします");
        sender.sendMessage(prefix+" §8created by Super__Game");
    }

    private List<String> strings(String s,List<String> args){
        List<String> list = new ArrayList<>();
        for(String s1:args){
            if(s1.startsWith(s))
                list.add(s1);
        }
        return list;
    }

    private List<String> strings(String s,String... args){
        List<String> list = new ArrayList<>();
        for(String s1:args){
            if(s1.startsWith(s))
                list.add(s1);
        }
        return list;
    }

    private boolean deleteFolder(File file){
        File[] fs =file.listFiles();
        if(fs!=null)
            for(File f:fs){
                boolean b = deleteFolder(f);
                if (!b){
                    return false;
                }
            }
        return file.delete();
    }

    private boolean copyFile(File world,File to) {
        try {
            System.out.println("copying File "+world.getName());
            Files.copy(world.toPath(),to.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if(world.isDirectory()){
            File[] files = world.listFiles();
            if(files!=null) {
                for (File file : files) {
                    copyFile(file, new File(to.getPath()+"\\"+file.getName()));
                }
            }
        }
        return true;
    }

    private boolean importWorld(CommandSender sender,String worldName,boolean newThread){
        if(Bukkit.getWorld(worldName)!=null){
            sender.sendMessage(prefix+"   §c§l既に存在しています！");
            return false;
        }
        File[] moto = folder.listFiles();
        if(moto != null) {
            boolean found = false;
            for (File file : moto) {
                if (file.getName().equals(worldName)) {
                    found = true;
                    sender.sendMessage(prefix+" §7ファイルをコピー中....");
                    sender.sendMessage(prefix+" §7File path : "+worldName);
                    if(!copyFile(file,new File(worldName))){
                        sender.sendMessage(prefix+" §c§lファイルが書き込めませんでした");
                        sender.sendMessage(prefix+" §7 (Error: MAIN-[couldn't copy file])");
                        return false;
                    }
                    break;
                }
            }
            if(!found){
                sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                return false;
            }
        }else {
            sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
            return false;
        }
        sender.sendMessage(prefix+" §7ワールド生成中....");
            World neww = getServer().createWorld(new WorldCreator(worldName));
            if (neww != null) {
                sender.sendMessage(prefix + " §a§lDone!!");
                sender.sendMessage(prefix + " §a§lワールド、" + worldName + "が生成されました");
            } else {
                sender.sendMessage(prefix + " §c§l正常に読み込めなかったようです");
                return false;
            }

        return true;
    }

    private boolean isExistIn(File directory,String fileName){
        if(!directory.isDirectory())return false;
        File[] fs = directory.listFiles();
        if(fs != null){
            for(File f:fs){
                if(f.getName().equals(fileName)){
                    return true;
                }
            }
        }
        return false;
    }
}
