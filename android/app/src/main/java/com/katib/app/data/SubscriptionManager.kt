package com.katib.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Wraps Google Play Billing for Katib Premium subscriptions. This is the
 * Android counterpart to the spec's StoreKit 2 SubscriptionManager.
 *
 * It is defensive by design: if Play products are not yet configured (e.g. in
 * development before App Store/Play Console setup), the app still runs — premium
 * simply stays locked, and [debugUnlock] can flip the local flag for testing.
 */
class SubscriptionManager(
    private val appContext: Context,
    private val prefs: Prefs,
) {
    companion object {
        private const val TAG = "KatibBilling"
        const val MONTHLY_PRODUCT_ID = "katib_monthly"
        const val ANNUAL_PRODUCT_ID = "katib_annual"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** Connect and reconcile current entitlement on app start. */
    fun start() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadProductDetails()
                        refreshEntitlement()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private suspend fun loadProductDetails() {
        val products = listOf(MONTHLY_PRODUCT_ID, ANNUAL_PRODUCT_ID).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        runCatching {
            val result = billingClient.queryProductDetails(params)
            result.productDetailsList?.forEach { productDetailsCache[it.productId] = it }
        }.onFailure { Log.w(TAG, "queryProductDetails failed", it) }
    }

    /** Re-check what the user actually owns and update [isPremium]. */
    suspend fun refreshEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        runCatching {
            val result = billingClient.queryPurchasesAsync(params)
            val active = result.purchasesList.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            // Honour either a real purchase or a previously stored (debug) unlock.
            val stored = prefsPremiumBlocking()
            setPremium(active || stored)
            result.purchasesList.forEach { handlePurchase(it) }
        }.onFailure { Log.w(TAG, "queryPurchases failed", it) }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetailsCache[productId]
        if (details == null) {
            Log.w(TAG, "Product $productId not loaded yet")
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            Log.w(TAG, "No offer token for $productId")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    fun restore() {
        scope.launch { refreshEntitlement() }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        setPremium(true)
        if (!purchase.isAcknowledged) {
            scope.launch {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                runCatching { billingClient.acknowledgePurchase(params) }
            }
        }
    }

    private fun setPremium(value: Boolean) {
        _isPremium.value = value
        scope.launch { prefs.setPremium(value) }
    }

    /** Dev-only: unlock premium locally without a real Play purchase. */
    fun debugUnlock(value: Boolean) = setPremium(value)

    private suspend fun prefsPremiumBlocking(): Boolean = prefs.isPremium.first()
}
