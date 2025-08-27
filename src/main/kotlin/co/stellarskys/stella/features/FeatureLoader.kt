package co.stellarskys.stella.features

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis
import org.reflections.Reflections
//#if MC >= 1.21.5
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//#elseif MC == 1.8.9
//$$ import net.minecraft.command.ICommand
//$$ import net.minecraftforge.client.ClientCommandHandler
//#endif

object FeatureLoader {
    private var moduleCount = 0
    private var commandCount = 0
    private var loadtime: Long = 0

    fun init() {
        val reflections = Reflections("co.stellarskys.stella")

        val features = reflections.getTypesAnnotatedWith(Stella.Module::class.java)
        val starttime = TimeUtils.now
        //val categoryOrder = listOf("dungeons", "stellanav", "msc")
        val categoryOrder = listOf<String>()

        features.sortedWith(compareBy<Class<*>> { clazz ->
            val packageName = clazz.`package`.name
            val category = packageName.substringAfterLast(".")
            categoryOrder.indexOf(category).takeIf { it != -1 } ?: Int.MAX_VALUE
        }.thenBy { it.name }).forEach { clazz ->
            try {
                Class.forName(clazz.name)
                moduleCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val commands = reflections.getTypesAnnotatedWith(Stella.Command::class.java)

        //#if MC >= 1.21.5
        commandCount = commands.size
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { commandClass ->
                try {
                    val commandInstance = commandClass.getDeclaredField("INSTANCE").get(null)
                    val registerMethod = commandClass.methods.find { it.name == "register" } // a bit eh but it works
                    registerMethod?.invoke(commandInstance, dispatcher)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        //#elseif MC == 1.8.9
        //$$ commands.forEach { commandClass ->
        //$$    try {
        //$$        val commandInstance = commandClass.getDeclaredField("INSTANCE").get(null) as ICommand
        //$$        ClientCommandHandler.instance.registerCommand(commandInstance)
        //$$        commandCount++
        //$$    } catch (e: Exception) {
        //$$        e.printStackTrace()
        //$$    }
        //$$ }
        //$$ println("[Stella] got past 1.21.5 check")
        //$$ println("command count: $commandCount")
        //#endif

        loadtime = starttime.since.millis
    }

    fun getFeatCount(): Int = moduleCount
    fun getCommandCount(): Int = commandCount
    fun getLoadtime(): Long = loadtime
}