package com.katib.app

import android.app.Application
import com.katib.app.data.Prefs
import com.katib.app.data.SubscriptionManager
import com.katib.app.net.KatibApiClient

/**
 * Holds app-wide singletons. Kept deliberately simple (no DI framework) so the
 * keyboard service and the main activity can reach the same instances.
 */
class KatibApplication : Application() {

    lateinit var prefs: Prefs
        private set
    lateinit var subscriptions: SubscriptionManager
        private set
    lateinit var api: KatibApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        subscriptions = SubscriptionManager(this, prefs)
        api = KatibApiClient()
        subscriptions.start()
    }
}
