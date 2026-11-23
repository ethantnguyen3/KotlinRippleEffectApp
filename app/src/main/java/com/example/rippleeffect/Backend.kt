package com.example.rippleeffect

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val BACKEND_TAG = "DataConnectBackend"

/**
 * NOTE:
 * This app calls Firebase Data Connect via its GraphQL endpoint.
 * Ensure your deployed connector has operations compatible with the two documents below:
 * 1) `ListSubmittedEntries`
 * 2) `CreateSubmittedEntry`
 *
 * Configure endpoint values in `gradle.properties`:
 * DATACONNECT_SERVICE_URL=https://<region>-<project>.cloudfunctions.net/<connector>
 * DATACONNECT_API_KEY=<web-api-key>
 */
object Backend {
	private val isoUtcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	private val serviceUrl: String
		get() = BuildConfig.DATACONNECT_SERVICE_URL.trim()

	private val apiKey: String
		get() = BuildConfig.DATACONNECT_API_KEY.trim()

	private val isConfigured: Boolean
		get() = serviceUrl.isNotBlank() && apiKey.isNotBlank()

	suspend fun fetchSubmittedEntries(): List<SubmittedEntry> = withContext(Dispatchers.IO) {
		if (!isConfigured) {
			Log.w(BACKEND_TAG, "Data Connect is not configured. Returning local-only state.")
			return@withContext emptyList()
		}

		val requestBody = JSONObject().apply {
			put(
				"query",
				"""
				query ListSubmittedEntries {
				  submittedEntries {
					id
					description
					amount
					currency
					type
					dateCreated
				  }
				}
				""".trimIndent()
			)
			put("variables", JSONObject())
		}

		runCatching {
			val response = executeGraphql(requestBody)
			val items = response
				.optJSONObject("data")
				?.optJSONArray("submittedEntries")
				?: JSONArray()

			buildList {
				for (i in 0 until items.length()) {
					val node = items.optJSONObject(i) ?: continue
					add(
						SubmittedEntry(
							id = node.optString("id", java.util.UUID.randomUUID().toString()),
							description = node.optString("description", ""),
							amount = node.optDouble("amount", 0.0),
							currency = parseCurrency(node.optString("currency", Currency.USD.code)),
							type = node.optString("type", "Sale"),
							dateCreated = parseDate(node.optString("dateCreated"))
						)
					)
				}
			}
		}.onFailure {
			Log.e(BACKEND_TAG, "Failed to fetch entries from Data Connect", it)
		}.getOrDefault(emptyList())
	}

	suspend fun saveSubmittedEntries(entries: List<AccountingEntry>, currency: Currency) = withContext(Dispatchers.IO) {
		if (!isConfigured) {
			Log.w(BACKEND_TAG, "Data Connect is not configured. Skipping remote save.")
			return@withContext
		}

		entries.forEach { entry ->
			val requestBody = JSONObject().apply {
				put(
					"query",
					"""
					mutation CreateSubmittedEntry(
					  ${'$'}description: String!,
					  ${'$'}amount: Float!,
					  ${'$'}currency: String!,
					  ${'$'}type: String!,
					  ${'$'}dateCreated: Timestamp!
					) {
					  submittedEntry_insert(data: {
						description: ${'$'}description,
						amount: ${'$'}amount,
						currency: ${'$'}currency,
						type: ${'$'}type,
						dateCreated: ${'$'}dateCreated
					  }) {
						id
					  }
					}
					""".trimIndent()
				)
				put(
					"variables",
					JSONObject().apply {
						put("description", entry.type)
						put("amount", entry.amount)
						put("currency", currency.code)
						put("type", if (entry.isExpense) "Expense" else "Sale")
						put("dateCreated", isoUtcDateFormat.format(Date()))
					}
				)
			}

			runCatching { executeGraphql(requestBody) }
				.onFailure { Log.e(BACKEND_TAG, "Failed to save entry: ${entry.type}", it) }
		}
	}

	private fun executeGraphql(body: JSONObject): JSONObject {
		val endpoint = URL(serviceUrl)
		val conn = (endpoint.openConnection() as HttpURLConnection).apply {
			requestMethod = "POST"
			connectTimeout = 15000
			readTimeout = 15000
			doOutput = true
			setRequestProperty("Content-Type", "application/json")
			setRequestProperty("x-goog-api-key", apiKey)
		}

		OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
			writer.write(body.toString())
			writer.flush()
		}

		val raw = try {
			BufferedReader(conn.inputStream.reader(Charsets.UTF_8)).use { it.readText() }
		} catch (ex: Exception) {
			val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
			throw IllegalStateException("GraphQL request failed [${conn.responseCode}] ${errorBody ?: ex.message}", ex)
		} finally {
			conn.disconnect()
		}

		val json = JSONObject(raw)
		if (json.has("errors")) {
			throw IllegalStateException("GraphQL errors: ${json.getJSONArray("errors")}")
		}
		return json
	}

	private fun parseCurrency(code: String): Currency {
		return Currency.entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Currency.USD
	}

	private fun parseDate(iso: String?): Date {
		if (iso.isNullOrBlank()) return Date()
		return runCatching { isoUtcDateFormat.parse(iso) ?: Date() }.getOrDefault(Date())
	}
}

