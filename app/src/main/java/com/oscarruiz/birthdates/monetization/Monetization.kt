package com.oscarruiz.birthdates.monetization

import android.app.Activity
import android.content.Context
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
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.oscarruiz.birthdates.BuildConfig
import com.oscarruiz.birthdates.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Compra única "Pro" con Google Play Billing. La compra va ligada a la cuenta de Google,
 * así que al reinstalar se recupera con queryPurchasesAsync sin cuentas propias.
 */
class BillingRepository(
    context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    companion object {
        /** Debe coincidir con el producto creado en Play Console (compra única, no consumible). */
        const val PRO_PRODUCT_ID = "remove_ads_pro"
    }

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var productDetails: ProductDetails? = null
    private val _proPrice = MutableStateFlow<String?>(null)
    val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    fun connect() {
        if (client.isReady) {
            scope.launch { refresh() }
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { refresh() }
            }

            override fun onBillingServiceDisconnected() = Unit // Se reintenta en el próximo connect().
        })
    }

    /** Consulta las compras de la cuenta. Devuelve true/false, o null si Play no responde. */
    suspend fun refresh(): Boolean? {
        if (!client.isReady) return null
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        val isPro = handlePurchases(result.purchasesList)
        settings.setPro(isPro) // También quita Pro si la compra se reembolsó.
        loadProductDetails()
        return isPro
    }

    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { if (handlePurchases(purchases)) settings.setPro(true) }
        }
    }

    /** Reconoce las compras nuevas (si no, Google las reembolsa a los 3 días). */
    private suspend fun handlePurchases(purchases: List<Purchase>): Boolean {
        var isPro = false
        for (purchase in purchases) {
            if (PRO_PRODUCT_ID !in purchase.products) continue
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            isPro = true
            if (!purchase.isAcknowledged) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                )
            }
        }
        return isPro
    }

    private suspend fun loadProductDetails() {
        if (productDetails != null) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val details = client.queryProductDetails(params).productDetailsList?.firstOrNull() ?: return
        productDetails = details
        _proPrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice
    }
}

/** Consentimiento RGPD con el SDK UMP de Google (obligatorio para mostrar anuncios en la UE). */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                // En depuración se simula estar en la UE (solo afecta a emuladores y dispositivos de prueba).
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build(),
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onDone() } },
            { onDone() },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}

/**
 * Arranca consentimiento y AdMob una sola vez por proceso. Los usuarios Pro nunca llegan aquí.
 * La UI solo muestra el banner cuando [adsReady] es true y el usuario no es Pro.
 */
class AdsController(
    private val context: Context,
    private val consent: ConsentManager,
    private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        consent.gatherConsent(activity) {
            if (consent.canRequestAds) initializeAds()
        }
        // Si ya había consentimiento de otra sesión, no hace falta esperar al formulario.
        if (consent.canRequestAds) initializeAds()
    }

    private fun initializeAds() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context.applicationContext) { _adsReady.value = true }
        }
    }
}
