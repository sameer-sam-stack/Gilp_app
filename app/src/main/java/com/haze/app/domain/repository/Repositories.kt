package com.haze.app.domain.repository

import com.haze.app.domain.model.LocalPromotion
import com.haze.app.domain.model.UserProfile
import com.haze.app.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun clearUserProfile(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
}

interface MobilityRepository {
    fun getSimulatedVehicles(): Flow<List<Vehicle>>
    suspend fun getVehicleById(vehicleId: String): Vehicle?
    suspend fun searchCompatibleVehicles(
        pickupQuery: String,
        dropQuery: String
    ): List<Vehicle>
    fun getLocalPromotions(): Flow<List<LocalPromotion>>
}
