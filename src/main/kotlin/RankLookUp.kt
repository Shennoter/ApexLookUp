package pers.shennoter


import Cache
import Craft
import Help
import LeaderBoard
import Listener
import ListenerRemove
import News
import Player
import Predator
import Map
import com.google.gson.Gson
import config.CustomComm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import utils.*
import java.io.File



object RankLookUp : KotlinPlugin(
    JvmPluginDescription(
        id = "pers.shennoter.RankLookUp",
        name = "ApexLookUp",
        version = "1.5.4",
    ){
        author("Shennoter")
    }
){
    override fun onEnable() {
        logger.info("Apex查询插件已启动")
        Config.reload()
        CustomComm.reload()
        CommandManager.registerCommand(Player)
        CommandManager.registerCommand(Map)
        CommandManager.registerCommand(Craft)
        CommandManager.registerCommand(Predator)
        CommandManager.registerCommand(News)
        CommandManager.registerCommand(Cache)
        CommandManager.registerCommand(Listener)
        CommandManager.registerCommand(ListenerRemove)
        CommandManager.registerCommand(Help)
        CommandManager.registerCommand(LeaderBoard)
        logger.info("众神之父赐予我视野！")
        //清理缓存
        if(Config.cacheAutoDel){
            RankLookUp.logger.info(removeCache(true))
        }
        createFiles()
        startListening()
        playerRegister()
        playerBonder()
        playerUnbonder()
    }

    override fun onDisable(){
        Config.save()
        CustomComm.save()
        CommandManager.unregisterCommand(Player)
        CommandManager.unregisterCommand(Map)
        CommandManager.unregisterCommand(Craft)
        CommandManager.unregisterCommand(Predator)
        CommandManager.unregisterCommand(News)
        CommandManager.unregisterCommand(Cache)
        CommandManager.unregisterCommand(Listener)
        CommandManager.unregisterCommand(ListenerRemove)
        CommandManager.unregisterCommand(Help)
        CommandManager.unregisterCommand(LeaderBoard)
        logger.info("我是布洛特·亨德尔，你可以叫我倒地了但还活着！")
    }
}

fun startListening(){
    if(Config.apiKey == ""){
        RankLookUp.logger.error("未找到ApiKey，请到https://apexlegendsapi.com/获取ApiKey填入./config/pers.shennoter.RankLookUp/config.yml中并重启mirai-console")
    }
    else{
        GlobalScope.launch { //启动监听任务
            val listendPlayer : ListendPlayer = Gson().fromJson(File("${RankLookUp.dataFolder}/Data.json").readText(), ListendPlayer::class.java)
            if(Config.listener && listendPlayer.data.size > 1) {
                playerStatListener()
            }
            val groups : GroupReminding = Gson().fromJson(File("${RankLookUp.dataFolder}/Reminder.json").readText(), GroupReminding::class.java)
            if(Config.mapRotationReminder && groups.data.size > 1){
                mapReminder()
            }
        }
    }
}



