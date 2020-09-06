package com.gmail.supergame314.extendedworlds;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class DataFileUse {
    
    private File df;
    private Logger logger;
    private List<String> readData = new ArrayList<>();

    public DataFileUse(File dataFile, JavaPlugin javaPlugin){
        df = dataFile;
        logger = javaPlugin.getLogger();
    }

    public void saveDefaultData() {
        if(!df.exists()){
            try {
                logger.info("data.datafile doesn't exist! creating file...");
                if(!df.createNewFile())
                    logger.warning("couldn't create data file.");
                else 
                    logger.info("Done!!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save(){
        saveDefaultData();
        try {
            FileWriter fw = new FileWriter(df);
            BufferedWriter bw = new BufferedWriter(fw);
            for(String s:readData){
                bw.write(s);
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void addData(String key,String... values){
        updateData();
        if (getLine("<"+key+">")==-1 && getLine("</"+key+">")==-1){
            List<String> list =Arrays.asList(values);
            list.add(0,"<"+key+">");
            list.add(list.size(),"</"+key+">");
            readData.addAll(list);
        }
        readData.addAll(getLine("<"+key+">")+1, Arrays.asList(values));
        save();
    }


    public void removeData(String key,String... values){
        updateData();
        for(int i = getLine("<"+key+">")+1;i<getLine("</"+key+">");)
            if(Arrays.asList(values).contains(readData.get(i)))readData.remove(i);
        save();
    }


    /**
     * get data from file .datafile
     *
     *
     * @param key key of data.
     * @return got data from the key. If the key not found,returns null.
     */


    public String[] getData(String key){
        updateData();
        if (getLine("<"+key+">")!=-1 && getLine("</"+key+">")!=-1)
            return readData.subList(getLine("<"+key+">")+1,getLine("</"+key+">")).toArray(new String[0]);
        else
            return null;
    }


    private int getLine(String regex) {
        for(int i =0;i<readData.size();i++){
            if(readData.get(i).matches(regex)){
                return i;
            }
        }
        return -1;
    }

    private void updateData(){
        try {
            FileReader fr = new FileReader(df);
            BufferedReader br = new BufferedReader(fr);
            String data;
            readData = new ArrayList<>();
            do {
                data = br.readLine();
                if (data != null) readData.add(data);
            } while (data != null);
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
