package com.example.data.verification

import android.util.Log
import com.example.data.easyslip.EasySlipRateLimitInfo
import com.example.data.easyslip.VerifySlipResponse
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Multi-provider bank slip verification.
 *
 * Providers (all verify a raw PromptPay / EMVCo QR payload):
 *  - EasySlip:   POST https://api.easyslip.com/v2/verify/bank,  Authorization: Bearer <key>
 *  - ThunderAPI: POST https://api.thunder.in.th/v2/verify/bank, Authorization: Bearer <key>
 *  - Slip2Go:    POST https://api.slip2go.com/api/verify-slip/qr-code/info, Authorization: <secret> (no Bearer)
 */
class SlipVerificationManager {
    private var provider = VerificationProvider.EASYSLIP
    private var apiKey = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _rateLimitInfo = MutableStateFlow(EasySlipRateLimitInfo())
    val rateLimitInfo: StateFlow<EasySlipRateLimitInfo> = _rateLimitInfo.asStateFlow()

    fun updateConfig(p: VerificationProvider, key: String) {
        provider = p
        apiKey = key.trim()
    }

    fun currentProvider(): VerificationProvider = provider

    suspend fun verifyPayload(
        payload: String,
        checkDuplicate: Boolean = false,
        matchAmount: Double? = null
    ): VerifySlipResponse = withContext(Dispatchers.IO) {
        // No API key configured → local simulation (demo mode)
        if (apiKey.isEmpty()) {
            return@withContext simulateSlipVerification(payload)
        }

        try {
            when (provider) {
                VerificationProvider.EASYSLIP -> verifyEasySlip(payload, checkDuplicate, matchAmount)
                VerificationProvider.THUNDER -> verifyThunder(payload, checkDuplicate, matchAmount)
                VerificationProvider.SLIP2GO -> verifySlip2Go(payload, checkDuplicate)
            }
        } catch (e: Exception) {
            Log.w("SlipVerification", "Verify error via ${provider.label}: ${e.message}")
            simulateSlipVerification(payload)
        }
    }

    // ---------- EasySlip ----------

    private fun verifyEasySlip(
        payload: String,
        checkDuplicate: Boolean,
        matchAmount: Double?
    ): VerifySlipResponse {
        val jsonObject = JSONObject().apply {
            put("payload", payload)
            put("checkDuplicate", checkDuplicate)
            matchAmount?.let { put("matchAmount", it) }
        }
        val request = Request.Builder()
            .url("https://api.easyslip.com/v2/verify/bank")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonObject.toJsonBody())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        updateRateLimit(response)

        return when {
            response.code == 429 -> rateLimitedResponse("EasySlip API quota limit reached. Please wait.")
            response.code == 404 -> {
                val errJson = runCatching { JSONObject(responseBody) }.getOrNull()
                VerifySlipResponse(
                    success = false,
                    errorCode = errJson?.optString("error.code") ?: "SLIP_NOT_FOUND",
                    errorMessage = errJson?.optString("error.message") ?: "Slip not found or invalid.",
                    verificationStatus = VerificationStatus.SLIP_NOT_FOUND
                )
            }
            !response.isSuccessful -> simulateSlipVerification(payload)
            else -> parseEasySlipResponse(JSONObject(responseBody))
        }
    }

    private fun parseEasySlipResponse(json: JSONObject): VerifySlipResponse {
        val success = json.optBoolean("success", false)
        val data = json.optJSONObject("data") ?: return invalidResponse()
        val isDup = data.optBoolean("isDuplicate", false)
        val rawSlip = data.optJSONObject("rawSlip") ?: JSONObject()
        val amount = rawSlip.optJSONObject("amount")?.optDouble("amount")

        val sender = rawSlip.optJSONObject("sender")
        val senderBank = sender?.optJSONObject("bank")
        val senderAccount = sender?.optJSONObject("account")
        val senderNameObj = senderAccount?.optJSONObject("name")

        val receiver = rawSlip.optJSONObject("receiver")
        val receiverBank = receiver?.optJSONObject("bank")
        val receiverAccount = receiver?.optJSONObject("account")
        val receiverNameObj = receiverAccount?.optJSONObject("name")

        val (date, time) = splitDate(rawSlip.optString("date"))

        return VerifySlipResponse(
            success = success,
            isDuplicate = isDup,
            isAmountMatched = data.optBoolean("isAmountMatched", true),
            transRef = rawSlip.optString("transRef").ifEmpty { null },
            sendingBank = senderBank?.optString("id")?.ifBlank { null }
                ?: senderBank?.optString("short")?.ifBlank { null },
            sendingBankName = senderBank?.optString("name")?.ifBlank { null },
            receivingBank = receiverBank?.optString("id")?.ifBlank { null }
                ?: receiverBank?.optString("short")?.ifBlank { null },
            receivingBankName = receiverBank?.optString("name")?.ifBlank { null },
            receiverName = nameFromJson(receiverNameObj),
            senderName = nameFromJson(senderNameObj),
            amount = amount,
            transDate = date,
            transTime = time,
            verificationStatus = if (isDup) VerificationStatus.DUPLICATE_DETECTED else VerificationStatus.VERIFIED
        )
    }

    // ---------- ThunderAPI ----------

    private fun verifyThunder(
        payload: String,
        checkDuplicate: Boolean,
        matchAmount: Double?
    ): VerifySlipResponse {
        val jsonObject = JSONObject().apply {
            put("payload", payload)
            put("checkDuplicate", checkDuplicate)
            matchAmount?.let { put("matchAmount", it) }
        }
        val request = Request.Builder()
            .url("https://api.thunder.in.th/v2/verify/bank")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonObject.toJsonBody())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        updateRateLimit(response)

        return when {
            response.code == 429 -> rateLimitedResponse("ThunderAPI quota limit reached. Please wait.")
            response.code == 404 -> notFoundResponse()
            !response.isSuccessful -> simulateSlipVerification(payload)
            else -> parseThunderResponse(JSONObject(responseBody))
        }
    }

    private fun parseThunderResponse(json: JSONObject): VerifySlipResponse {
        val success = json.optBoolean("success", false)
        val data = json.optJSONObject("data") ?: return invalidResponse()
        val isDup = data.optBoolean("isDuplicate", false)
        val amount = data.optJSONObject("amount")?.optDouble("amount")

        val sender = data.optJSONObject("sender")
        val senderBank = sender?.optJSONObject("bank")
        val senderNameObj = sender?.optJSONObject("account")?.optJSONObject("name")

        val receiver = data.optJSONObject("receiver")
        val receiverBank = receiver?.optJSONObject("bank")
        val receiverNameObj = receiver?.optJSONObject("account")?.optJSONObject("name")

        val (date, time) = splitDate(data.optString("date"))

        return VerifySlipResponse(
            success = success,
            isDuplicate = isDup,
            isAmountMatched = data.optBoolean("isAmountMatched", true),
            transRef = data.optString("transRef").ifEmpty { null },
            sendingBank = senderBank?.optString("id")?.ifBlank { null }
                ?: senderBank?.optString("short")?.ifBlank { null },
            sendingBankName = senderBank?.optString("name")?.ifBlank { null }
                ?: senderBank?.optString("short")?.ifBlank { null }?.let { getBankName(it) },
            receivingBank = receiverBank?.optString("id")?.ifBlank { null }
                ?: receiverBank?.optString("short")?.ifBlank { null },
            receivingBankName = receiverBank?.optString("name")?.ifBlank { null }
                ?: receiverBank?.optString("short")?.ifBlank { null }?.let { getBankName(it) },
            receiverName = nameFromJson(receiverNameObj),
            senderName = nameFromJson(senderNameObj),
            amount = amount,
            transDate = date,
            transTime = time,
            verificationStatus = if (isDup) VerificationStatus.DUPLICATE_DETECTED
                else if (success) VerificationStatus.VERIFIED else VerificationStatus.UNVERIFIED
        )
    }

    // ---------- Slip2Go ----------

    private fun verifySlip2Go(payload: String, checkDuplicate: Boolean): VerifySlipResponse {
        val jsonObject = JSONObject().apply {
            put(
                "payload",
                JSONObject().apply {
                    put("qrCode", payload)
                    if (checkDuplicate) {
                        put("checkCondition", JSONObject().apply { put("checkDuplicate", true) })
                    }
                }
            )
        }
        val request = Request.Builder()
            .url("https://api.slip2go.com/api/verify-slip/qr-code/info")
            // Slip2Go expects the secret raw in Authorization (no Bearer prefix)
            .addHeader("Authorization", apiKey)
            .post(jsonObject.toJsonBody())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        updateRateLimit(response)

        return when {
            response.code == 429 -> rateLimitedResponse("Slip2Go quota limit reached. Please wait.")
            !response.isSuccessful -> simulateSlipVerification(payload)
            else -> parseSlip2GoResponse(JSONObject(responseBody))
        }
    }

    private fun parseSlip2GoResponse(json: JSONObject): VerifySlipResponse {
        val code = json.optString("code")
        val message = json.optString("message")
        val success = code == "200000"
        val data = json.optJSONObject("data")
        if (data == null) {
            return VerifySlipResponse(
                success = false,
                errorCode = code.ifEmpty { "INVALID_RESPONSE" },
                errorMessage = message.ifEmpty { "Malformed response from Slip2Go." },
                verificationStatus = if (success) VerificationStatus.UNVERIFIED else VerificationStatus.SLIP_NOT_FOUND
            )
        }

        val isDup = message.contains("duplicate", ignoreCase = true) || data.optBoolean("isDuplicate", false)

        val sender = data.optJSONObject("sender")
        val senderBank = sender?.optJSONObject("bank")
        val receiver = data.optJSONObject("receiver")
        val receiverBank = receiver?.optJSONObject("bank")

        val (date, time) = splitDate(data.optString("dateTime"))

        return VerifySlipResponse(
            success = success,
            isDuplicate = isDup,
            isAmountMatched = true,
            transRef = data.optString("transRef").ifEmpty { null },
            sendingBank = senderBank?.optString("id")?.ifBlank { null },
            sendingBankName = senderBank?.optString("name")?.ifBlank { null },
            receivingBank = receiverBank?.optString("id")?.ifBlank { null },
            receivingBankName = receiverBank?.optString("name")?.ifBlank { null },
            amount = if (data.has("amount") && !data.isNull("amount")) data.optDouble("amount") else null,
            transDate = date,
            transTime = time,
            verificationStatus = when {
                isDup -> VerificationStatus.DUPLICATE_DETECTED
                success -> VerificationStatus.VERIFIED
                else -> VerificationStatus.SLIP_NOT_FOUND
            }
        ).copy(
            receiverName = receiver?.optJSONObject("account")?.optString("name")?.ifBlank { null },
            senderName = sender?.optJSONObject("account")?.optString("name")?.ifBlank { null }
        )
    }

    // ---------- Shared ----------

    private fun simulateSlipVerification(payload: String): VerifySlipResponse {
        val isDuplicate = payload.endsWith("9999")
        val status = if (isDuplicate) VerificationStatus.DUPLICATE_DETECTED else VerificationStatus.UNVERIFIED
        return VerifySlipResponse(
            success = false,
            isDuplicate = isDuplicate,
            matchedAccount = null,
            isAmountMatched = false,
            transRef = null,
            sendingBank = null,
            sendingBankName = null,
            receivingBank = null,
            receivingBankName = null,
            receiverName = null,
            senderName = null,
            amount = extractAmount(payload),
            transDate = null,
            transTime = null,
            errorMessage = if (isDuplicate) "Duplicate slip"
                else "${provider.label} verification not configured — enable in Settings to verify",
            verificationStatus = status
        )
    }

    private fun rateLimitedResponse(message: String): VerifySlipResponse {
        _rateLimitInfo.value = _rateLimitInfo.value.copy(isThrottled = true)
        return VerifySlipResponse(
            success = false,
            errorCode = "RATE_LIMIT_EXCEEDED",
            errorMessage = message,
            verificationStatus = VerificationStatus.RATE_LIMITED
        )
    }

    private fun notFoundResponse(): VerifySlipResponse = VerifySlipResponse(
        success = false,
        errorCode = "SLIP_NOT_FOUND",
        errorMessage = "Slip not found or invalid.",
        verificationStatus = VerificationStatus.SLIP_NOT_FOUND
    )

    private fun invalidResponse(): VerifySlipResponse = VerifySlipResponse(
        success = false,
        errorCode = "INVALID_RESPONSE",
        errorMessage = "Malformed response from ${provider.label}.",
        verificationStatus = VerificationStatus.UNVERIFIED
    )

    private fun updateRateLimit(response: okhttp3.Response) {
        val remainingHeader = response.header("X-RateLimit-Remaining")?.toIntOrNull()
        if (remainingHeader != null) {
            _rateLimitInfo.value = _rateLimitInfo.value.copy(remainingQuota = remainingHeader)
        }
    }

    private fun nameFromJson(nameObj: JSONObject?): String? {
        if (nameObj == null) return null
        return nameObj.optString("th").ifBlank { nameObj.optString("en") }.ifBlank { null }
    }

    private fun splitDate(isoDate: String): Pair<String?, String?> {
        if (isoDate.isBlank()) return null to null
        return runCatching {
            var datePart = isoDate.take(10)
            val timePartRaw = isoDate.substringAfterLast("T").substringAfterLast(" ").take(8)
            val time = if (timePartRaw.contains(":")) {
                val parts = timePartRaw.split(":")
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else null
            } else null
            if (datePart.contains("/")) {
                val dp = datePart.split("/")
                if (dp.size == 3 && dp[2].length == 4) {
                    datePart = "${dp[2]}-${dp[1].padStart(2, '0')}-${dp[0].padStart(2, '0')}"
                }
            }
            datePart to time
        }.getOrDefault(null to null)
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""\d{1,3}(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?""")
        return regex.find(text)?.value?.replace(",", "")?.toDoubleOrNull()
    }

    private fun JSONObject.toJsonBody(): okhttp3.RequestBody =
        toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    fun getBankName(code: String): String {
        return when (code) {
            "004" -> "Kasikornbank (KBank)"
            "014" -> "Siam Commercial Bank (SCB)"
            "002" -> "Bangkok Bank (BBL)"
            "006" -> "Krungthai Bank (KTB)"
            "025" -> "Bank of Ayudhya (Krungsri)"
            "011" -> "TMBThanachart Bank (ttb)"
            "022" -> "CIMB Thai"
            "030" -> "Government Savings Bank (GSB)"
            "KBANK" -> "Kasikornbank (KBank)"
            "SCB" -> "Siam Commercial Bank (SCB)"
            "BBL" -> "Bangkok Bank (BBL)"
            "KTB" -> "Krungthai Bank (KTB)"
            "BAY" -> "Bank of Ayudhya (Krungsri)"
            "TTB" -> "TMBThanachart Bank (ttb)"
            else -> "Bank $code"
        }
    }
}
