package TitleSign;


import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.network.protocol.ModalFormRequestPacket;
import cn.nukkit.network.protocol.ModalFormResponsePacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Message extends PluginBase implements Listener{

    @Override
    public void onEnable() {
        if(!new File(this.getDataFolder()+"/config.yml").exists()){
            this.saveDefaultConfig();
            this.reloadConfig();
        }
        File file = new File(this.getDataFolder()+"/data.yml");
        if(!file.exists())
            this.getData();
        this.getServer().getPluginManager().registerEvents(this,this);
    }


    public Config getConfig(){
        this.reloadConfig();
        return super.getConfig();
    }

    private Config getData(){
        return new Config(this.getDataFolder()+"/data.yml",Config.YAML);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        this.getServer().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            public void onRun(int i) {
                showUI(player);
            }
        },80);
    }

    private LinkedHashMap<ButtonType,Object> getClickButton(String id){
        List<Map> maps = getConfig().getMapList("按键");
        if(maps.size() == 0) return null;
        LinkedHashMap<ButtonType,Object> button = new LinkedHashMap<>();
        for(Map OMap:maps){
            if(OMap.containsKey(ButtonType.ID.getName())){
                String ids = (String) OMap.get(ButtonType.ID.getName());
                if(ids.equals(id)){
                    for (ButtonType type:ButtonType.values()){
                        if(OMap.containsKey(type.getName()))
                            button.put(type,OMap.get(type.getName()));
                        else
                            button.put(type,type.getDefaultValue());
                        }
                    }
                }
        }
        return button;
    }

    private LinkedList<LinkedHashMap<ButtonType,Object>> getClickButtonAll(){
        List<Map> maps = getConfig().getMapList("按键");
        if(maps.size() == 0) return null;
        LinkedList<LinkedHashMap<ButtonType,Object>> lists = new LinkedList<>();
        for(Map OMap:maps){
            if(OMap.containsKey(ButtonType.ID.getName())){
                lists.add(getClickButton(String.valueOf(OMap.get(ButtonType.ID.getName()))));
            }
        }
        return lists;
    }


    private boolean canShow(Player player,String id){
        LinkedHashMap<ButtonType,Object> button = getClickButton(id);
        if(button != null){
            if(getData().getMapList(player.getName()) != null){
                if(isOutDay(player,id))
                    return true;
                else{
                    int playerClick = getPlayerClickCount(player,id);
                    int buttonClick = Integer.parseInt(String.valueOf(button.get(ButtonType.ClickCount)));
                    Server.getInstance().getLogger().info("playerClick: "+playerClick);
                    Server.getInstance().getLogger().info("buttonClick: "+buttonClick);
                    return playerClick == buttonClick;
                }
            }else
                return true;
        }
        return false;
    }


    //转换Date
    private Date getDate(String format){
        SimpleDateFormat lsdStrFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return lsdStrFormat.parse(format);
        }catch (ParseException e){
            return null;
        }
    }


    // 判断是否达到更新时间
    private boolean isOutDay(Player player, String id)
    {
        String date = getPlayerClickDate(player,id);
        if(date == null) return true;
        Date date1 = getDate(getPlayerClickDate(player,id));
        if(date1 != null){
            Calendar cal = Calendar.getInstance();
            cal.setTime(date1);
            long time1 = cal.getTimeInMillis();
            cal.setTime(new Date());
            long time2 = cal.getTimeInMillis();
            long between_days = (time2-time1)/(1000*3600*24);
            int day = Integer.parseInt(String.valueOf(between_days));
            LinkedHashMap<ButtonType,Object> button = getClickButton(id);
            if(button != null){
                int days = Integer.parseInt(String.valueOf(button.get(ButtonType.UpData)));
                return days == day;

            }
        }
        return false;
    }
    /*
    * player:
    *   - id:
    *     clickCount:
    *     date: */

    private void saveClickButton(Player player,String id){
        LinkedHashMap<ButtonType,Object> button = getClickButton(id);
        if(button != null){
            LinkedHashMap<String,Object> saves = new LinkedHashMap<>();
            Config data = getData();
            List<Map> playerConfig = new LinkedList<>();
            if(data.getMapList(player.getName()) != null){
                playerConfig = data.getMapList(player.getName());
                for(int i = 0;i < playerConfig.size();i++){
                    Map map = playerConfig.get(i);
                    if(String.valueOf(map.get(PlayerType.ID.getName())).equals(id)){
                        int playerClick = Integer.parseInt(String.valueOf(map.get(PlayerType.clickCount.getName())));
                        int buttonClick = Integer.parseInt(String.valueOf(button.get(ButtonType.ClickCount)));
                        saves.put(PlayerType.ID.getName(),map.get(PlayerType.ID.getName()));
                        if(playerClick < buttonClick){
                           saves.put(PlayerType.clickCount.getName(),playerClick+1);
                           saves.put(PlayerType.date.getName(),map.get(PlayerType.date.getName()));
                           playerConfig.set(i,saves);
                        }else{
                            saves.put(PlayerType.clickCount.getName(),map.get(PlayerType.clickCount.getName()));
                            saves.put(PlayerType.date.getName(),new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                            playerConfig.set(i,saves);
                        }
                        data.set(player.getName(),playerConfig);
                        data.save();
                        return;
                    }
                }
            }
            saves.put(PlayerType.ID.getName(),id);
            saves.put(PlayerType.date.getName(),new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            saves.put(PlayerType.clickCount.getName(),1);
            playerConfig.add(saves);
            data.set(player.getName(),playerConfig);
            data.save();
        }
    }


    private String getPlayerClickDate(Player player,String id){
        Config date = getData();
        if(date.get(player.getName()) != null){
            List<Map> getList = date.getMapList(player.getName());
            for(Map OMap:getList){
                if(OMap.containsKey(ButtonType.ID.getName())){
                    String name = String.valueOf(OMap.get(ButtonType.ID.getName()));
                    if(name.equals(id))
                        return String.valueOf(OMap.get(PlayerType.date.getName()));
                }

            }

        }
        return null;
    }

    private int getPlayerClickCount(Player player,String id){
        Config date = getData();
        if(date.get(player.getName()) != null){
            List<Map> getList = date.getMapList(player.getName());
            for(Map OMap:getList){
                if(OMap.containsKey(ButtonType.ID.getName())){
                    String name = String.valueOf(OMap.get(ButtonType.ID.getName()));
                    if(name.equals(id))
                        return Integer.parseInt(String.valueOf(OMap.get(PlayerType.clickCount.getName())));
                }

            }

        }
        return 0;
    }

    private void showUI(Player player){
        ModalFormRequestPacket ui = new ModalFormRequestPacket();
        ui.formId = 0xefeca01;
        LinkedList<LinkedHashMap<ButtonType, Object>> buttons = getClickButtonAll();
        ArrayList<Map<String,Object>> button = new ArrayList<>();
        if(buttons != null){
            for(LinkedHashMap<ButtonType,Object> btn : buttons){
                if(canShow(player,String.valueOf(btn.get(ButtonType.ID)))){
                    LinkedHashMap<String,Object> button_ = new LinkedHashMap<>(),
                            image   = new LinkedHashMap<>();
                    button_.put("text",btn.get(ButtonType.Text));
                    image.put("type",btn.getOrDefault(ButtonType.BtnType,"path"));
                    image.put("data",btn.getOrDefault(ButtonType.BtnData,ButtonType.BtnData.getDefaultValue()));
                    button_.put("image",image);
                    button.add(button_);
                }
            }
        }
        LinkedHashMap<String,Object> data = new LinkedHashMap<>();
        data.put("type","form");
        data.put("title",getConfig().getString("公告标题").replace("{n}","\n").replace("{p}",player.getName()));
        data.put("content",getConfig().getString("公告").replace("{n}","\n").replace("{p}",player.getName()));
        data.put("buttons",button);
        ui.data = new GsonBuilder().setPrettyPrinting().create().toJson(data);
        player.dataPacket(ui);
    }
    @EventHandler
    public void onclickUi(DataPacketReceiveEvent event){
        String data;
        ModalFormResponsePacket ui;
        Player player = event.getPlayer();
        if(!(event.getPacket() instanceof ModalFormResponsePacket)) return;
        ui = (ModalFormResponsePacket)event.getPacket();
        data = ui.data.trim();
        int fromId = ui.formId;
        switch (fromId){
            case 0xefeca01:
                if(data.equals("null")) return;
                LinkedList<LinkedHashMap<ButtonType, Object>> buttons = getClickButtonAll();
                LinkedList<LinkedHashMap<ButtonType,Object>> showButtons = new LinkedList<>();
                if(buttons != null){
                    for (LinkedHashMap<ButtonType, Object> btn:buttons){
                        if(canShow(player,String.valueOf(btn.get(ButtonType.ID)))){
                            showButtons.add(btn);
                        }
                    }
                    LinkedHashMap<ButtonType,Object> button = showButtons.get(Integer.parseInt(data));
                    saveClickButton(player,String.valueOf(button.get(ButtonType.ID)));
                    ArrayList commands = (ArrayList) button.getOrDefault(ButtonType.ClickCommand,ButtonType.ClickCommand.getDefaultValue());
                    for (Object command:commands){
                        if(command instanceof String)
                            Server.getInstance().dispatchCommand(player,(((String) command).replace("@p",player.getName())));
                    }
                }
                break;
        }

    }



    public enum ButtonType{
        ID("ID","click"),
        Text("文本","按键名称"),
        BtnType("图片访问类型","path"),
        BtnData("图片路径",""),
        UpData("更新时间(天)",1),
        ClickCount("限制点击次数",1),
        ClickCommand("Click",new ArrayList<String>());
        protected String name;
        protected Object defaultValue;
        ButtonType(String name,Object defaultValue){
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    public enum PlayerType{
        ID("ID"),
        clickCount("clickCount"),
        date("date");
        protected String name;
        PlayerType(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}

