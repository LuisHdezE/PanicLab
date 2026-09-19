package com.example.parser

import com.example.data.local.dao.DeviceDao
import com.example.domain.model.DeviceModel
import com.example.util.RulePackJsonParser

object DeviceResolver {

    // Built-in verified seed mapping from iPhone X through iPhone 17e
    private val STATIC_DEVICE_MAP: Map<String, DeviceModel> = mapOf(
        "iPhone10,3" to DeviceModel("iPhone10,3", "iPhone X", "IPHONE_X", "X", "THERMAL_CLASSIC_X_TO_12", 2017),
        "iPhone10,6" to DeviceModel("iPhone10,6", "iPhone X", "IPHONE_X", "X", "THERMAL_CLASSIC_X_TO_12", 2017),
        "iPhone11,2" to DeviceModel("iPhone11,2", "iPhone XS", "IPHONE_XS", "XS", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,4" to DeviceModel("iPhone11,4", "iPhone XS Max", "IPHONE_XS_MAX", "XS_MAX", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,6" to DeviceModel("iPhone11,6", "iPhone XS Max", "IPHONE_XS_MAX", "XS_MAX", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone11,8" to DeviceModel("iPhone11,8", "iPhone XR", "IPHONE_XR", "XR", "THERMAL_CLASSIC_X_TO_12", 2018),
        "iPhone12,1" to DeviceModel("iPhone12,1", "iPhone 11", "IPHONE_11", "11", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,3" to DeviceModel("iPhone12,3", "iPhone 11 Pro", "IPHONE_11_PRO", "11_PRO", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,5" to DeviceModel("iPhone12,5", "iPhone 11 Pro Max", "IPHONE_11_PRO_MAX", "11_PRO_MAX", "THERMAL_CLASSIC_X_TO_12", 2019),
        "iPhone12,8" to DeviceModel("iPhone12,8", "iPhone SE (2nd generation)", "IPHONE_SE2", "SE2", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,1" to DeviceModel("iPhone13,1", "iPhone 12 mini", "IPHONE_12_MINI", "12_MINI", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,2" to DeviceModel("iPhone13,2", "iPhone 12", "IPHONE_12", "12", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,3" to DeviceModel("iPhone13,3", "iPhone 12 Pro", "IPHONE_12_PRO", "12_PRO", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone13,4" to DeviceModel("iPhone13,4", "iPhone 12 Pro Max", "IPHONE_12_PRO_MAX", "12_PRO_MAX", "THERMAL_CLASSIC_X_TO_12", 2020),
        "iPhone14,2" to DeviceModel("iPhone14,2", "iPhone 13 Pro", "IPHONE_13_PRO", "13_PRO", "SMC_13", 2021),
        "iPhone14,3" to DeviceModel("iPhone14,3", "iPhone 13 Pro Max", "IPHONE_13_PRO_MAX", "13_PRO_MAX", "SMC_13", 2021),
        "iPhone14,4" to DeviceModel("iPhone14,4", "iPhone 13 mini", "IPHONE_13_MINI", "13_MINI", "SMC_13_MINI", 2021),
        "iPhone14,5" to DeviceModel("iPhone14,5", "iPhone 13", "IPHONE_13", "13", "SMC_13", 2021),
        "iPhone14,6" to DeviceModel("iPhone14,6", "iPhone SE (3rd generation)", "IPHONE_SE3", "SE3", "THERMAL_CLASSIC_X_TO_12", 2022),
        "iPhone14,7" to DeviceModel("iPhone14,7", "iPhone 14", "IPHONE_14", "14", "SMC_14_BASE", 2022),
        "iPhone14,8" to DeviceModel("iPhone14,8", "iPhone 14 Plus", "IPHONE_14_PLUS", "14_PLUS", "SMC_14_BASE", 2022),
        "iPhone15,2" to DeviceModel("iPhone15,2", "iPhone 14 Pro", "IPHONE_14_PRO", "14_PRO", "SMC_14_PRO", 2022),
        "iPhone15,3" to DeviceModel("iPhone15,3", "iPhone 14 Pro Max", "IPHONE_14_PRO_MAX", "14_PRO_MAX", "SMC_14_PRO", 2022),
        "iPhone15,4" to DeviceModel("iPhone15,4", "iPhone 15", "IPHONE_15", "15", "SMC_15_BASE", 2023),
        "iPhone15,5" to DeviceModel("iPhone15,5", "iPhone 15 Plus", "IPHONE_15_PLUS", "15_PLUS", "SMC_15_BASE", 2023),
        "iPhone16,1" to DeviceModel("iPhone16,1", "iPhone 15 Pro", "IPHONE_15_PRO", "15_PRO", "SMC_15_PRO", 2023),
        "iPhone16,2" to DeviceModel("iPhone16,2", "iPhone 15 Pro Max", "IPHONE_15_PRO_MAX", "15_PRO_MAX", "SMC_15_PRO", 2023),
        "iPhone17,1" to DeviceModel("iPhone17,1", "iPhone 16 Pro", "IPHONE_16_PRO", "16_PRO", "SMC_16_PRO", 2024),
        "iPhone17,2" to DeviceModel("iPhone17,2", "iPhone 16 Pro Max", "IPHONE_16_PRO_MAX", "16_PRO_MAX", "SMC_16_PRO", 2024),
        "iPhone17,3" to DeviceModel("iPhone17,3", "iPhone 16", "IPHONE_16", "16", "SMC_16_BASE", 2024),
        "iPhone17,4" to DeviceModel("iPhone17,4", "iPhone 16 Plus", "IPHONE_16_PLUS", "16_PLUS", "SMC_16_BASE", 2024),
        "iPhone17,5" to DeviceModel("iPhone17,5", "iPhone 16e", "IPHONE_16E", "16E", "SMC_16E", 2025),
        "iPhone18,1" to DeviceModel("iPhone18,1", "iPhone 17 Pro", "IPHONE_17_PRO", "17_PRO", "SMC_17", 2025),
        "iPhone18,2" to DeviceModel("iPhone18,2", "iPhone 17 Pro Max", "IPHONE_17_PRO_MAX", "17_PRO_MAX", "SMC_17", 2025),
        "iPhone18,3" to DeviceModel("iPhone18,3", "iPhone 17", "IPHONE_17", "17", "SMC_17", 2025),
        "iPhone18,4" to DeviceModel("iPhone18,4", "iPhone Air", "IPHONE_AIR", "AIR", "SMC_17", 2025),
        "iPhone18,5" to DeviceModel("iPhone18,5", "iPhone 17e", "IPHONE_17E", "17E", "SMC_17E", 2026)
    )

    suspend fun resolve(productCode: String?, deviceDao: DeviceDao? = null): DeviceModel? {
        if (productCode.isNullOrBlank()) return null
        val cleanCode = productCode.trim()

        // 1. Try DB if available
        if (deviceDao != null) {
            try {
                val dbEntity = deviceDao.getDeviceByProductCode(cleanCode)
                if (dbEntity != null) {
                    return RulePackJsonParser.entityToDevice(dbEntity)
                }
            } catch (e: Exception) {
                // Fall back to static map
            }
        }

        // 2. Direct static map lookup
        return STATIC_DEVICE_MAP[cleanCode]
    }

    fun resolveSynchronous(productCode: String?): DeviceModel? {
        if (productCode.isNullOrBlank()) return null
        return STATIC_DEVICE_MAP[productCode.trim()]
    }
}
