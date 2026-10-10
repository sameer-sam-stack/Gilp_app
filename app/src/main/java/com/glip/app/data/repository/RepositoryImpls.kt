package com.glip.app.data.repository

import com.glip.app.data.demo.DemoDataStore
import com.glip.app.data.local.DemoProfilePreferences
import com.glip.app.domain.model.LocalPromotion
import com.glip.app.domain.model.UserProfile
import com.glip.app.domain.model.Vehicle
import com.glip.app.domain.repository.MobilityRepository
import com.glip.app.domain.repository.UserRepository
import com.glip.app.domain.usecase.RouteMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UserRepositoryImpl(
    private val preferences: DemoProfilePreferences
) : UserRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return preferences.observeUserProfile()
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            preferences.saveUserProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearUserProfile(): Result<Unit> {
        return try {
            preferences.clearUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return preferences.getUserProfile() != null
    }
}

class DemoMobilityRepositoryImpl(
    private val routeMatcher: RouteMatcher = RouteMatcher()
) : MobilityRepository {

    override fun getSimulatedVehicles(): Flow<List<Vehicle>> {
        return flowOf(DemoDataStore.demoVehicles)
    }

    override suspend fun getVehicleById(vehicleId: String): Vehicle? {
        return DemoDataStore.demoVehicles.find { it.vehicleId == vehicleId }
    }

    override suspend fun searchCompatibleVehicles(
        pickupQuery: String,
        dropQuery: String
    ): List<Vehicle> {
        return routeMatcher.findCompatibleVehicles(
            vehicles = DemoDataStore.demoVehicles,
            pickupQuery = pickupQuery,
            dropQuery = dropQuery
        )
    }

    override fun getLocalPromotions(): Flow<List<LocalPromotion>> {
        return flowOf(DemoDataStore.demoPromotions)
    }
}
