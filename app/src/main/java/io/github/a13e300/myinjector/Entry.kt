package io.github.a13e300.myinjector

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.a13e300.myinjector.bridge.LoadPackageParam
import io.github.a13e300.myinjector.telegram.TelegramHandler

class Entry : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        lateinit var modulePath: String
        val moduleRes: XModuleResources by lazy {
            XModuleResources.createInstance(
                modulePath,
                null
            )
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        logI("MyInjector: ${lpparam.packageName} ${lpparam.processName}")
        val handler = when (lpparam.packageName) {
            in listOf(
                "org.telegram.messenger",
                "org.telegram.messenger.web",
                "org.telegram.messenger.beta",
                "org.telegram.plus",
                "com.exteragram.messenger",
                "com.radolyn.ayugram",
                "uz.unnarsx.cherrygram",
                "xyz.nextalone.nagram",
                "nu.gpu.nagram",
                "com.xtaolabs.pagergram"
            ) -> TelegramHandler
            else -> return
        }
        logPrefix = "[${handler.javaClass.simpleName}] "
        handler.hook(LoadPackageParam(lpparam))
    }
}
