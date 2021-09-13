package com.attafitamim.nfc.view.destinations.cards.scan.model

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attafitamim.nfc.view.destinations.cards.global.asBankCardPayload
import com.attafitamim.nfc.view.nfc.INfcTagHost
import com.attafitamim.nfc.view.nfc.INfcTagListener
import com.attafitamim.nfc.view.nfc.NfcEmvProvider
import com.github.devnied.emvnfccard.parser.EmvTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import java.lang.Exception

class BankCardScanViewModel(
    private val parser: EmvTemplate,
    private val provider: NfcEmvProvider,
    private val nfcHost: INfcTagHost
) : ViewModel(), ContainerHost<BankCardScanState, BankCardScanSideEffect>, INfcTagListener {

    override val container by lazy {
        val initialState = BankCardScanState()
        container<BankCardScanState, BankCardScanSideEffect>(initialState)
    }

    init {
        nfcHost.registerListener(this)
    }

    override fun onCleared() {
        nfcHost.unregisterListener(this)
        super.onCleared()
    }

    override fun onNewTag(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) readCard(isoDep)
    }

    private fun readCard(isoDep: IsoDep) = intent {
        val scanSideEffect = BankCardScanSideEffect.ShowScanStartToast
        postSideEffect(scanSideEffect)

        try {
            provider.tag = isoDep
            val card = parser.readEmvCard()
            val cardPayload = card.asBankCardPayload

            reduce {
                state.copy(cardPayload = cardPayload)
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: e.toString()
            val errorSideEffect = BankCardScanSideEffect.ShowScanErrorToast(errorMessage)
            postSideEffect(errorSideEffect)
        }
    }
}