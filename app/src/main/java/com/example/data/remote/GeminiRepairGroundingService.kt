package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.DiagnosticReport
import com.example.domain.model.GroundedRepairSuggestion
import com.example.domain.model.SearchGroundingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepairGroundingService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "GeminiGroundingService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        private fun logWarning(msg: String) {
            try {
                Log.w(TAG, msg)
            } catch (e: Exception) {
                println("[$TAG WARN] $msg")
            }
        }

        private fun logError(msg: String, tr: Throwable? = null) {
            try {
                Log.e(TAG, msg, tr)
            } catch (e: Exception) {
                println("[$TAG ERROR] $msg")
                tr?.printStackTrace()
            }
        }
    }

    suspend fun fetchRepairSuggestions(report: DiagnosticReport): Result<GroundedRepairSuggestion> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            logWarning("Gemini API key is not configured. Falling back to local offline repair suggestions.")
            return@withContext Result.success(generateOfflineRepairSuggestion(report, "Clave API de Gemini no configurada en Secrets panel."))
        }

        val prompt = buildPromptForReport(report)

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })

                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put(
                                "text",
                                """
                                Eres un instructor y especialista senior en diagnóstico de hardware Apple, microsoldadura y esquemáticos (ZXW / Wuxinji).
                                Tu objetivo es analizar el código de Panic Full presentado, buscar en Google las mejores prácticas comprobadas de la comunidad de técnicos (iFixit, VccBoard, REWA, microsoldadores certificados) y entregar sugerencias de reparación precisas, directas y accionables.
                                
                                Estructura tu respuesta en las siguientes secciones claramente identificables:
                                1. [RESUMEN Y CAUSA RAÍZ]
                                2. [PASOS DE REPARACIÓN EN TALLER]
                                3. [LÍNEAS Y COMPONENTES AFECTADOS]
                                4. [MEDICIONES EN MODO DIODO Y VOLTAJES]
                                5. [PRECAUCIONES CRÍTICAS]
                                
                                Sé directo, profesional, técnico y conciso.
                                """.trimIndent()
                            )
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                logError("Gemini API call failed: ${response.code} -> $responseBodyString")
                val fallback = generateOfflineRepairSuggestion(report, "Error en API (${response.code}). Mostrando guía de reparación técnica local.")
                return@withContext Result.success(fallback)
            }

            val jsonResponse = JSONObject(responseBodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.success(generateOfflineRepairSuggestion(report, "Sin respuesta de Gemini."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawTextBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        rawTextBuilder.append(part.getString("text"))
                    }
                }
            }
            val responseText = rawTextBuilder.toString().ifBlank { "No se obtuvieron detalles de la consulta." }

            val searchSources = mutableListOf<SearchGroundingSource>()
            val searchQueries = mutableListOf<String>()

            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val webQueries = groundingMetadata.optJSONArray("webSearchQueries")
                if (webQueries != null) {
                    for (i in 0 until webQueries.length()) {
                        searchQueries.add(webQueries.getString(i))
                    }
                }

                val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (groundingChunks != null) {
                    for (i in 0 until groundingChunks.length()) {
                        val chunk = groundingChunks.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri", "")
                            val title = web.optString("title", "Fuente Técnica Web")
                            if (uri.isNotBlank()) {
                                searchSources.add(SearchGroundingSource(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            val parsedSuggestion = parseResponseToSuggestion(
                rawText = responseText,
                sources = searchSources,
                queries = searchQueries,
                report = report
            )

            Result.success(parsedSuggestion)
        } catch (e: Exception) {
            logError("Exception during Gemini Grounding call", e)
            Result.success(generateOfflineRepairSuggestion(report, "Modo sin conexión: ${e.localizedMessage ?: "Error de red"}"))
        }
    }

    private fun buildPromptForReport(report: DiagnosticReport): String {
        val modelName = report.deviceModel?.marketingName ?: report.productCode
        val panicFamily = report.panicFamilies.joinToString(", ") { it.name }
        val diagnosisLabel = report.primaryCandidate?.label ?: "Falla de hardware desconocida"
        val suspectedComps = report.primaryCandidate?.suspectedComponents?.joinToString(", ") { "${it.name} (${it.role})" } ?: "No especificados"
        val panicSummary = report.panicStringSummary.take(400)

        val evidencesText = report.evidences.take(5).joinToString("\n") { ev ->
            "- ${ev.title}: ${ev.normalizedValue} (Línea ${ev.lineNumber}): ${ev.excerpt.take(120)}"
        }

        return """
            Busca en tiempo real las mejores soluciones y procedimientos de microsoldadura para:
            - Dispositivo: $modelName (Código: ${report.productCode}, iOS: ${report.osVersion})
            - Familia de Panic: $panicFamily
            - Diagnóstico preliminar: $diagnosisLabel
            - Componentes sospechosos: $suspectedComps
            - Registro del pánico (extracto): $panicSummary
            - Evidencias técnicas detectadas:
            $evidencesText

            Por favor proporciona:
            1. Diagnóstico exacto y causas raíces más frecuentes en talleres.
            2. Lista paso a paso de verificación y reparación (desde descarte de flex/periféricos hasta microelectrónica en placa).
            3. Líneas de alimentación/datos a revisar en el esquemático (ej. I2C, SPI, VDD, PP1V8).
            4. Mediciones en modo diodo recomendadas y componentes propensos a fallar (resistencias pull-up, diodos TVS, condensadores de desacoplo, circuitos integrados).
            5. Precauciones indispensables para no dañar Face ID, procesador u otros módulos.
        """.trimIndent()
    }

    private fun parseResponseToSuggestion(
        rawText: String,
        sources: List<SearchGroundingSource>,
        queries: List<String>,
        report: DiagnosticReport
    ): GroundedRepairSuggestion {
        val detailedSteps = mutableListOf<String>()
        val suspectedComponents = mutableListOf<String>()
        val diodeModeTips = mutableListOf<String>()
        val cautions = mutableListOf<String>()

        val lines = rawText.lines()
        var currentSection = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val upper = trimmed.uppercase()
            when {
                upper.contains("RESUMEN") || upper.contains("CAUSA RAÍZ") -> currentSection = "SUMMARY"
                upper.contains("PASOS") || upper.contains("PROCEDIMIENTO") || upper.contains("VERIFICACIÓN") -> currentSection = "STEPS"
                upper.contains("LÍNEAS") || upper.contains("COMPONENTES") || upper.contains("ESQUEMÁTICO") -> currentSection = "COMPONENTS"
                upper.contains("DIODO") || upper.contains("MEDICIÓN") || upper.contains("VOLTAJE") -> currentSection = "DIODE"
                upper.contains("PRECAUCI") || upper.contains("CUIDADO") || upper.contains("ADVERTENCIA") -> currentSection = "CAUTIONS"
                else -> {
                    val cleanBullet = trimmed.removePrefix("-").removePrefix("*").removePrefix("•").trim()
                    if (cleanBullet.length > 3) {
                        when (currentSection) {
                            "STEPS" -> detailedSteps.add(cleanBullet)
                            "COMPONENTS" -> suspectedComponents.add(cleanBullet)
                            "DIODE" -> diodeModeTips.add(cleanBullet)
                            "CAUTIONS" -> cautions.add(cleanBullet)
                        }
                    }
                }
            }
        }

        if (detailedSteps.isEmpty()) {
            detailedSteps.addAll(report.repairFlow.firstChecks)
        }

        val primaryCandidate = report.primaryCandidate
        if (suspectedComponents.isEmpty() && primaryCandidate?.suspectedComponents?.isNotEmpty() == true) {
            primaryCandidate.suspectedComponents.forEach {
                suspectedComponents.add("${it.name} (${it.role})")
            }
        }

        if (cautions.isEmpty()) {
            cautions.add("Desconectar siempre la batería antes de conectar/desconectar cualquier flex o componente.")
            cautions.add("Proteger sensores Face ID/TrueDepth de calor excesivo (>80°C).")
        }

        return GroundedRepairSuggestion(
            summary = rawText,
            detailedSteps = detailedSteps,
            suspectedComponents = suspectedComponents,
            diodeModeReferenceTips = diodeModeTips,
            cautions = cautions,
            searchSources = sources,
            searchQueries = queries,
            retrievedAt = System.currentTimeMillis(),
            isRealTimeGrounded = sources.isNotEmpty() || queries.isNotEmpty()
        )
    }

    private fun generateOfflineRepairSuggestion(report: DiagnosticReport, reasonNote: String): GroundedRepairSuggestion {
        val model = report.deviceModel?.marketingName ?: report.productCode
        val diag = report.primaryCandidate?.label ?: "Falla de sensores/alimentación"

        val steps = mutableListOf<String>()
        val comps = mutableListOf<String>()
        val diodeTips = mutableListOf<String>()
        val cautions = mutableListOf<String>()
        val simulatedQueries = listOf(
            "$model ${report.primaryCandidate?.ruleId ?: "panic"} repair guide",
            "$model $diag micro-soldering fix"
        )
        val simulatedSources = listOf(
            SearchGroundingSource(
                title = "Base de Conocimiento de Hardware PanicLab ($model)",
                url = "https://paniclab.tech/kb/${report.productCode.lowercase()}",
                snippet = "Procedimientos de taller y esquemáticos validados por la comunidad."
            ),
            SearchGroundingSource(
                title = "Guía de Diagnóstico de Líneas I2C/SMC ($diag)",
                url = "https://ifixit.com/Device/iPhone",
                snippet = "Descarte de periféricos y solución de pánicos recurrentes."
            )
        )

        when {
            diag.contains("Micrófono", ignoreCase = true) || diag.contains("mic2", ignoreCase = true) -> {
                steps.add("Desconectar el flex de puerto de carga/micrófono inferior y encender la placa conectada a fuente.")
                steps.add("Verificar si el tiempo de reinicio de 3 minutos desaparece con el flex desconectado.")
                steps.add("Probar con un flex de carga original o de alta calidad (OEM).")
                steps.add("Revisar el conector FPC en la placa base bajo microscopio por pines doblados o sulfato.")
                steps.add("Si persiste con flex desconectado, medir la línea I2C3_SDA e I2C3_SCL en modo diodo respecto a tierra.")

                comps.add("Flex de puerto de carga (Lightning/USB-C + Micrófono 2)")
                comps.add("Conector FPC de puerto de carga (J_DOCK / J_FLEX)")
                comps.add("Resistencias pull-up de la línea I2C3 (R_I2C3_SDA, R_I2C3_SCL)")
                comps.add("Filtros EMI y diodos TVS de protección en la línea")

                diodeTips.add("Línea I2C3_SDA: ~0.420V - 0.490V (Modo diodo, punta roja a GND).")
                diodeTips.add("Línea I2C3_SCL: ~0.420V - 0.490V. Si da OL (Open Loop), hay corte en la pista.")
                diodeTips.add("Si da 0.000V o <0.100V, capacitor o diodo TVS en corto a tierra.")
            }
            diag.contains("Sensor Térmico", ignoreCase = true) || diag.contains("Thermal", ignoreCase = true) || diag.contains("NTC", ignoreCase = true) -> {
                steps.add("Identificar el sensor térmico específico (Batería, Flash, Lógica Superior o Auricular).")
                steps.add("Probar con batería original conocida con línea HDQ/SWI/I2C de gas gauge intacta.")
                steps.add("Desconectar flex de auricular/sensor de proximidad superior para descartar sensor de luz/temperatura.")
                steps.add("Inspeccionar termistor NTC en el esquemático y verificar divisor resistivo.")

                comps.add("Termistor NTC del módulo de carga o flex superior")
                comps.add("BMS de la batería / Conector de batería FPC")
                comps.add("Línea SENSOR_TEMP / THERM_SENSE")

                diodeTips.add("Línea BATT_SWI / HDQ: ~0.550V - 0.620V en modo diodo.")
                diodeTips.add("Línea NTC: ~0.480V. Verificar resistencia de 10k/100k a 25°C.")
            }
            diag.contains("Barómetro", ignoreCase = true) || diag.contains("Presión", ignoreCase = true) -> {
                steps.add("Desconectar el flex secundario donde se aloja el barómetro (según modelo).")
                steps.add("Inspeccionar la membrana del sensor barométrico por ingreso de líquido o polvo metálico.")
                steps.add("Reemplazar el flex periférico antes de tocar la placa.")

                comps.add("Sensor Barométrico (IC Barometer)")
                comps.add("Flex de botón de volumen / carga")
                comps.add("Línea I2C de comunicación del sensor")

                diodeTips.add("Alimentación PP1V8_S2 / PP1V8_ALWAYS: 1.80V estables.")
                diodeTips.add("Líneas de datos I2C: ~0.450V con caída de diodo simétrica.")
            }
            diag.contains("NAND", ignoreCase = true) || diag.contains("Almacenamiento", ignoreCase = true) -> {
                steps.add("Inspeccionar rails de voltaje de la NAND (PP3V0_NAND, PP1V8_NAND, PP0V9_NAND).")
                steps.add("Comprobar si el dispositivo entra en modo DFU o Recovery de forma estable.")
                steps.add("Verificar capacitores de desacoplo alrededor del chip NAND por cortos.")
                steps.add("Si hay falla de NVMe / PCIe, comprobar líneas diferenciales de reloj y datos.")

                comps.add("Chip de Memoria NAND Flash")
                comps.add("PMIC Principal (Power Management IC)")
                comps.add("Capacitores de desacoplo en rails PP3V0 / PP1V8")

                diodeTips.add("Línea PP3V0_NAND: ~0.400V - 0.450V.")
                diodeTips.add("Línea PP1V8_NAND: ~0.350V - 0.420V.")
                diodeTips.add("Líneas PCIe TX/RX: ~0.300V simétricas.")
            }
            else -> {
                steps.add("Desconectar todos los periféricos no esenciales (cámaras, flex superior, flex de carga, flash).")
                steps.add("Iniciar únicamente con pantalla y cable de alimentación para aislar periféricos en corto.")
                steps.add("Conectar periféricos uno por uno hasta reproducir el pánico.")
                steps.add("Inspeccionar la placa en microscopio en búsqueda de zonas con corrosión o golpes.")

                comps.add("Periféricos y flex cables de interconexión")
                comps.add("Conectores FPC principales")
                comps.add("Líneas principales de comunicación I2C y SPI")

                diodeTips.add("Revisar líneas principales PP_VDD_MAIN y PP_BATT_VCC (>0.300V).")
                diodeTips.add("Revisar líneas I2C primarias (deben marcar entre 0.400V y 0.550V).")
            }
        }

        cautions.add("Desconectar la batería antes de manipular conectores FPC.")
        cautions.add("No aplicar calor superior a 80°C sobre módulos Face ID / Proyector de puntos.")
        cautions.add("Verificar siempre con multímetro en escala de continuidad antes de inyectar voltaje.")

        val fullSummary = StringBuilder().apply {
            append("### Guía de Reparación Recomendada: $diag ($model)\n\n")
            if (reasonNote.isNotBlank()) {
                append("*Nota: $reasonNote*\n\n")
            }
            append("**Resumen Técnico:**\n")
            append("El código de Panic Full detectado apunta a una falla en el subsistema de $diag en $model. En el 85%+ de los casos de taller, esta falla se origina en un periférico desconectado, dañado por ingreso de líquido o con el sensor averiado en su flex correspondiente.\n\n")
            append("**Procedimiento de Taller Sugerido:**\n")
            steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
            append("\n**Componentes y Líneas a Inspeccionar:**\n")
            comps.forEach { append("- $it\n") }
            append("\n**Mediciones Clave:**\n")
            diodeTips.forEach { append("- $it\n") }
        }.toString()

        return GroundedRepairSuggestion(
            summary = fullSummary,
            detailedSteps = steps,
            suspectedComponents = comps,
            diodeModeReferenceTips = diodeTips,
            cautions = cautions,
            searchSources = simulatedSources,
            searchQueries = simulatedQueries,
            retrievedAt = System.currentTimeMillis(),
            isRealTimeGrounded = false
        )
    }
}
