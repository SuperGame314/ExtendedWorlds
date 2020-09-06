package com.gmail.supergame314.extendedworlds;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ExtendedWorlds extends JavaPlugin {

    static String prefix = "§f[§c§lEx§7§lWorld§f]";
    static File folder = null;
    static File dataFile = null;
    static DataFileUse dfu = null;

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
                sender.sendMessage(prefix + "  §7requested by :§0" + wc.seed());

                Bukkit.getConsoleSender().sendMessage(prefix + "§7§lRunning creating world task on "+getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                    World w = Bukkit.getServer().createWorld(wc);
                    if (w != null) {
                        dfu.addData("addedWorlds",w.getName());
                        sender.sendMessage(prefix + " §a§lDone!!");
                    } else {
                        sender.sendMessage(prefix + " §c§l正常に読み込めなかったようです");
                    }
                }));
                break;
            case "rm":
            case "remove":
            case "del":
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(prefix+" §2§l/exw delete <ワールド名>");
                    sender.sendMessage(prefix+"   §4§l他のもですがこれを実行すると戻せません！！");
                    return true;
                }
                World w = Bukkit.getWorld(args[1]);
                if (w == null) {
                    sender.sendMessage(prefix + "§c§lワールド§e§l" + args[1] + "§c§lが見つかりませんでした");
                    return true;
                }
                if(sender instanceof Player && ((Player) sender).getLocation().getWorld()==w){
                    sender.sendMessage(prefix+" §c§lあなたはそのワールドにいます！");
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
                    sender.sendMessage(prefix + " §a§lワールド§e§l"+args[1]+"§a§lが正常に削除されました");
                }else {
                    sender.sendMessage(prefix + " §c§lファイルが見つからなかったか、削除できませんでした");
                    sender.sendMessage(prefix + " §c§l削除が正常に終了できませんでした");
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
            case "save":
                if(args.length<3){
                    sender.sendMessage(prefix+" §2§l/exw save <ワールド名> <true/false>");
                    sender.sendMessage(prefix+"   §7対象のワールドを保存するか設定します");
                    sender.sendMessage(prefix+"   §7falseにして鯖を閉じるとワールドは保存されず、");
                    sender.sendMessage(prefix+"   §7前回の最初のワールドに戻ります");
                    return true;
                }
                w = Bukkit.getWorld(args[1]);
                if(w == null){
                    sender.sendMessage(prefix + "§c§lワールド§e§l"+args[1]+"§c§lが見つかりませんでした");
                    return true;
                }
                if("true".startsWith(args[2]) || "false".startsWith(args[2]))
                    w.setAutoSave(args[2].equalsIgnoreCase("true"));
                else {
                    sender.sendMessage(prefix + "§c§l§e§ltrue§c§lか§e§lfalse§c§lで設定してください");
                    return true;
                }
                sender.sendMessage(prefix + "§a§lワールド§e§l"+args[1]+"§a§lのオートセーブを"+w.isAutoSave()+"にしました");
                break;
            case "import":
                if(args.length<2){
                    sender.sendMessage(prefix+" §2§l/exw import <ファイル名>");
                    sender.sendMessage(prefix+"   §7プラグインフォルダからワールドをコピーします");
                    sender.sendMessage(prefix+"   §7ファイル名がそのままワールド名になります");
                    return true;
                }
                if(Bukkit.getWorld(args[1])!=null){
                    sender.sendMessage(prefix+"   §c§l既に存在しています！");
                    return true;
                }
                File[] moto = folder.listFiles();
                if(moto != null) {
                    boolean found = false;
                    for (File file : moto) {
                        if (file.getName().equals(args[1])) {
                            found = true;
                            sender.sendMessage(prefix+" §7ファイルをコピー中....");
                            File newFile = new File(args[1]);
                            sender.sendMessage(prefix+" §7File path : "+args[1]);
                            b=file.renameTo(newFile);
                            if(!b){
                                sender.sendMessage(prefix+" §c§lファイルが書き込めませんでした");
                                sender.sendMessage(prefix+" §7 (Error: MAIN-[couldn't rename file])");
                                return true;
                            }
                            break;
                        }
                    }
                    if(!found){
                        sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                        return true;
                    }
                }else {
                    sender.sendMessage(prefix+" §c§lファイルが見つかりませんでした");
                }
                sender.sendMessage(prefix+" §7ワールド生成中....");
                Bukkit.getConsoleSender().sendMessage(prefix + "§7§lRunning creating world task on "+getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                    World neww = getServer().createWorld(new WorldCreator(args[1]));
                    if (neww != null) {
                        sender.sendMessage(prefix + " §a§lDone!!");
                        sender.sendMessage(prefix + " §a§lワールド、"+args[1]+"が生成されました");
                        dfu.addData("addedWorlds",neww.getName());
                    } else {
                        sender.sendMessage(prefix + " §c§l正常に読み込めなかったようです");
                    }
                }));

        }
        return true;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        folder = getDataFolder();
        dfu = new DataFileUse(new File(folder+"data.datafile"),this);
        dfu.saveDefaultData();
        String[] worlds=dfu.getData("addedWorlds");
        if(worlds!=null) {
            for (String world : worlds) {
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
            return strings(args[0],Arrays.asList("create","delete","warp","import","save"));
        }
        if(args.length==2){
            if(args[0].equalsIgnoreCase("warp") || args[0].equalsIgnoreCase("delete")) {
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
        sender.sendMessage(prefix+"   §4§l他のもですがこれを実行すると戻せません！！");
        sender.sendMessage(prefix+" §2§l/exw warp <ワールド名> [x] [z]");
        sender.sendMessage(prefix+"   §7対象のワールドにワープします");
        sender.sendMessage(prefix+"   §7座標省略すると0,0に飛ばされます....");
        sender.sendMessage(prefix+" §2§l/exw save <ワールド名> <true/false>");
        sender.sendMessage(prefix+"   §7対象のワールドを保存するか設定します");
        sender.sendMessage(prefix+"   §7falseにして鯖を閉じるとワールドは保存されず、");
        sender.sendMessage(prefix+"   §7前回の最初のワールドに戻ります");
        sender.sendMessage(prefix+" §2§l/exw reload <ワールド名>");
        sender.sendMessage(prefix+"   §7対象のワールドを前回の保存時の状態に戻します");
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


}
