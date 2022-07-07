package utils


import bean.Users
import com.google.gson.Gson
import config.CustomComm
import kotlinx.coroutines.GlobalScope
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.messageChainOf
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import pers.shennoter.ApexImage
import pers.shennoter.Config
import pers.shennoter.RankLookUp
import playerStat
import java.io.File

//使用事件通道监听查询自己信息的事件，只有这样才能获取到sender的qq号
fun playerRegister() {
    GlobalEventChannel.parentScope(GlobalScope).subscribeAlways<GroupMessageEvent> { event ->
        if (event.message.content == "/apex" || event.message.content == CustomComm.apex) {
            val gson = Gson()
            val users = gson.fromJson(File("${RankLookUp.dataFolder}/Users.json").readText(), Users::class.java)
            val qid = sender.id
            if (qid in users.data.keys) {
                users.data[qid]?.forEach {
                    Thread.sleep(2000)
                    val image = ApexImage()
                    var code = playerStat(it, image, Config.platform)
                    when (Config.mode) {
                        "pic" -> {
                            if (code == "查询成功") {
                                subject.sendMessage(messageChainOf(At(qid), image.get().uploadAsImage(subject)))
                                RankLookUp.logger.info(code)
                            } else { //查询错误则尝试其他平台
                                var unfound = true
                                listOf("PC","X1","PS4","SWITCH").forEach{ platform ->
                                    Thread.sleep(2000)
                                    code = playerStat(it, image, platform)
                                    when (Config.mode) {
                                        "pic" -> {
                                            if (code == "查询成功") {
                                                subject.sendMessage(messageChainOf(At(qid), image.get().uploadAsImage(subject)))
                                                RankLookUp.logger.info(code)
                                                unfound = false
                                            }
                                        }
                                        "text" -> {
                                            subject.sendMessage(messageChainOf(At(qid), PlainText(code!!)))
                                        }
                                        else -> subject.sendMessage("config.yml回复模式配置错误，请检查")
                                    }
                                }
                                if(unfound){
                                    RankLookUp.logger.error(code)
                                    subject.sendMessage(code!!)
                                }
                            }
                        }
                        "text" -> {  // 要分辨太麻烦，懒得写文字模式的多平台尝试了
                            subject.sendMessage(messageChainOf(At(qid), PlainText(code!!)))
                        }
                        else -> subject.sendMessage("config.yml回复模式配置错误，请检查")
                    }
                }
            } else {
                subject.sendMessage(message.quote() + "未绑定ID，请发送/apexbd绑定")
            }
        }
    }
}

fun playerBonder() {
    GlobalEventChannel.parentScope(GlobalScope).subscribeAlways<GroupMessageEvent> { event ->
        val pattern = "/apexbd [_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
        val patternCustom = "${CustomComm.apexubd}[_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
        if(event.message.content.matches(pattern) || event.message.content.matches(patternCustom)) { // 判断是否为指令
            val idPattern = "(?<=/apexbd )[_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
            val idPatternCustom = "(?<=${CustomComm.apexubd})[_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
            val finalPattern = "[_a-zA-Z0-9 ]+".toRegex()
            var found = idPattern.find(event.message.content) // 获取指令后内容
            if (found == null) {
                found = idPatternCustom.find(event.message.content)
            }
            if (found != null) {
                val id = found.value.trimStart()
                if (id.matches(finalPattern)) { // 判断是否包含汉字和特殊字符
                    val gson = Gson()
                    val users = gson.fromJson(File("${RankLookUp.dataFolder}/Users.json").readText(), Users::class.java)
                    val qid = sender.id
                    if ((qid in users.data.keys) && (users.data[qid]?.contains(id) == true)) {
                        subject.sendMessage(message.quote() + "该ID已绑定")
                    } else {
                        var url =
                            "https://api.mozambiquehe.re/bridge?version=5&platform=PC&player=${id}&auth=${Config.apiKey}"
                        var res = getRes(url)
                        if (res.first == 1) {
                            if (Config.extendApiKey.isNotEmpty()) { //如果api过热且config有额外apikey，则使用额外apikey重试
                                run breaking@{
                                    Config.extendApiKey.forEach {
                                        url =
                                            "https://api.mozambiquehe.re/bridge?version=5&platform=PC&player=${id}&auth=$it"
                                        res = getRes(url)
                                        if (res.first == 0) return@breaking
                                    }
                                }
                            }
                        }
                        if (res.first == 1) { // 检查其他平台
                            run breaking@{
                                listOf("PC", "X1", "PS4", "SWITCH").forEach { platform ->
                                    Thread.sleep(2000)
                                    url =
                                        "https://api.mozambiquehe.re/bridge?version=5&platform=${platform}&player=${id}&auth=${Config.apiKey}"
                                    res = getRes(url)
                                    if (res.first == 0) return@breaking
                                }
                            }
                        }
                        if (res.first == 1) { //如果还是不行就报错
                            subject.sendMessage(res.second!!)
                            RankLookUp.logger.error(res.second)
                        }
                        if (res.first == 0) {
                            users.data[qid]?.add(id)
                            File("${RankLookUp.dataFolder}/Users.json").writeText(gson.toJson(users))
                            subject.sendMessage(message.quote() + "绑定成功")
                        }
                    }
                }
                else{
                    subject.sendMessage(message.quote() + "ID格式错误")
                }
            }
        }
    }
}

fun playerUnbonder(){
    GlobalEventChannel.parentScope(GlobalScope).subscribeAlways<GroupMessageEvent> { event ->
        val pattern = "/apexubd [_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
        val patternCustom = "(?<=${CustomComm.apexubd})[_a-zA-Z0-9 ]+".toRegex()
        if(event.message.content.matches(pattern) || event.message.content.matches(patternCustom)) {    // 判断是否为指令
            val idPattern = "(?<=/apexubd )[_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
            val idPatternCustom = "(?<=${CustomComm.apexubd})[_a-zA-Z0-9\u4e00-\u9fa5 ]+".toRegex()
            val finalPattern = "[_a-zA-Z0-9 ]+".toRegex()
            var found = idPattern.find(event.message.content)   // 获取指令后内容
            if (found == null) {
                found = idPatternCustom.find(event.message.content)
            }
            if (found != null) {
                val id = found.value.trimStart()
                if (id.matches(finalPattern)) { // 判断是否包含汉字和特殊字符
                    val gson = Gson()
                    val users = gson.fromJson(File("${RankLookUp.dataFolder}/Users.json").readText(), Users::class.java)
                    val qid = sender.id
                    if (id == "all") {  // /apexubd all 解绑所有ID
                        users.data.remove(qid)
                        File("${RankLookUp.dataFolder}/Users.json").writeText(gson.toJson(users))
                        subject.sendMessage(message.quote() + "所有ID解绑成功")
                    }
                    else if ((qid in users.data.keys) && (users.data[qid]?.contains(id) == true)) {
                        users.data[qid]?.remove(id)
                        File("${RankLookUp.dataFolder}/Users.json").writeText(gson.toJson(users))
                        subject.sendMessage(message.quote() + "解绑成功")
                    }
                    else {
                        subject.sendMessage(message.quote() + "ID未绑定")
                    }
                }
                else{
                    subject.sendMessage(message.quote() + "ID格式错误")
                }
            }
        }
    }
}