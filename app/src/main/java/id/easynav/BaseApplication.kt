package id.easynav

import android.app.Application
import com.huawei.hms.maps.MapsInitializer

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this)
        MapsInitializer.setApiKey("DAEDAMra+AooQA9+S1B56nF18Ad98duMqSfgDNaQwoShOP2Y3wIwd0X9qjwhQqi0Y5mwpsCch9w66zrOzd0qOtTN2wiNjHfWjGk1+Q==")
    }
}