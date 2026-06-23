package com.example.topmejorestiendas.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servicio de consulta de RUC contra la API pública de SUNAT (vía apis.net.pe).
 *
 * Realiza consultas HTTP GET con autenticación Bearer Token.
 * No requiere dependencias externas (usa HttpURLConnection nativo de Android).
 *
 * Uso:
 * ```
 * val result = SunatService.consultarRuc("20100017491")
 * result.onSuccess { rucData ->
 *     println(rucData.razonSocial) // "SUPERINTENDENCIA NAC. DE ADUANAS Y..."
 *     println(rucData.isValid)     // true si ACTIVO y HABIDO
 * }
 * ```
 */
object SunatService {

    /**
     * Valida que el RUC tenga el formato correcto (11 dígitos numéricos).
     * Los RUC peruanos empiezan con 10 (persona natural) o 20 (persona jurídica).
     */
    fun isValidRucFormat(ruc: String): Boolean {
        if (ruc.length != SunatConfig.RUC_LENGTH) return false
        if (!ruc.all { it.isDigit() }) return false
        val prefix = ruc.substring(0, 2)
        return prefix == "10" || prefix == "15" || prefix == "17" || prefix == "20"
    }

    /**
     * Consulta los datos de un RUC en la API de SUNAT.
     *
     * @param ruc Número de RUC de 11 dígitos
     * @return Result con [SunatRucResponse] (Success) o excepción (Failure)
     */
    suspend fun consultarRuc(ruc: String): Result<SunatRucResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Validación de formato antes de hacer la petición
                if (!isValidRucFormat(ruc)) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            "RUC inválido. Debe tener 11 dígitos y empezar con 10 o 20."
                        )
                    )
                }

                val url = URL("${SunatConfig.BASE_URL}?numero=$ruc")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer ${SunatConfig.BEARER_TOKEN}")
                    setRequestProperty("Referer", SunatConfig.REFERER)
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    connection.disconnect()

                    val json = JSONObject(response.toString())
                    val rucResponse = parseResponse(json)

                    Result.success(rucResponse)
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 422) {
                    connection.disconnect()
                    Result.failure(
                        NoSuchElementException("RUC no encontrado en la base de datos de SUNAT.")
                    )
                } else {
                    connection.disconnect()
                    Result.failure(
                        RuntimeException("Error del servidor SUNAT (código $responseCode). Intenta nuevamente.")
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(
                    RuntimeException("Tiempo de espera agotado al consultar SUNAT. Verifica tu conexión a internet.")
                )
            } catch (e: java.net.UnknownHostException) {
                Result.failure(
                    RuntimeException("Sin conexión a internet. Verifica tu conexión para consultar SUNAT.")
                )
            } catch (e: Exception) {
                Result.failure(
                    RuntimeException("Error al consultar SUNAT: ${e.message}")
                )
            }
        }
    }

    /**
     * Parsea la respuesta JSON de la API de SUNAT al modelo [SunatRucResponse].
     */
    private fun parseResponse(json: JSONObject): SunatRucResponse {
        return SunatRucResponse(
            ruc = json.optString("ruc", json.optString("numeroDocumento", "")),
            razonSocial = json.optString("nombre_o_razon_social", json.optString("razonSocial", json.optString("nombre", ""))),
            estado = json.optString("estado", "DESCONOCIDO"),
            condicion = json.optString("condicion", "DESCONOCIDO"),
            direccion = json.optString("direccion", json.optString("direccion_completa", "")),
            departamento = json.optString("departamento", ""),
            provincia = json.optString("provincia", ""),
            distrito = json.optString("distrito", "")
        )
    }
}
